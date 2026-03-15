package com.workflow.controller;

import com.workflow.dto.StepDto;
import com.workflow.service.StepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class StepController {

    private final StepService stepService;

    @PostMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<StepDto> addStep(@PathVariable UUID workflowId, @RequestBody StepDto dto) {
        return stepService.addStep(workflowId, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<List<StepDto>> listSteps(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(stepService.listByWorkflow(workflowId));
    }

    @PutMapping("/steps/{id}")
    public ResponseEntity<StepDto> update(@PathVariable UUID id, @RequestBody StepDto dto) {
        return stepService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/steps/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return stepService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
