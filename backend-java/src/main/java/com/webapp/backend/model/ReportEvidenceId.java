package com.webapp.backend.model;

import java.io.Serializable;
import java.util.UUID;
import java.util.Objects;

public class ReportEvidenceId implements Serializable {
    private UUID reportId;
    private String evidence;

    public ReportEvidenceId() {}

    public ReportEvidenceId(UUID reportId, String evidence) {
        this.reportId = reportId;
        this.evidence = evidence;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportEvidenceId that = (ReportEvidenceId) o;
        return Objects.equals(reportId, that.reportId) && Objects.equals(evidence, that.evidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, evidence);
    }
}
