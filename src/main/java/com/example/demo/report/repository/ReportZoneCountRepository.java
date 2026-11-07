package com.example.demo.report.repository;

import com.example.demo.report.dto.ZoneReportCountDto;
import com.example.demo.report.model.Report;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ReportZoneCountRepository {

    List<ZoneReportCountDto> countByZone(Specification<Report> specification);
}
