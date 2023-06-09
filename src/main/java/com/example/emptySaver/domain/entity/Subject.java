package com.example.emptySaver.domain.entity;

import jakarta.persistence.Entity;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
//@Builder
@ToString
public class Subject extends Periodic_Schedule{

    private String dept; //학부(과) 정보입니다.
    private String upperDivName; //상위 부서 이름
    private String deptDiv; //by colg
    private String subDiv;  //by dept


    private String subject_div; //교과구분 정보입니다.


    private String subject_div2; //세부영역 정보입니다.
    private String subject_no; //교과번호 정보입니다.

    private String class_div; //  분반 정보입니다.

    private String subjectname; // 교과목명 정보입니다.

    private String shyr; //학년 정보입니다.


    private int credit; //학점 정보입니다.


    private String prof_nm;// 담당교수 정보입니다.

    private String day_night_nm; //주야 정보
    private String class_type; // 강의 유형 정보
    private String class_nm; // 강의시간및강의실
    private String tlsn_count; // 수강인원 정보입니다.

    private String tlsn_limit_count; // 수강 정원 정보
    private String etc_permit_yn; //타과허용 정보입니다.
    private String sec_permit_yn; // 복수전공 정보입니다.

    private String years; //학년도 정보입니다.
    private String term; // 학기 정보

    //private long[] weekScheduleData;

    static public Periodic_Schedule copySchedule(Subject schedule){
        Periodic_Schedule subject = new Subject();
        subject.setWeekScheduleData(schedule.getWeekScheduleData());


        subject.setName(schedule.getSubjectname());
        subject.setBody(schedule.getClass_nm());
        subject.setPublicType(false);

        subject.setGroupType(false);
        subject.setGroupId(schedule.getGroupId());
        subject.setGroupName(schedule.getGroupName());
        subject.setOriginScheduleId(schedule.getId());

        subject.setCategory(schedule.getCategory());
        subject.setSubCategory(schedule.getSubCategory());

        subject.setStartTime(schedule.getStartTime());
        subject.setEndTime(schedule.getEndTime());

        subject.setHideType(schedule.isHideType());
        return subject;
    }
}
