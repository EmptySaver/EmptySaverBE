package com.example.emptySaver.repository;

import com.example.emptySaver.domain.entity.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataMongoTest
class ScheduleRepositoryTest {
    @Autowired
    private ScheduleRepository repository;

    @BeforeEach
    void beforeEach(){
        repository.deleteAll();
    }

    @DisplayName("Schedule 저장 테스트")
    @Test
    void saveSchedule(){
        Schedule schedule = Schedule.builder().name("헬스 ㄱ").body("상체하는 날").build();
        Schedule savedSchedule = repository.save(schedule);
        Optional<Schedule> foundSchedule = repository.findById(savedSchedule.getId());

        assertThat(foundSchedule.get().getId()).isEqualTo(savedSchedule.getId());
        assertThat(foundSchedule.get().getBody()).isEqualTo(savedSchedule.getBody());
    }

}