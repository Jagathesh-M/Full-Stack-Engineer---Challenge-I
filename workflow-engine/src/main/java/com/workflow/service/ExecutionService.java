package com.workflow.service;

import com.workflow.dto.ExecutionDto;
import com.workflow.entity.Execution;
import com.workflow.entity.Rule;
import com.workflow.entity.Step;
import com.workflow.engine.RuleEngine;
import com.workflow.repository.ExecutionRepository;
import com.workflow.repository.RuleRepository;
import com.workflow.repository.StepRepository;
import com.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;
    private final StepRepository stepRepository;
    private final RuleRepository ruleRepository;
    private final RuleEngine ruleEngine;

    @Value("${workflow.max-loop-iterations:100}")
    private int maxLoopIterations;

    @Transactional
    public Optional<ExecutionDto> startExecution(UUID workflowId, Map<String, Object> data, UUID triggeredBy) {
        return workflowRepository.findById(workflowId).map(workflow -> {
            UUID startStepId = workflow.getStartStepId();
            if (startStepId == null) {
                Execution failed = Execution.builder()
                        .workflowId(workflowId)
                        .workflowVersion(workflow.getVersion())
                        .status(Execution.ExecutionStatus.failed)
                        .data(data)
                        .logs(List.of(Map.<String, Object>of("error", "Workflow has no start step")))
                        .triggeredBy(triggeredBy)
                        .startedAt(Instant.now())
                        .endedAt(Instant.now())
                        .build();
                return toDto(executionRepository.save(failed));
            }
            Execution execution = Execution.builder()
                    .workflowId(workflowId)
                    .workflowVersion(workflow.getVersion())
                    .status(Execution.ExecutionStatus.in_progress)
                    .data(data != null ? data : new HashMap<>())
                    .logs(new ArrayList<>())
                    .currentStepId(startStepId)
                    .retries(0)
                    .triggeredBy(triggeredBy)
                    .startedAt(Instant.now())
                    .build();
            execution = executionRepository.save(execution);
            runSteps(execution);

            Execution updated = executionRepository.findById(execution.getId()).orElse(execution);

            return toDto(updated);

        });
    }

    private void runSteps(Execution execution) {
        List<Map<String, Object>> logs = execution.getLogs() != null ? new ArrayList<>(execution.getLogs()) : new ArrayList<>();
        Map<String, Object> data = execution.getData() != null ? execution.getData() : new HashMap<>();
        UUID currentStepId = execution.getCurrentStepId();
        int iterations = 0;

        while (currentStepId != null && iterations < maxLoopIterations) {
            iterations++;
            Optional<Step> stepOpt = stepRepository.findById(currentStepId);
            if (stepOpt.isEmpty()) {
                execution.setStatus(Execution.ExecutionStatus.failed);
                execution.setEndedAt(Instant.now());
                execution.setCurrentStepId(null);
                logs.add(Map.of("error", "Step not found: " + currentStepId));
                execution.setLogs(logs);
                executionRepository.save(execution);
                return;
            }
            Step step = stepOpt.get();
            Instant stepStart = Instant.now();
            List<Map<String, Object>> evaluatedRules = new ArrayList<>();
            List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(step.getId());
            UUID nextStepId = null;
            String selectedCondition = null;
            boolean ruleMatched = false;

            for (Rule rule : rules) {
                boolean result = ruleEngine.evaluate(rule.getCondition(), data);
                evaluatedRules.add(Map.of(
                        "rule", rule.getCondition(),
                        "result", result
                ));
                if (result) {
                    nextStepId = rule.getNextStepId();
                    selectedCondition = rule.getCondition();
                    ruleMatched = true;
                    break;
                }
            }

            String nextStepName = null;
            if (nextStepId != null) {
                nextStepName = stepRepository.findById(nextStepId).map(Step::getName).orElse(null);
            }

            Map<String, Object> stepLog = new HashMap<>();
            stepLog.put("step_id", step.getId().toString());
            stepLog.put("step_name", step.getName());
            stepLog.put("step_type", step.getStepType().name());
            stepLog.put("evaluated_rules", evaluatedRules);
            stepLog.put("selected_next_step", nextStepName);
            stepLog.put("selected_condition", selectedCondition);
            stepLog.put("status", "completed");
            stepLog.put("started_at", stepStart.toString());
            stepLog.put("ended_at", Instant.now().toString());
            stepLog.put("approver_id", (Object) null);
            stepLog.put("error_message", (Object) null);
            logs.add(stepLog);

            if (!ruleMatched && !evaluatedRules.isEmpty()) {
                execution.setStatus(Execution.ExecutionStatus.failed);
                execution.setEndedAt(Instant.now());
                execution.setCurrentStepId(null);
                execution.setLogs(logs);
                executionRepository.save(execution);
                return;
            }

            if (nextStepId == null) {
                execution.setStatus(Execution.ExecutionStatus.completed);
                execution.setEndedAt(Instant.now());
                execution.setCurrentStepId(null);
                execution.setLogs(logs);
                executionRepository.save(execution);
                return;
            }

            currentStepId = nextStepId;
            execution.setCurrentStepId(currentStepId);
            execution.setLogs(logs);
            executionRepository.save(execution);
        }

        if (iterations >= maxLoopIterations) {
            execution.setStatus(Execution.ExecutionStatus.failed);
            execution.setEndedAt(Instant.now());
            execution.setCurrentStepId(null);
            logs.add(Map.of("error", "Max loop iterations exceeded"));
            execution.setLogs(logs);
        }
        executionRepository.save(execution);
    }

    public Optional<ExecutionDto> getById(UUID id) {
        return executionRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public Optional<ExecutionDto> cancel(UUID id) {
        return executionRepository.findById(id)
                .filter(e -> e.getStatus() == Execution.ExecutionStatus.pending || e.getStatus() == Execution.ExecutionStatus.in_progress)
                .map(e -> {
                    e.setStatus(Execution.ExecutionStatus.canceled);
                    e.setEndedAt(Instant.now());
                    e.setCurrentStepId(null);
                    return toDto(executionRepository.save(e));
                });
    }

    @Transactional
    public Optional<ExecutionDto> retry(UUID id) {
        return executionRepository.findById(id)
                .filter(e -> e.getStatus() == Execution.ExecutionStatus.failed)
                .map(e -> {
                    e.setRetries(e.getRetries() + 1);
                    e.setStatus(Execution.ExecutionStatus.in_progress);
                    UUID failedStepId = findLastFailedStepId(e.getLogs());
                    if (failedStepId != null) {
                        e.setCurrentStepId(failedStepId);
                        runSteps(e);
                    } else {
                        e.setStatus(Execution.ExecutionStatus.failed);
                        e.setEndedAt(Instant.now());
                    }
                    return toDto(executionRepository.save(e));
                });
    }

    private UUID findLastFailedStepId(List<Map<String, Object>> logs) {
        if (logs == null || logs.isEmpty()) return null;
        for (int i = logs.size() - 1; i >= 0; i--) {
            Map<String, Object> log = logs.get(i);
            if (log.containsKey("step_id")) {
                try {
                    return UUID.fromString((String) log.get("step_id"));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public Page<ExecutionDto> listExecutions(Pageable pageable) {
        return executionRepository.findAll(pageable).map(e -> {
            ExecutionDto dto = toDto(e);
            workflowRepository.findById(e.getWorkflowId()).ifPresent(w -> dto.setWorkflowName(w.getName()));
            return dto;
        });
    }

    private ExecutionDto toDto(Execution e) {
        return ExecutionDto.builder()
                .id(e.getId())
                .workflowId(e.getWorkflowId())
                .workflowVersion(e.getWorkflowVersion())
                .status(e.getStatus())
                .data(e.getData())
                .logs(e.getLogs())
                .currentStepId(e.getCurrentStepId())
                .retries(e.getRetries())
                .triggeredBy(e.getTriggeredBy())
                .startedAt(e.getStartedAt())
                .endedAt(e.getEndedAt())
                .build();
    }
}
