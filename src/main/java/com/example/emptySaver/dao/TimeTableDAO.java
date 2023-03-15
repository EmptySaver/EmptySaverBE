package com.example.emptySaver.dao;

import com.example.emptySaver.domain.entity.Schedule;
import com.example.emptySaver.domain.entity.Time_Table;

public interface TimeTableDAO {
    Time_Table saveTimeTable(Time_Table timeTable);
    void addScheduleInTimeTable(String timeTableId,Schedule schedule);
}
