package com.workflow.repository;

import com.workflow.entity.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    Page<Workflow> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Workflow> findByIdAndIsActiveTrue(UUID id);

    Optional<Workflow> findFirstByOrderByVersionDesc();
}
