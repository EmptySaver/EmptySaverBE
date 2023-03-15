package com.example.emptySaver.repository;

import com.example.emptySaver.domain.entity.Time_Table;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TimeTableRepository extends MongoRepository<Time_Table,String> {

}
