package com.example.emptySaver.dao.impl;

import com.example.emptySaver.dao.TimeTableDAO;
import com.example.emptySaver.domain.entity.Schedule;
import com.example.emptySaver.domain.entity.Time_Table;
import com.example.emptySaver.repository.ScheduleRepository;
import com.example.emptySaver.repository.TimeTableRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Component  //빈으로 등록되어 다른 클래스가 인터페이스로 의존성 주입 받을 때 자동 등록됨
@RequiredArgsConstructor
public class TimeTableDAOImpl implements TimeTableDAO {

    @Autowired
    private final TimeTableRepository timeTableRepository;

    @Autowired
    private final MongoTemplate template;

    @Autowired
    private final ScheduleRepository scheduleRepository;

    @Override
    public Time_Table saveTimeTable(Time_Table timeTable) {
        Time_Table savedEntity = timeTableRepository.save(timeTable);
        return savedEntity;
    }

    @Override
    public void addScheduleInTimeTable(String timeTableId,Schedule schedule) {
        Schedule savedSchedule = scheduleRepository.save(schedule);

        UpdateResult updateResult = template.update(Time_Table.class)
                .matching(where("id").is(timeTableId))
                .apply(new Update().push("schedule_list", savedSchedule))
                .first();

        if(updateResult.getModifiedCount() == 0){ //write 가 실패한 경우
            //throw new Exception();
        }

        log.info("addScheduleInTimeTable timeTableId={}", timeTableId);
    }
}
