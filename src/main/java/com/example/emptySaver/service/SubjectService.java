package com.example.emptySaver.service;

import com.example.emptySaver.domain.dto.SubjectDto;
import com.example.emptySaver.domain.dto.TimeTableDto;
import com.example.emptySaver.domain.entity.*;
import com.example.emptySaver.repository.MemberRepository;
import com.example.emptySaver.repository.ScheduleRepository;
import com.example.emptySaver.repository.SubjectRepository;
import com.example.emptySaver.utils.UosSubjectAutoSaver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final UosSubjectAutoSaver uosSubjectAutoSaver;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void saveAllSubjectByYearAndTerm(String years, String term){
        boolean isExistInDB = subjectRepository.existsByYearsAndTerm(years, term);
        if(isExistInDB){
            log.info("year: " + years +", term"+ term + " is already saved");
            return;
        }

        uosSubjectAutoSaver.saveAllSubjectByTerm(years,term);
        log.info("year: " + years +", term"+ term + " saved Complete");
    }

    private List<SubjectDto.SubjectInfo> subjectToDtoConvert(List<Subject> subjectList){
        List<SubjectDto.SubjectInfo> ret = new ArrayList<>();

        for (Subject subject: subjectList) {
            SubjectDto.SubjectInfo subjectInfo = SubjectDto.SubjectInfo.builder()
                    .subjectname(subject.getSubjectname())
                    .dept(subject.getDept())
                    .subject_div(subject.getSubject_div())
                    .subject_div2(subject.getSubject_div2())
                    .class_div(subject.getClass_div())
                    .shyr(subject.getShyr())
                    .credit(subject.getCredit())
                    .prof_nm(subject.getProf_nm())
                    .class_type(subject.getClass_type())
                    .class_nm(subject.getClass_nm())
                    .build();
            ret.add(subjectInfo);
            //이거 일일히 다 채우기
        }

        return ret;
    }

    public List<SubjectDto.SubjectInfo> getSubjectsMatchedName(String name){
        List<Subject> searchedList = subjectRepository.findBySubjectnameContaining(name);

        return subjectToDtoConvert(searchedList);
    }

    private Periodic_Schedule convertSubjectToPeriodicSchedule(Subject subject) {
        Periodic_Schedule periodicSchedule = new Periodic_Schedule();
        periodicSchedule.setWeekScheduleData(subject.getWeekScheduleData());
        periodicSchedule.setName(subject.getSubjectname());
        periodicSchedule.setBody(subject.getClass_nm());
        return periodicSchedule;
    }

    //Member의 periodic schedule로서 저장시키기
    @Transactional
    public void saveSubjectToMemberSchedule(Long memberId, Long subjectId){
        Member member = memberRepository.findById(memberId).get();

        Subject subject = subjectRepository.findById(subjectId).get();
        Periodic_Schedule schedule = this.convertSubjectToPeriodicSchedule(subject);

        Time_Table timeTable = member.getTimeTable();
        schedule.setTimeTable(timeTable);
        Schedule savedSchedule = scheduleRepository.save(schedule);

        List<Schedule> scheduleList = timeTable.getScheduleList();
        scheduleList.add(savedSchedule);
        timeTable.calcAllWeekScheduleData();
        log.info("add Subject Schedule"+ savedSchedule.getId() + " to Member" + member.getId());
    }
}
