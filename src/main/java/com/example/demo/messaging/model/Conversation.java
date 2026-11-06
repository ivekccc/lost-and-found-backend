package com.example.demo.messaging.model;

import com.example.demo.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false, unique = true)
    private Long claimId;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false)
    private User holder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "holder_read_at")
    private LocalDateTime holderReadAt;

    @Column(name = "claimant_read_at")
    private LocalDateTime claimantReadAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean hasParticipant(Long userId) {
        return holder.getId().equals(userId) || claimant.getId().equals(userId);
    }

    public ConversationRole roleOf(Long userId) {
        return holder.getId().equals(userId) ? ConversationRole.HOLDER : ConversationRole.CLAIMANT;
    }

    public User otherParticipant(Long userId) {
        return holder.getId().equals(userId) ? claimant : holder;
    }

    public LocalDateTime readAtOf(Long userId) {
        return holder.getId().equals(userId) ? holderReadAt : claimantReadAt;
    }

    public void markReadBy(Long userId, LocalDateTime at) {
        if (holder.getId().equals(userId)) {
            holderReadAt = at;
        } else {
            claimantReadAt = at;
        }
    }
}
