package com.workflow.dto;

import com.workflow.entity.Step.StepType;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepDto {
    private UUID id;
    private UUID workflowId;
    private String name;
    private StepType stepType;
    private Integer order;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
