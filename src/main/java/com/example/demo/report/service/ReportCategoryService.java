package com.example.demo.report.service;

import org.springframework.context.i18n.LocaleContextHolder;

import com.example.demo.report.dto.ReportCategoryDto;
import com.example.demo.report.repository.ReportCategoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ReportCategoryService {
    private final ReportCategoryRepository reportCategoryRepository;

    public List<ReportCategoryDto> getAllActiveCategories(){
        return reportCategoryRepository.findByIsActiveTrue().stream().map(reportCategory -> new ReportCategoryDto(
                reportCategory.getId(),
                reportCategory.localizedName(LocaleContextHolder.getLocale()),
                reportCategory.getMinQuestions(),
                reportCategory.getImageUrl()
        )).collect(Collectors.toList());
    }
}
