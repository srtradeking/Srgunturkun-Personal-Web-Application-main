package com.webapp.backend.mapper;

import com.webapp.backend.model.ReportEvidence;
import org.mapstruct.*;

import java.util.List;

/**
 * Report Evidence Mapper
 *
 * Maps ReportEvidence entity (internal only - no DTO exposed)
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface ReportEvidenceMapper {

    ReportEvidence toEntity(ReportEvidence reportEvidence);

    List<ReportEvidence> toEntityList(List<ReportEvidence> reportEvidences);
}
