package com.workflow.controller;

import com.workflow.dto.ExecutionDto;
import com.workflow.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<ExecutionDto> execute(
            @PathVariable UUID workflowId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = body.containsKey("data") ? (Map<String, Object>) body.get("data") : body;
        UUID triggeredBy = body.containsKey("triggeredBy") && body.get("triggeredBy") != null
                ? UUID.fromString(body.get("triggeredBy").toString()) : null;
        return executionService.startExecution(workflowId, data, triggeredBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionDto> getById(@PathVariable UUID id) {
        return executionService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/executions/{id}/cancel")
    public ResponseEntity<ExecutionDto> cancel(@PathVariable UUID id) {
        return executionService.cancel(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/executions/{id}/retry")
    public ResponseEntity<ExecutionDto> retry(@PathVariable UUID id) {
        return executionService.retry(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/executions")
    public ResponseEntity<Page<ExecutionDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(executionService.listExecutions(PageRequest.of(page, size)));
    }
}
