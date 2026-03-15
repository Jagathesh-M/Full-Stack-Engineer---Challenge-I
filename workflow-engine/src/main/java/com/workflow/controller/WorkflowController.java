package com.workflow.controller;

import com.workflow.dto.WorkflowDetailDto;
import com.workflow.dto.WorkflowDto;
import com.workflow.dto.WorkflowListDto;
import com.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@CrossOrigin
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public ResponseEntity<WorkflowDto> create(@RequestBody WorkflowDto dto) {
        return ResponseEntity.ok(workflowService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<WorkflowListDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workflowService.list(search, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDto> getById(@PathVariable UUID id) {
        return workflowService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<WorkflowDetailDto> getDetail(@PathVariable UUID id) {
        return workflowService.getDetailById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDto> update(@PathVariable UUID id, @RequestBody WorkflowDto dto) {
        return workflowService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return workflowService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
