package com.example.emptySaver.repository;

import com.example.emptySaver.domain.entity.Time_Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

@DataMongoTest
class TimeTableRepositoryTest {
    @Autowired
    private TimeTableRepository repository;

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
}