package com.bae4.drivedrop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    private String googleSub;
    private String email;
    @Column(length = 512)
    private String refreshToken;
    private Integer totalShares = 0; // TODO: FINISH COUNTING LOGIC
}
