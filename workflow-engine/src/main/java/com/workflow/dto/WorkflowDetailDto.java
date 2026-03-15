package com.workflow.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDetailDto {
    private WorkflowDto workflow;
    private List<StepDto> steps;
    private List<StepWithRulesDto> stepsWithRules;
}
