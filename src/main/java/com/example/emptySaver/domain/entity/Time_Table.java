package com.example.emptySaver.domain.entity;

import jakarta.persistence.GeneratedValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "time_table")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Time_Table {
    @Id
    private String id;

    private String title;

    private String gen_member_id;

}
