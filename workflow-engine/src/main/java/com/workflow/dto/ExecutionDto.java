package com.workflow.dto;

import com.workflow.entity.Execution.ExecutionStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionDto {
    private UUID id;
    private UUID workflowId;
    private Integer workflowVersion;
    private ExecutionStatus status;
    private Map<String, Object> data;
    private List<Map<String, Object>> logs;
    private UUID currentStepId;
    private Integer retries;
    private UUID triggeredBy;
    private Instant startedAt;
    private Instant endedAt;
    private String workflowName; // optional, for list/audit
}
