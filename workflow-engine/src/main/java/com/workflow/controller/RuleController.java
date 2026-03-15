package com.workflow.controller;

import com.workflow.dto.RuleDto;
import com.workflow.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class RuleController {

    private final RuleService ruleService;

    @PostMapping("/steps/{stepId}/rules")
    public ResponseEntity<RuleDto> addRule(@PathVariable UUID stepId, @RequestBody RuleDto dto) {
        return ruleService.addRule(stepId, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/steps/{stepId}/rules")
    public ResponseEntity<List<RuleDto>> listRules(@PathVariable UUID stepId) {
        return ResponseEntity.ok(ruleService.listByStep(stepId));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<RuleDto> update(@PathVariable UUID id, @RequestBody RuleDto dto) {
        return ruleService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return ruleService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
