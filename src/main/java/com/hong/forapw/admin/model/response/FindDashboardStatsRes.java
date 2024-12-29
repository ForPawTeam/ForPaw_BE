package com.hong.forapw.admin.model.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record FindDashboardStatsRes(
        UserStatsDTO userStatsDTO,
        AnimalStatsDTO animalStatsDTO,
        List<DailyVisitorDTO> dailyVisitorDTOS,
        List<HourlyVisitorDTO> hourlyVisitorDTOS,
        DailySummaryDTO dailySummaryDTO
) {

    public record UserStatsDTO(Long activeUsers, Long inactiveUsers) {
    }

    public record AnimalStatsDTO(
            Long waitingForAdoption,
            Long adoptionProcessing,
            Long adoptedRecently,
            Long adoptedTotal) {
    }

    public record DailyVisitorDTO(LocalDate date, Long visitors) {

        public DailyVisitorDTO(Map.Entry<LocalDate, Long> entry){
            this(
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    public record HourlyVisitorDTO(LocalDateTime hour, Long visitors) {

        public HourlyVisitorDTO(Map.Entry<LocalTime, Long> entry){
            this(
                    LocalDateTime.of(LocalDate.now(), entry.getKey()),
                    entry.getValue()
            );
        }
    }

    public record DailySummaryDTO(
            Long entries,
            Long newPost,
            Long newComment,
            Long newAdoptApplication) {
    }
}