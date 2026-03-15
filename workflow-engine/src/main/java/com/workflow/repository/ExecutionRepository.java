package com.workflow.repository;

import com.workflow.entity.Execution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

    Page<Execution> findByWorkflowId(UUID workflowId, Pageable pageable);
}
