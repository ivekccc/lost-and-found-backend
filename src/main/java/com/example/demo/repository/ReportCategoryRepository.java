package com.example.demo.repository;

import com.example.demo.model.ReportCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportCategoryRepository extends JpaRepository<ReportCategory, Long> {
    Optional<ReportCategory> findByName(String name);

    List<ReportCategory> findByIsActiveTrue();
}
