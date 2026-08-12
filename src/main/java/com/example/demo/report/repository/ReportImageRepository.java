package com.example.demo.report.repository;

import com.example.demo.report.model.ReportImage;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    List<ReportImage> findByReportId(Long reportId);

    boolean existsByPublicId(String publicId);
}
