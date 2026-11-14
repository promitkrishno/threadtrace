package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.*;
import com.cse444.threadtrace.lineage.infrastructure.LotEventRepository;
import com.cse444.threadtrace.lineage.infrastructure.LotLineageEdgeRepository;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RecordLotEventUseCase {

    private final LotRepository lotRepository;
    private final LotEventRepository lotEventRepository;
    private final LotLineageEdgeRepository lotLineageEdgeRepository;
    private final TraceabilityProperties properties;
    private final MassBalanceValidator massBalanceValidator;

    public RecordLotEventUseCase(
            LotRepository lotRepository,
            LotEventRepository lotEventRepository,
            LotLineageEdgeRepository lotLineageEdgeRepository,
            TraceabilityProperties properties,
            MassBalanceValidator massBalanceValidator
    ) {
        this.lotRepository = lotRepository;
        this.lotEventRepository = lotEventRepository;
        this.lotLineageEdgeRepository = lotLineageEdgeRepository;
        this.properties = properties;
        this.massBalanceValidator = massBalanceValidator;
    }

    public RecordEventResult recordEvent(RecordEventCommand command) {
        Instant rawOccurred = command.occurredAt() != null ? command.occurredAt() : Instant.now();
        Instant occurredAt = rawOccurred.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        LotEntity targetLot;
        boolean isGenesis = false;

        // 1. Resolve or Create Target Lot
        if (command.lotId() == null) {
            boolean isMergeCreation = command.mergeParents() != null && !command.mergeParents().isEmpty();
            if (command.eventType() != EventType.RECEIVE && !isMergeCreation) {
                throw new DomainException("INVALID_OPERATION", "Lot ID must be provided unless creating a new lot via RECEIVE or N-to-1 merge");
            }
            if (command.newLotNumber() == null || command.brandId() == null) {
                throw new DomainException("INVALID_NEW_LOT", "New lot number and brand ID are required when creating a lot");
            }
            if (lotRepository.existsByLotNumber(command.newLotNumber())) {
                throw new DomainException("LOT_EXISTS", "Lot with number '" + command.newLotNumber() + "' already exists");
            }

            targetLot = new LotEntity(
                    command.newLotNumber(),
                    command.brandId(),
                    command.facilityId(),
                    command.styleCode(),
                    command.shipmentCode(),
                    LotStatus.ACTIVE,
                    command.outputQuantity(),
                    command.lotUnit() != null ? command.lotUnit() : Unit.KG
            );
            targetLot = lotRepository.save(targetLot);
            isGenesis = true;
        } else {
            targetLot = lotRepository.findById(command.lotId())
                    .orElseThrow(() -> new DomainException("LOT_NOT_FOUND", "Lot not found with id: " + command.lotId()));
            targetLot.validateCanRecordEvent(command.eventType());
        }

        // 2. Mass Balance and Loss Percentage Calculation
        BigDecimal inputQty = command.inputQuantity();
        BigDecimal outputQty = command.outputQuantity();

        var massBalanceResult = massBalanceValidator.validate(
                command.eventType(),
                inputQty,
                outputQty,
                targetLot
        );
        BigDecimal lossPct = massBalanceResult.lossPercentage();
        boolean massBalanceFlagged = massBalanceResult.flagged();

        // 3. Sustainability Step Metrics
        TraceabilityProperties.EmissionFactors emissions = properties.emissionFactors();
        BigDecimal stepWater = outputQty.multiply(BigDecimal.valueOf(emissions.waterLitersPerUnit())).setScale(4, RoundingMode.HALF_UP);
        BigDecimal stepEnergy = outputQty.multiply(BigDecimal.valueOf(emissions.energyKwhPerUnit())).setScale(4, RoundingMode.HALF_UP);
        BigDecimal stepCo2e = outputQty.multiply(BigDecimal.valueOf(emissions.co2eKgPerUnit())).setScale(4, RoundingMode.HALF_UP);

        // 4. Sequence and SHA-256 Hash Chaining
        Optional<LotEventEntity> latestEventOpt = lotEventRepository.findLatestEventByLotId(targetLot.getId());
        long sequenceNo = latestEventOpt.map(e -> e.getSequenceNo() + 1).orElse(1L);
        String previousHash = latestEventOpt.map(LotEventEntity::getEventHash).orElse(LotEventEntity.GENESIS_HASH);

        String eventHash = LotEventEntity.computeHash(
                previousHash,
                targetLot.getId(),
                sequenceNo,
                command.eventType(),
                command.facilityId(),
                inputQty,
                outputQty,
                lossPct,
                command.unit(),
                occurredAt
        );

        LotEventEntity event = new LotEventEntity();
        event.setLotId(targetLot.getId());
        event.setSequenceNo(sequenceNo);
        event.setEventType(command.eventType());
        event.setFacilityId(command.facilityId());
        event.setInputQuantity(inputQty);
        event.setOutputQuantity(outputQty);
        event.setLossPercentage(lossPct);
        event.setUnit(command.unit());
        event.setStepWaterLiters(stepWater);
        event.setStepEnergyKwh(stepEnergy);
        event.setStepCo2eKg(stepCo2e);
        event.setMassBalanceFlagged(massBalanceFlagged);
        event.setPreviousEventHash(previousHash);
        event.setEventHash(eventHash);
        event.setOccurredAt(occurredAt);
        event.setRecordedAt(Instant.now());

        LotEventEntity savedEvent = lotEventRepository.save(event);

        // 5. Update Target Lot Metrics
        targetLot.setCurrentFacilityId(command.facilityId());
        targetLot.setAccumulatedWaterL(targetLot.getAccumulatedWaterL().add(stepWater));
        targetLot.setAccumulatedEnergyKwh(targetLot.getAccumulatedEnergyKwh().add(stepEnergy));
        targetLot.setAccumulatedCo2eKg(targetLot.getAccumulatedCo2eKg().add(stepCo2e));
        targetLot.setCurrentQuantity(outputQty);

        List<UUID> createdChildLotIds = new ArrayList<>();

        // 6. Handle 1-to-N Lot Splits
        if (command.splitChildren() != null && !command.splitChildren().isEmpty()) {
            for (ChildLotOutput childDto : command.splitChildren()) {
                if (lotRepository.existsByLotNumber(childDto.lotNumber())) {
                    throw new DomainException("CHILD_LOT_EXISTS", "Child lot number '" + childDto.lotNumber() + "' already exists");
                }
                LotEntity childLot = new LotEntity(
                        childDto.lotNumber(),
                        targetLot.getBrandId(),
                        command.facilityId(),
                        childDto.styleCode() != null ? childDto.styleCode() : targetLot.getStyleCode(),
                        childDto.shipmentCode() != null ? childDto.shipmentCode() : targetLot.getShipmentCode(),
                        LotStatus.ACTIVE,
                        childDto.quantity(),
                        childDto.unit()
                );
                // Child inherits proportional sustainability metrics
                BigDecimal ratio = childDto.allocationRatio() != null ? childDto.allocationRatio() : BigDecimal.ONE;
                childLot.setAccumulatedWaterL(targetLot.getAccumulatedWaterL().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                childLot.setAccumulatedEnergyKwh(targetLot.getAccumulatedEnergyKwh().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                childLot.setAccumulatedCo2eKg(targetLot.getAccumulatedCo2eKg().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                childLot.setCertifiedOrganicPct(targetLot.getCertifiedOrganicPct());
                childLot.setCertifiedRecycledPct(targetLot.getCertifiedRecycledPct());

                LotEntity savedChild = lotRepository.save(childLot);
                createdChildLotIds.add(savedChild.getId());

                // Lineage edge: Parent -> Child
                LotLineageEdgeEntity edge = new LotLineageEdgeEntity(
                        targetLot.getId(),
                        savedChild.getId(),
                        savedEvent.getId(),
                        childDto.quantity(),
                        childDto.unit().name(),
                        ratio
                );
                lotLineageEdgeRepository.save(edge);

                // Child genesis event
                String childHash = LotEventEntity.computeHash(
                        LotEventEntity.GENESIS_HASH,
                        savedChild.getId(),
                        1L,
                        EventType.RECEIVE,
                        command.facilityId(),
                        childDto.quantity(),
                        childDto.quantity(),
                        BigDecimal.ZERO,
                        childDto.unit().name(),
                        occurredAt
                );
                LotEventEntity childEvent = new LotEventEntity();
                childEvent.setLotId(savedChild.getId());
                childEvent.setSequenceNo(1L);
                childEvent.setEventType(EventType.RECEIVE);
                childEvent.setFacilityId(command.facilityId());
                childEvent.setInputQuantity(childDto.quantity());
                childEvent.setOutputQuantity(childDto.quantity());
                childEvent.setLossPercentage(BigDecimal.ZERO);
                childEvent.setUnit(childDto.unit().name());
                childEvent.setPreviousEventHash(LotEventEntity.GENESIS_HASH);
                childEvent.setEventHash(childHash);
                childEvent.setOccurredAt(occurredAt);
                childEvent.setRecordedAt(Instant.now());
                lotEventRepository.save(childEvent);
            }
            if (massBalanceFlagged) {
                targetLot.setStatus(LotStatus.QUARANTINED);
            } else {
                targetLot.updateStatusForEvent(command.eventType(), true);
            }
        } else if (command.mergeParents() != null && !command.mergeParents().isEmpty()) {
            // 7. Handle N-to-1 Lot Merges
            for (ParentLotInput parentInput : command.mergeParents()) {
                LotEntity parentLot = lotRepository.findById(parentInput.parentLotId())
                        .orElseThrow(() -> new DomainException("PARENT_LOT_NOT_FOUND", "Parent lot not found: " + parentInput.parentLotId()));
                parentLot.validateCanRecordEvent(command.eventType());
                parentLot.updateStatusForEvent(command.eventType(), true); // Merged parent consumed
                lotRepository.save(parentLot);

                BigDecimal ratio = parentInput.allocationRatio() != null ? parentInput.allocationRatio() : BigDecimal.ONE;
                targetLot.setAccumulatedWaterL(targetLot.getAccumulatedWaterL().add(parentLot.getAccumulatedWaterL().multiply(ratio)));
                targetLot.setAccumulatedEnergyKwh(targetLot.getAccumulatedEnergyKwh().add(parentLot.getAccumulatedEnergyKwh().multiply(ratio)));
                targetLot.setAccumulatedCo2eKg(targetLot.getAccumulatedCo2eKg().add(parentLot.getAccumulatedCo2eKg().multiply(ratio)));

                LotLineageEdgeEntity edge = new LotLineageEdgeEntity(
                        parentLot.getId(),
                        targetLot.getId(),
                        savedEvent.getId(),
                        parentInput.inputQuantityUsed(),
                        parentInput.inputUnit(),
                        ratio
                );
                lotLineageEdgeRepository.save(edge);
            }
            if (massBalanceFlagged) {
                targetLot.setStatus(LotStatus.QUARANTINED);
            } else {
                targetLot.updateStatusForEvent(command.eventType(), false);
            }
        } else {
            // Standard single-lot processing step
            if (massBalanceFlagged) {
                targetLot.setStatus(LotStatus.QUARANTINED);
            } else {
                boolean isConsumed = outputQty.compareTo(BigDecimal.ZERO) <= 0;
                targetLot.updateStatusForEvent(command.eventType(), isConsumed);
            }
        }

        lotRepository.save(targetLot);

        return new RecordEventResult(
                savedEvent.getId(),
                targetLot.getId(),
                targetLot.getLotNumber(),
                savedEvent.getSequenceNo(),
                savedEvent.getEventType(),
                targetLot.getStatus(),
                savedEvent.getInputQuantity(),
                savedEvent.getOutputQuantity(),
                savedEvent.getLossPercentage(),
                savedEvent.getMassBalanceFlagged(),
                savedEvent.getPreviousEventHash(),
                savedEvent.getEventHash(),
                savedEvent.getOccurredAt(),
                createdChildLotIds
        );
    }

    @Transactional(readOnly = true)
    public IntegrityVerificationResult verifyLotIntegrity(UUID lotId) {
        LotEntity lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new DomainException("LOT_NOT_FOUND", "Lot not found with id: " + lotId));

        List<LotEventEntity> events = lotEventRepository.findByLotIdOrderBySequenceNoAsc(lotId);
        if (events.isEmpty()) {
            return IntegrityVerificationResult.valid(lot.getId(), lot.getLotNumber(), 0);
        }

        String expectedPreviousHash = LotEventEntity.GENESIS_HASH;
        for (LotEventEntity event : events) {
            // Check previous hash linkage
            if (!event.getPreviousEventHash().equals(expectedPreviousHash)) {
                return IntegrityVerificationResult.tampered(
                        lot.getId(),
                        lot.getLotNumber(),
                        events.size(),
                        event.getId(),
                        event.getSequenceNo(),
                        "Broken hash chain: expected previousHash='" + expectedPreviousHash + "', found='" + event.getPreviousEventHash() + "'"
                );
            }

            // Recalculate event hash
            String computedHash = event.calculateExpectedHash();
            if (!event.getEventHash().equalsIgnoreCase(computedHash)) {
                return IntegrityVerificationResult.tampered(
                        lot.getId(),
                        lot.getLotNumber(),
                        events.size(),
                        event.getId(),
                        event.getSequenceNo(),
                        "Hash mismatch: stored eventHash='" + event.getEventHash() + "', computed='" + computedHash + "'"
                );
            }

            expectedPreviousHash = event.getEventHash();
        }

        return IntegrityVerificationResult.valid(lot.getId(), lot.getLotNumber(), events.size());
    }
}
