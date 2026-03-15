package com.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private List<Map<String, Object>> logs;

    @Column(name = "current_step_id")
    private UUID currentStepId;

    @Column(nullable = false)
    private Integer retries = 0;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public enum ExecutionStatus {
        pending,
        in_progress,
        completed,
        failed,
        canceled
    }
}
