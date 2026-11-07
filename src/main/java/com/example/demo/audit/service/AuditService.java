package com.example.demo.audit.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.util.Optional;
import com.example.demo.audit.AdminActionEvent;
import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.audit.dto.AuditEntryDto;
import com.example.demo.audit.model.AdminAuditEntry;
import com.example.demo.audit.repository.AdminAuditEntryRepository;
import com.example.demo.shared.web.PageQuery;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String SYSTEM_ACTOR = "System";
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of("createdAt", "createdAt", "id", "id");

    private final AdminAuditEntryRepository repository;
    private final UserFacade userFacade;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AdminActionEvent event) {
        AdminAuditEntry entry = new AdminAuditEntry();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> actor = authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()
                ? Optional.empty()
                : userFacade.findByEmail(authentication.getName());
        entry.setActorId(actor.map(User::getId).orElse(null));
        entry.setActorName(actor.map(this::displayName).orElse(SYSTEM_ACTOR));
        entry.setAction(event.action());
        entry.setTargetType(event.targetType());
        entry.setTargetId(event.targetId());
        entry.setSummary(event.summary());
        entry.setDetails(event.details());
        entry.setCreatedAt(LocalDateTime.now());
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditEntryDto> getEntries(AuditAction action, AuditTargetType targetType, Long targetId, Long actorId,
                                          Integer page, Integer size, String sort, String direction) {
        Specification<AdminAuditEntry> spec = Specification.allOf(
                equalTo("action", action),
                equalTo("targetType", targetType),
                equalTo("targetId", targetId),
                equalTo("actorId", actorId));
        PageRequest pageRequest = PageQuery.of(page, size, sort, direction, SORTABLE_COLUMNS, "createdAt", Sort.Direction.DESC);
        return repository.findAll(spec, pageRequest).map(this::toDto);
    }

    private Specification<AdminAuditEntry> equalTo(String attribute, Object value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get(attribute), value);
    }

    private AuditEntryDto toDto(AdminAuditEntry entry) {
        return new AuditEntryDto(entry.getId(), entry.getActorId(), entry.getActorName(), entry.getAction(),
                entry.getTargetType(), entry.getTargetId(), entry.getSummary(), entry.getDetails(), entry.getCreatedAt());
    }

    private String displayName(User user) {
        String combined = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return combined.isEmpty() ? user.getUsername() : combined;
    }
}
