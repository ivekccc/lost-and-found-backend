package com.example.demo.repository;

import com.example.demo.model.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {

    List<QuestionTemplate> findByCategoryIdAndIsActiveTrueOrderByIdAsc(Long categoryId);

    List<QuestionTemplate> findByCategoryIdOrderByIdAsc(Long categoryId);
}
