package com.example.demo.repository;

import com.example.demo.model.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    List<ReportImage> findByReportId(Long reportId);
}
