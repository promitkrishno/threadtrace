package com.cse444.threadtrace.bulkimport.adapter;

public class ImportRowValidationException extends RuntimeException {

    private final int rowNumber;

    public ImportRowValidationException(int rowNumber, String message) {
        super(String.format("Row %d: %s", rowNumber, message));
        this.rowNumber = rowNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }
}
