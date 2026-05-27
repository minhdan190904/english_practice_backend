package com.minhdan.english_practice_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSetting {

    @Id
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "daily_goal")
    private Integer dailyGoal;

    @Column(name = "is_notification_enabled")
    private Boolean isNotificationEnabled;
}
