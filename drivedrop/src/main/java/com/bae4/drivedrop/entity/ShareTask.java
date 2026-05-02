package com.bae4.drivedrop.entity;

import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.enums.TaskStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Entity
public class ShareTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private ShareMode shareMode;

    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus = TaskStatus.PENDING_FILE;

    @ManyToOne
    private User owner;

    private String googleFileId;
    @Column(length = 2048)
    private String uploadUrl;
    private String fileName;

    private LocalDateTime activatedAt;
    private Integer targetDownloadCount;
    private boolean isFileDeleted = false;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<AccessCode> accessCodes;
}
