package com.workflow.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleDto {
    private UUID id;
    private UUID stepId;
    private String condition;
    private UUID nextStepId;
    private Integer priority;
    private Instant createdAt;
    private Instant updatedAt;
}
