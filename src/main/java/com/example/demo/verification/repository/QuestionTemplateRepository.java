package com.example.demo.verification.repository;

import com.example.demo.verification.model.QuestionTemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {

    List<QuestionTemplate> findByCategoryIdAndIsActiveTrueOrderByIdAsc(Long categoryId);

    List<QuestionTemplate> findByCategoryIdOrderByIdAsc(Long categoryId);
}
