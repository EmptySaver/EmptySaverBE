package com.example.emptySaver.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document(collection = "schedule")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Schedule {
    @Id
    private String id;

    @DocumentReference(lookup = "{ 'acronym' : ?#{#target} }")
    //@Field("timeTable_id")
    private Time_Table timeTable;

    private String name;
    private String body;

    private Date startDate;
    private int duringTime;

    //private List<Integer> allotTime;
    private boolean isPeriod;
    private boolean isOnlySemester;
}
