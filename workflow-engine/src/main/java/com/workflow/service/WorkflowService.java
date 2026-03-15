package com.workflow.service;

import com.workflow.dto.StepDto;
import com.workflow.dto.WorkflowDetailDto;
import com.workflow.dto.WorkflowDto;
import com.workflow.dto.WorkflowListDto;
import com.workflow.entity.Step;
import com.workflow.entity.Workflow;
import com.workflow.repository.StepRepository;
import com.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final StepRepository stepRepository;
    private final StepRuleAggregator stepRuleAggregator;

    @Transactional
    public WorkflowDto create(WorkflowDto dto) {
        Workflow w = Workflow.builder()
                .name(dto.getName())
                .version(1)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .inputSchema(dto.getInputSchema())
                .startStepId(dto.getStartStepId())
                .build();
        w = workflowRepository.save(w);
        return toDto(w);
    }

    public Page<WorkflowListDto> list(String search, Pageable pageable) {
        Page<Workflow> page = search != null && !search.isBlank()
                ? workflowRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
                : workflowRepository.findAll(pageable);
        return page.map(w -> toListDto(w, stepRepository.countByWorkflowId(w.getId())));
    }

    private WorkflowListDto toListDto(Workflow w, long stepCount) {
        return WorkflowListDto.builder()
                .id(w.getId())
                .name(w.getName())
                .version(w.getVersion())
                .isActive(w.getIsActive())
                .inputSchema(w.getInputSchema())
                .startStepId(w.getStartStepId())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .stepCount(stepCount)
                .build();
    }

    public Optional<WorkflowDto> getById(UUID id) {
        return workflowRepository.findById(id).map(this::toDto);
    }

    public Optional<WorkflowDetailDto> getDetailById(UUID id) {
        return workflowRepository.findById(id).map(w -> {
            List<Step> steps = stepRepository.findByWorkflowIdOrderByOrderAsc(id);
            return WorkflowDetailDto.builder()
                    .workflow(toDto(w))
                    .steps(steps.stream().map(this::stepToDto).collect(Collectors.toList()))
                    .stepsWithRules(stepRuleAggregator.aggregate(id))
                    .build();
        });
    }

    @Transactional
    public Optional<WorkflowDto> update(UUID id, WorkflowDto dto) {
        return workflowRepository.findById(id).map(w -> {
            w.setVersion(w.getVersion() + 1);
            if (dto.getName() != null) w.setName(dto.getName());
            if (dto.getInputSchema() != null) w.setInputSchema(dto.getInputSchema());
            if (dto.getStartStepId() != null) w.setStartStepId(dto.getStartStepId());
            if (dto.getIsActive() != null) w.setIsActive(dto.getIsActive());
            workflowRepository.save(w);
            return toDto(w);
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!workflowRepository.existsById(id)) return false;
        workflowRepository.deleteById(id);
        return true;
    }

    private WorkflowDto toDto(Workflow w) {
        return WorkflowDto.builder()
                .id(w.getId())
                .name(w.getName())
                .version(w.getVersion())
                .isActive(w.getIsActive())
                .inputSchema(w.getInputSchema())
                .startStepId(w.getStartStepId())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    StepDto stepToDto(Step s) {
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
}
