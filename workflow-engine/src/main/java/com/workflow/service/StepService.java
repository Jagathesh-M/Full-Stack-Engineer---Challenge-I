package com.workflow.service;

import com.workflow.dto.StepDto;
import com.workflow.entity.Step;
import com.workflow.entity.Workflow;
import com.workflow.repository.StepRepository;
import com.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StepService {

    private final StepRepository stepRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowService workflowService;

    @Transactional
    public Optional<StepDto> addStep(UUID workflowId, StepDto dto) {
        if (!workflowRepository.existsById(workflowId)) return Optional.empty();
        int order = dto.getOrder() != null ? dto.getOrder() : (int) stepRepository.findByWorkflowIdOrderByOrderAsc(workflowId).stream().count();
        Step step = Step.builder()
                .workflowId(workflowId)
                .name(dto.getName())
                .stepType(dto.getStepType())
                .order(order)
                .metadata(dto.getMetadata())
                .build();
        step = stepRepository.save(step);
        Optional<Workflow> wf = workflowRepository.findById(workflowId);
        if (wf.isPresent() && wf.get().getStartStepId() == null) {
            Workflow w = wf.get();
            w.setStartStepId(step.getId());
            workflowRepository.save(w);
        }
        return Optional.of(workflowService.stepToDto(step));
    }

    public List<StepDto> listByWorkflow(UUID workflowId) {
        return stepRepository.findByWorkflowIdOrderByOrderAsc(workflowId).stream()
                .map(workflowService::stepToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<StepDto> update(UUID id, StepDto dto) {
        return stepRepository.findById(id).map(step -> {
            if (dto.getName() != null) step.setName(dto.getName());
            if (dto.getStepType() != null) step.setStepType(dto.getStepType());
            if (dto.getOrder() != null) step.setOrder(dto.getOrder());
            if (dto.getMetadata() != null) step.setMetadata(dto.getMetadata());
            stepRepository.save(step);
            return workflowService.stepToDto(step);
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!stepRepository.existsById(id)) return false;
        stepRepository.deleteById(id);
        return true;
    }

    public Optional<Step> getStepEntity(UUID id) {
        return stepRepository.findById(id);
    }
}
