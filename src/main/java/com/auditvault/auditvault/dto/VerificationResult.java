package com.auditvault.auditvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chain verification result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResult {

    private Boolean isIntact;
    private String message;
    private Long firstInconsistencyId;
    private String violationType;
    private Long totalRecordsVerified;
    private Long firstArchivedRecordId;
}
