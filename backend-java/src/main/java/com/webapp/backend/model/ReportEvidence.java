package com.webapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "report_evidence")
@IdClass(ReportEvidenceId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportEvidence {

    @Id
    @Column(name = "report_id")
    private UUID reportId;

    @Id
    @Column(name = "evidence", length = 512)
    private String evidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", insertable = false, updatable = false)
    private Report report;
}
