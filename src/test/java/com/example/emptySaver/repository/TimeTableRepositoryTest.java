package com.example.emptySaver.repository;

import com.example.emptySaver.dao.TimeTableDAO;
import com.example.emptySaver.domain.entity.Schedule;
import com.example.emptySaver.domain.entity.Time_Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

@ComponentScan(basePackages = "com.example.emptySaver")
@DataMongoTest
class TimeTableRepositoryTest {
    @Autowired
    private TimeTableRepository repository;
    @Autowired
    private TimeTableDAO timeTableDAO;

    @BeforeEach
    void beforeEach(){
        repository.deleteAll();
    }

    @Test
    void createTimeTable(){
        Time_Table timeTable = Time_Table.builder().title("흘러넘처").build();
        Time_Table savedTimeTable = repository.save(timeTable);
        Optional<Time_Table> foundTable = repository.findById(savedTimeTable.getId());

        assertThat(foundTable.get().getTitle()).isEqualTo(timeTable.getTitle());
    }

    @DisplayName("시간표에 일정을 reference로 추가, 저장")
    @Test
    void saveWithSchedule(){
        //given
        Time_Table timeTableEntity = Time_Table.builder().id("added").title("흘러넘처").build();
        Schedule scheduleEntity = Schedule.builder().name("헬스 ㄱ").body("상체하는 날").build();

        //when
        Time_Table savedTimeTable = repository.save(timeTableEntity);
        Time_Table foundTable = repository.findById(savedTimeTable.getId()).get();

        timeTableDAO.addScheduleInTimeTable(foundTable.getId(), scheduleEntity);

        //then
        foundTable = repository.findById(savedTimeTable.getId()).get();
        //System.out.println(foundTable);
        assertThat(foundTable.getSchedule_list().size()).isEqualTo(1);

        assertThat(foundTable.getSchedule_list().get(0).getId()).isEqualTo(scheduleEntity.getId());
        System.out.println(foundTable.getSchedule_list().get(0).getTimeTable().getTitle());
    }
}