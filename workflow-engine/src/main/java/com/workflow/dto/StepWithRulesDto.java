package com.workflow.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepWithRulesDto {
    private StepDto step;
    private List<RuleDto> rules;
}
