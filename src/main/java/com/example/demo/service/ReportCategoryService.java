package com.example.demo.service;

import com.example.demo.dto.ReportCategoryDto;
import com.example.demo.repository.ReportCategoryRepository;
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
                reportCategory.getName()
        )).collect(Collectors.toList());
    }
}
