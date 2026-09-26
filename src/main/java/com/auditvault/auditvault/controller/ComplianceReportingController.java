package com.auditvault.auditvault.controller;

import com.auditvault.auditvault.service.ComplianceReportingService;
import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Scenario C: Compliance Reporting
 * 
 * Clarified Requirement:
 * Provide audit reports for regulatory compliance on data access to client/account data.
 * 
 * Features:
 * - Track all data access events (READ, UPDATE, DELETE, PERMISSION_CHANGE)
 * - Aggregate access patterns by actor and resource
 * - Identify compliance risks (high volume access, delete operations, unusual patterns)
 * - Generate compliance status (GREEN/AMBER/RED)
 * 
 * Endpoints:
 * - GET /audit/compliance/report - Generate compliance report
 */
@RestController
@RequestMapping("/audit/compliance")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Compliance Reporting", description = "APIs for generating compliance reports and tracking data access patterns")
public class ComplianceReportingController {

    private final ComplianceReportingService complianceReportingService;

    public ComplianceReportingController(ComplianceReportingService complianceReportingService) {
        this.complianceReportingService = complianceReportingService;
    }

    /**
     * Scenario C: Compliance Reporting
     * 
     * CLARIFIED REQUIREMENT:
     * "Regulators need to be able to audit access to client account data."
     * 
     * Clarification:
     * - "Audit access" means track who accessed what data, when, and what they did
     * - "Client account data" means resources of type ACCOUNT, CLIENT, CUSTOMER
     * - "Regulators" are systems/users with compliance reporting access
     * 
     * AMBIGUITIES & ASSUMPTIONS:
     * 1. Time period: Default 30 days (configurable via daysBack parameter)
     * 2. Access types: READ, UPDATE, DELETE, PERMISSION changes on sensitive resources
     * 3. Compliance status based on risk indicators:
     *    - GREEN: No significant risk
     *    - AMBER: Unusual patterns (high volume, unusual times)
     *    - RED: High-risk operations detected (deletes, permission changes)
     * 4. Report consumers: Compliance officers, auditors, regulators (assumes authorization layer)
     * 
     * DESIGN DECISIONS:
     * - Filter events by resourceType (ACCOUNT, CLIENT, etc.) and eventType (DATA_READ, etc.)
     * - Aggregate access by actor, resource, and time period
     * - Include both detailed trail and summary statistics
     * - Provide risk assessment and recommendations
     * - Report generation is read-only (no external side effects)
     * 
     * @param daysBack Number of days to include in report (default 30)
     * @return Comprehensive compliance report
     */
    @GetMapping("/report")
    @Operation(summary = "Generate compliance report", description = "Generates a comprehensive compliance report tracking data access to client/account data with risk assessment and recommendations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compliance report generated successfully",
            content = @Content(schema = @Schema(type = "object", example = "{\"header\":{},\"executiveSummary\":{},\"accessPatternsByActor\":[],\"riskIndicators\":[]}"))),
        @ApiResponse(responseCode = "400", description = "Invalid daysBack parameter"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<JsonNode> generateComplianceReport(
            @Parameter(description = "Number of days to include in report (1-365, default: 30)", example = "30")
            @RequestParam(defaultValue = "30") Integer daysBack) {

        log.info("GET /audit/compliance/report - Generating compliance report for last {} days", daysBack);

        if (daysBack <= 0 || daysBack > 365) {
            daysBack = 30;
        }

        JsonNode report = complianceReportingService.generateComplianceReport(daysBack);

        return ResponseEntity.ok(report);
    }
}
