package com.example.emptySaver.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class TimeTableDto {
    @Data
    @Builder
    public static class TimeTableInfo{
        private LocalDate startDate;
        private LocalDate endData;
        @Builder.Default
        private List<List<Boolean>> bitListsPerDay = new ArrayList<>();
        @Builder.Default
        private List<List<ScheduleDto>> scheduleListPerDays = new ArrayList<List<ScheduleDto>>();
    }

    @Data
    @Builder
    public static class ScheduleDto{
        private Long id;
        private String name;
        private String body;
        private Long timeBitData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulePostDto{
        private String name;
        private String body;
        private String periodicType;

        @Builder.Default
        private List<String> periodicTimeStringList = new ArrayList<>(); //only for Periodic

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;    //only for Non_Periodic
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;      //only for Non_Periodic
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeTableRequestForm{
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
    }
}