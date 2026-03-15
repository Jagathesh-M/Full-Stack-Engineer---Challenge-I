package com.workflow.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowListDto {
    private UUID id;
    private String name;
    private Integer version;
    private Boolean isActive;
    private Map<String, Object> inputSchema;
    private UUID startStepId;
    private Instant createdAt;
    private Instant updatedAt;
    private long stepCount;
}
