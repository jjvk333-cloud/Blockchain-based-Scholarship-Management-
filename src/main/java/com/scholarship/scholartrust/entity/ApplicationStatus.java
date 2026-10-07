package com.scholarship.scholartrust.entity;

public enum ApplicationStatus {
    PENDING,
    REVIEWING,
    APPROVED,
    REJECTED,
    DISBURSED;

    /**
     * Maps ApplicationStatus to the Solidity ScholarshipLedger.Status ordinal:
     * SUBMITTED(0), UNDER_REVIEW(1), APPROVED(2), REJECTED(3), DISBURSED(4)
     */
    public int toChainOrdinal() {
        switch (this) {
            case PENDING:
                return 0; // SUBMITTED
            case REVIEWING:
                return 1; // UNDER_REVIEW
            case APPROVED:
                return 2; // APPROVED
            case REJECTED:
                return 3; // REJECTED
            case DISBURSED:
                return 4; // DISBURSED
            default:
                throw new IllegalArgumentException("Unknown ApplicationStatus: " + this);
        }
    }

    /**
     * Maps Solidity ScholarshipLedger.Status ordinal back to ApplicationStatus:
     * 0 -> PENDING, 1 -> REVIEWING, 2 -> APPROVED, 3 -> REJECTED, 4 -> DISBURSED
     */
    public static ApplicationStatus fromChainOrdinal(int ordinal) {
        switch (ordinal) {
            case 0:
                return PENDING;
            case 1:
                return REVIEWING;
            case 2:
                return APPROVED;
            case 3:
                return REJECTED;
            case 4:
                return DISBURSED;
            default:
                throw new IllegalArgumentException("Invalid blockchain status ordinal: " + ordinal);
        }
    }
}