package com.example.emptySaver.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "schedule")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Schedule {
    @Id
    private String id;

    private String name;
    private String body;

}
