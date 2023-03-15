package com.example.emptySaver.repository;

import com.example.emptySaver.domain.entity.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScheduleRepository extends MongoRepository<Schedule,String> {
}
