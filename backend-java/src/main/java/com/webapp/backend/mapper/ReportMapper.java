package com.webapp.backend.mapper;

import com.webapp.backend.dto.ReportsDTO;
import com.webapp.backend.model.Report;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Report Mapper
 *
 * Maps between Report entity and ReportsDTO using MapStruct.
 * Handles UUID id conversion and evidence list conversion to DTOs
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface ReportMapper {

    @Mapping(target = "id", ignore = true)
    ReportsDTO toDto(Report report);

    @Mapping(target = "id", ignore = true)
    Report toEntity(ReportsDTO reportDTO);

    List<ReportsDTO> toDtoList(List<Report> reports);

    List<Report> toEntityList(List<ReportsDTO> reportDTOs);
}
