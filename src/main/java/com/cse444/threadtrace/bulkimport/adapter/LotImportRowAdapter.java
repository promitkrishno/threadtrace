package com.cse444.threadtrace.bulkimport.adapter;

import com.cse444.threadtrace.lineage.application.RecordEventCommand;

import java.util.Map;
import java.util.UUID;

/**
 * Adapter Pattern: Converts heterogeneous raw factory import rows
 * (CSV cutting reports, Excel packing lists) into standardized RecordEventCommands (FR9).
 */
public interface LotImportRowAdapter {

    RecordEventCommand adapt(
            int rowNumber,
            Map<String, String> rowFields,
            UUID defaultFacilityId,
            UUID defaultBrandId
    );

    boolean supports(String importType);
}
