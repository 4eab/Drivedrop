package com.bae4.drivedrop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class AccessCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private ShareTask task;

    private boolean used = false; // Download Finished
    private boolean assigned = false; // Someone opens the download link
    private LocalDateTime usedAt;
    private LocalDateTime assignedAt;

    public AccessCode(String code, ShareTask task) {
        this.code = code;
        this.task = task;
    }

    public AccessCode() {}
}
