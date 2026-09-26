package com.auditvault.auditvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Query filter for retrieving audit logs with optional filtering
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogQueryFilter {

    private String actorId;
    private String resourceType;
    private String resourceId;
    private String eventType;
    private LocalDateTime fromTimestamp;
    private LocalDateTime toTimestamp;
    
    @Builder.Default
    private Integer pageNumber = 0;
    
    @Builder.Default
    private Integer pageSize = 50;

    public int getPageNumber() {
        return pageNumber != null ? pageNumber : 0;
    }

    public int getPageSize() {
        return pageSize != null ? pageSize : 50;
    }
}
