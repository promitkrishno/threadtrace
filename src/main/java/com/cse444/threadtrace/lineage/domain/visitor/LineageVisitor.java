package com.cse444.threadtrace.lineage.domain.visitor;

import com.cse444.threadtrace.lineage.domain.composite.CompositeTransformedLot;
import com.cse444.threadtrace.lineage.domain.composite.LeafSourceLot;

/**
 * Visitor Pattern: Visitor interface for traversing a LineageNode DAG
 * to execute roll-up algorithms (footprints, claims, audits).
 */
public interface LineageVisitor {
    void visitLeaf(LeafSourceLot leaf);
    void visitComposite(CompositeTransformedLot composite);
}
