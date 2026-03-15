package com.workflow.service;

import com.workflow.dto.RuleDto;
import com.workflow.entity.Rule;
import com.workflow.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;

    @Transactional
    public Optional<RuleDto> addRule(UUID stepId, RuleDto dto) {
        Rule rule = Rule.builder()
                .stepId(stepId)
                .condition(dto.getCondition() != null ? dto.getCondition() : "DEFAULT")
                .nextStepId(dto.getNextStepId())
                .priority(dto.getPriority() != null ? dto.getPriority() : 999)
                .build();
        rule = ruleRepository.save(rule);
        return Optional.of(toDto(rule));
    }

    public List<RuleDto> listByStep(UUID stepId) {
        return ruleRepository.findByStepIdOrderByPriorityAsc(stepId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<RuleDto> update(UUID id, RuleDto dto) {
        return ruleRepository.findById(id).map(rule -> {
            if (dto.getCondition() != null) rule.setCondition(dto.getCondition());
            if (dto.getNextStepId() != null) rule.setNextStepId(dto.getNextStepId());
            if (dto.getPriority() != null) rule.setPriority(dto.getPriority());
            ruleRepository.save(rule);
            return toDto(rule);
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!ruleRepository.existsById(id)) return false;
        ruleRepository.deleteById(id);
        return true;
    }

    private RuleDto toDto(Rule r) {
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
