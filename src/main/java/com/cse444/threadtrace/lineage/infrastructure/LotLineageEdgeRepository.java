package com.cse444.threadtrace.lineage.infrastructure;

import com.cse444.threadtrace.lineage.domain.model.LotLineageEdgeEntity;
import com.cse444.threadtrace.lineage.domain.model.LotLineageEdgeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LotLineageEdgeRepository extends JpaRepository<LotLineageEdgeEntity, LotLineageEdgeId> {
    List<LotLineageEdgeEntity> findByParentLotId(UUID parentLotId);
    List<LotLineageEdgeEntity> findByChildLotId(UUID childLotId);
}
