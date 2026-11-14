package com.cse444.threadtrace.lineage.domain.strategy;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AllocationStrategyRegistry {

    private final Map<String, AllocationStrategy> strategies;
    private final AllocationStrategy defaultStrategy;

    public AllocationStrategyRegistry(Map<String, AllocationStrategy> strategyBeans) {
        this.strategies = strategyBeans;
        this.defaultStrategy = strategyBeans.values().stream()
                .filter(s -> "MASS".equalsIgnoreCase(s.getStrategyName()))
                .findFirst()
                .orElseGet(() -> strategyBeans.values().stream().findFirst().orElseThrow());
    }

    public AllocationStrategy getStrategy(String name) {
        if (name == null || name.isBlank()) {
            return defaultStrategy;
        }
        return Optional.ofNullable(strategies.get(name))
                .or(() -> strategies.values().stream()
                        .filter(s -> s.getStrategyName().equalsIgnoreCase(name))
                        .findFirst())
                .orElse(defaultStrategy);
    }
}
