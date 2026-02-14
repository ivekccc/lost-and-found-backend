package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByUserId(Long userId);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    List<Report> findByStatusNot(ReportStatus status);

    Page<Report> findByType(ReportType type, Pageable pageable);

    List<Report> findByTypeAndStatusNot(ReportType type, ReportStatus status);

    Page<Report> findByTypeAndStatus(ReportType type, ReportStatus status, Pageable pageable);

    Page<Report> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Report> findByStatusNot(ReportStatus status, Pageable pageable);
}
