package com.example.demo.report.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.location.model.City;
import com.example.demo.location.model.Zone;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.notification.model.NotificationType;
import com.example.demo.report.dto.CreateSavedSearchRequestDto;
import com.example.demo.report.dto.SavedSearchDto;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.model.SavedSearch;
import com.example.demo.report.repository.ReportCategoryRepository;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.report.repository.SavedSearchRepository;
import com.example.demo.shared.exception.DuplicateResourceException;
import com.example.demo.shared.exception.InvalidSavedSearchException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedSearchService {

    private static final int MAX_SAVED_SEARCHES = 10;

    private final SavedSearchRepository savedSearchRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final ReportRepository reportRepository;
    private final UserFacade userFacade;
    private final LocationFacade locationFacade;
    private final NotificationFacade notificationFacade;

    @Transactional(readOnly = true)
    public List<SavedSearchDto> getSavedSearches(String userEmail) {
        User user = userFacade.requireByEmail(userEmail);
        Locale locale = LocaleContextHolder.getLocale();
        return savedSearchRepository.findAllOfUser(user.getId()).stream()
                .map(savedSearch -> toDto(savedSearch, locale))
                .toList();
    }

    @Transactional
    public SavedSearchDto createSavedSearch(CreateSavedSearchRequestDto request, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);
        City city = user.getActiveCity();
        String searchText = normalize(request.getSearch());

        if (request.getType() == null && request.getCategoryId() == null
                && request.getZoneId() == null && searchText == null) {
            throw new InvalidSavedSearchException("savedSearch.empty");
        }

        ReportCategory category = request.getCategoryId() == null
                ? null
                : reportCategoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("category.notFound"));

        Zone zone = request.getZoneId() == null
                ? null
                : locationFacade.findZone(request.getZoneId())
                        .filter(found -> found.getCityId().equals(city.getId()))
                        .orElseThrow(() -> new InvalidSavedSearchException("savedSearch.zoneNotInCity"));

        List<SavedSearch> existing = savedSearchRepository.findAllOfUser(user.getId());
        boolean duplicate = existing.stream().anyMatch(savedSearch ->
                savedSearch.getCity().getId().equals(city.getId())
                        && savedSearch.getType() == request.getType()
                        && Objects.equals(idOf(savedSearch.getCategory()), request.getCategoryId())
                        && Objects.equals(savedSearch.getZone() == null ? null : savedSearch.getZone().getId(),
                                request.getZoneId())
                        && sameText(savedSearch.getSearchText(), searchText));
        if (duplicate) {
            throw new DuplicateResourceException("savedSearch.duplicate");
        }
        if (existing.size() >= MAX_SAVED_SEARCHES) {
            throw new InvalidSavedSearchException("savedSearch.limit", MAX_SAVED_SEARCHES);
        }

        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setUser(user);
        savedSearch.setCity(city);
        savedSearch.setType(request.getType());
        savedSearch.setCategory(category);
        savedSearch.setZone(zone);
        savedSearch.setSearchText(searchText);

        return toDto(savedSearchRepository.save(savedSearch), LocaleContextHolder.getLocale());
    }

    @Transactional
    public void deleteSavedSearch(Long savedSearchId, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);
        SavedSearch savedSearch = savedSearchRepository.findByIdAndUserId(savedSearchId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("savedSearch.notFound"));
        savedSearchRepository.delete(savedSearch);
    }

    @Transactional
    public void notifySubscribers(Long reportId) {
        List<Long> subscriberIds = savedSearchRepository.findSubscribersOf(reportId);
        if (subscriberIds.isEmpty()) {
            return;
        }
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return;
        }

        String keyPrefix = report.getType() == ReportType.LOST
                ? "notification.savedSearchLost"
                : "notification.savedSearchFound";
        String dataJson = "{\"reportId\":" + report.getId() + "}";
        String place = placeOf(report);

        for (Long subscriberId : subscriberIds) {
            notificationFacade.notify(subscriberId, NotificationType.SAVED_SEARCH_MATCH,
                    keyPrefix + ".title", keyPrefix + ".body", dataJson, report.getTitle(), place);
        }
        log.info("Report {} matched saved searches of {} users", reportId, subscriberIds.size());
    }

    @Transactional
    public void deleteSavedSearchesOfUser(Long userId) {
        savedSearchRepository.deleteByUserId(userId);
    }

    private String placeOf(Report report) {
        Zone zone = report.getLocation() == null ? null : report.getLocation().getZone();
        if (zone == null) {
            return "";
        }
        return zone.getParentName() == null ? zone.getName() : zone.getName() + ", " + zone.getParentName();
    }

    private SavedSearchDto toDto(SavedSearch savedSearch, Locale locale) {
        ReportCategory category = savedSearch.getCategory();
        Zone zone = savedSearch.getZone();
        return new SavedSearchDto(
                savedSearch.getId(),
                savedSearch.getCity().getId(),
                savedSearch.getCity().getName(),
                savedSearch.getType(),
                idOf(category),
                category == null ? null : category.localizedName(locale),
                zone == null ? null : zone.getId(),
                zone == null ? null : zone.getName(),
                zone == null ? null : zone.getParentId(),
                zone == null ? null : zone.getParentName(),
                savedSearch.getSearchText(),
                savedSearch.getCreatedAt());
    }

    private Long idOf(ReportCategory category) {
        return category == null ? null : category.getId();
    }

    private String normalize(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim().replaceAll("\\s+", " ");
    }

    private boolean sameText(String left, String right) {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right);
    }
}
