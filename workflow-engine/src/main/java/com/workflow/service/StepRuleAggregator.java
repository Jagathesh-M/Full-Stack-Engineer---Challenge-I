package com.workflow.service;

import com.workflow.dto.RuleDto;
import com.workflow.dto.StepDto;
import com.workflow.dto.StepWithRulesDto;
import com.workflow.entity.Rule;
import com.workflow.entity.Step;
import com.workflow.repository.RuleRepository;
import com.workflow.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StepRuleAggregator {

    private final StepRepository stepRepository;
    private final RuleRepository ruleRepository;

    public List<StepWithRulesDto> aggregate(UUID workflowId) {
        List<Step> steps = stepRepository.findByWorkflowIdOrderByOrderAsc(workflowId);
        return steps.stream().map(step -> {
            List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(step.getId());
            return StepWithRulesDto.builder()
                    .step(toStepDto(step))
                    .rules(rules.stream().map(this::toRuleDto).collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());
    }

    private StepDto toStepDto(Step s) {
        return StepDto.builder()
                .id(s.getId())
                .workflowId(s.getWorkflowId())
                .name(s.getName())
                .stepType(s.getStepType())
                .order(s.getOrder())
                .metadata(s.getMetadata())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private RuleDto toRuleDto(Rule r) {
        return RuleDto.builder()
                .id(r.getId())
                .stepId(r.getStepId())
                .condition(r.getCondition())
                .nextStepId(r.getNextStepId())
                .priority(r.getPriority())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
