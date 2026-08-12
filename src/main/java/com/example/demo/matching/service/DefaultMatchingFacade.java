package com.example.demo.matching.service;

import com.example.demo.matching.MatchingFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefaultMatchingFacade implements MatchingFacade {

    private final ReportMatchService reportMatchService;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countMatchesFor(List<Long> reportIds) {
        return reportMatchService.getMatchCounts(reportIds);
    }
}
