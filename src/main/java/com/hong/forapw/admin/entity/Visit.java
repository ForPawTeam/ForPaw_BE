package com.hong.forapw.admin.entity;

import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "visit_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private LocalDateTime date;


    @Builder
    public Visit(User user, LocalDateTime date) {
        this.user = user;
        this.date = date;
    }

    public boolean isSameDate(LocalDate targetDate) {
        return date.toLocalDate().isEqual(targetDate);
    }

    public LocalTime getTruncatedHour() {
        return date.toLocalTime().truncatedTo(ChronoUnit.HOURS);
    }

    public LocalDate getTruncatedDate() {
        return date.toLocalDate();
    }
}
