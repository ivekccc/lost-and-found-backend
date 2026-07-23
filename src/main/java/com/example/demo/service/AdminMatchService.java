package com.example.demo.service;

import com.example.demo.dto.AdminMatchListDto;
import com.example.demo.model.ReportMatch;
import com.example.demo.model.ReportMatchStatus;
import com.example.demo.repository.ReportMatchRepository;
import com.example.demo.repository.ReportMatchSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMatchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReportMatchRepository reportMatchRepository;

    @Transactional(readOnly = true)
    public Page<AdminMatchListDto> getMatches(int page, int size, ReportMatchStatus status, Integer minScore) {
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Specification<ReportMatch> spec = Specification.allOf(
                ReportMatchSpecifications.hasStatus(status),
                ReportMatchSpecifications.scoreAtLeast(minScore)
        );

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize,
                Sort.by(Sort.Direction.DESC, "score").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        return reportMatchRepository.findAll(spec, pageable).map(this::toDto);
    }

    private AdminMatchListDto toDto(ReportMatch match) {
        return new AdminMatchListDto(
                match.getId(),
                match.getScore(),
                match.getDistanceKm().doubleValue(),
                match.getDistanceScore(),
                match.getTimeGapDays(),
                match.getTimeScore(),
                match.getTextSimilarity().doubleValue(),
                match.getTextScore(),
                match.getStatus(),
                match.getLostReport().getId(),
                match.getLostReport().getTitle(),
                match.getLostReport().getStatus(),
                match.getFoundReport().getId(),
                match.getFoundReport().getTitle(),
                match.getFoundReport().getStatus(),
                match.getLostDismissedAt(),
                match.getFoundDismissedAt(),
                match.getNotifiedAt(),
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }
}
