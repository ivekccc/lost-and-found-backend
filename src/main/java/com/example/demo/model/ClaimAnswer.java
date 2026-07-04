package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "claim_answers")
@Getter
@Setter
public class ClaimAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_question_id", nullable = false)
    private ChallengeQuestion question;

    @Column(name = "answer_text", nullable = false, length = 1000)
    private String answerText;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}
