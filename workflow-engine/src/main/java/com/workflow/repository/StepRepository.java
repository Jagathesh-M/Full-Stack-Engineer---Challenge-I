package com.workflow.repository;

import com.workflow.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StepRepository extends JpaRepository<Step, UUID> {

    List<Step> findByWorkflowIdOrderByOrderAsc(UUID workflowId);

    long countByWorkflowId(UUID workflowId);
}
