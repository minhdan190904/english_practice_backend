package com.minhdan.english_practice_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_streaks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreak {

    @Id
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "max_streak")
    private Integer maxStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;
}
