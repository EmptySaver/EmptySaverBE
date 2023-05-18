package com.example.emptySaver.service.impl;

import com.example.emptySaver.domain.dto.GroupDto;
import com.example.emptySaver.domain.dto.TimeTableDto;
import com.example.emptySaver.domain.entity.*;
import com.example.emptySaver.errorHandler.BaseException;
import com.example.emptySaver.errorHandler.BaseResponseStatus;
import com.example.emptySaver.repository.*;
import com.example.emptySaver.service.FCMService;
import com.example.emptySaver.service.GroupService;
import com.example.emptySaver.service.MemberService;
import com.example.emptySaver.service.TimeTableService;
import com.example.emptySaver.utils.TimeDataSuperUltraConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeTableServiceImpl implements TimeTableService {
    @PersistenceContext
    private final EntityManager em;

    private final TimeDataSuperUltraConverter timeDataConverter;
    private final ScheduleRepository scheduleRepository;
    private final PeriodicScheduleRepository periodicScheduleRepository;
    private final NonPeriodicScheduleRepository nonPeriodicScheduleRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    private final GroupService groupService;
    private final MemberService memberService;
    private final FCMService fcmService;

    private Map<String,Integer> dayToIntMap = Map.of("월",0, "화", 1,"수",2,"목",3,"금",4,"토",5,"일",6);

    public Schedule getScheduleById(final Long scheduleId) {
        Optional<Schedule> scheduleOptional = scheduleRepository.findById(scheduleId);

        if(scheduleOptional.isEmpty())  {//존재x 스케줄
            throw new BaseException(BaseResponseStatus.NOT_EXIST_SCHEDULE_ID);
        }

        return scheduleOptional.get();
    }

    @Override
    public List<TimeTableDto.SearchedScheduleDto> convertScheduleListToSearchedScheduleDtoList(List<Schedule> scheduleList){
        List<TimeTableDto.SearchedScheduleDto> retList = new ArrayList<>();

        for (Schedule schedule : scheduleList) {
            TimeTableDto.SearchedScheduleDto build = TimeTableDto.SearchedScheduleDto.builder()
                    .id(schedule.getId())
                    .name(schedule.getName())
                    .body(schedule.getBody())
                    //.groupInfo(groupService.getGroupDetails(schedule.getTimeTable().getTeam().getId()))
                    .timeData(this.timeDataConverter.convertScheduleTimeDataToString(schedule))
                    .periodicType(false)
                    .build();
            Team team = schedule.getTimeTable().getTeam();
            GroupDto.DetailGroupRes groupBuild = GroupDto.DetailGroupRes.builder()
                    .groupId(team.getId()).groupName(team.getName()).oneLineInfo(team.getOneLineInfo())
                    .groupDescription(team.getDescription())//.nowMember(Long.valueOf(memberTeamRepository.countByTeam(team)))
                    .maxMember(team.getMaxMember()).isPublic(team.isPublic())//.categoryLabel(categoryService.getLabelByCategory(team.getCategory()))
                    .build();

            build.setGroupInfo(groupBuild);

            if (schedule instanceof Periodic_Schedule)
                build.setPeriodicType(true);

            retList.add(build);
        }

        return retList;
    }

    @Override
    public List<TimeTableDto.SearchedScheduleDto> getSearchedScheduleDtoList(final TimeTableDto.ScheduleSearchRequestForm searchForm){
        int dayOfWeek = searchForm.getEndTime().getDayOfWeek().getValue() -1;

        List<Schedule> includedScheduleList = new ArrayList<>();

        List<Periodic_Schedule> periodicScheduleList = periodicScheduleRepository.findByPublicType(true);
        for (Periodic_Schedule periodicSchedule : periodicScheduleList) {
            if(timeDataConverter.checkBitsIsBelongToLocalDataTime(periodicSchedule.getWeekScheduleData()[dayOfWeek]
                    ,searchForm.getStartTime(), searchForm.getEndTime()))
                includedScheduleList.add(periodicSchedule);

        }

        List<Non_Periodic_Schedule> nonPeriodicScheduleList = nonPeriodicScheduleRepository.findByPublicTypeAndStartTimeBetween(true, searchForm.getStartTime(), searchForm.getEndTime());
        for (Non_Periodic_Schedule nonPeriodicSchedule : nonPeriodicScheduleList) {
            if(nonPeriodicSchedule.getEndTime().isBefore(searchForm.getEndTime().plusMinutes(1)))
                includedScheduleList.add(nonPeriodicSchedule);
        }

        return this.convertScheduleListToSearchedScheduleDtoList(includedScheduleList);
    }

    //그룹장에게 schedule 저장
    @Transactional
    private void saveScheduleInOwnerTimeTable(final TimeTableDto.SchedulePostDto schedulePostDto, final Long originScheduleId,final boolean isPublicTypeSchedule){
        Member member = memberService.getMember();
        Time_Table timeTable = member.getTimeTable();

        Schedule schedule = this.convertDtoToSchedule(schedulePostDto);
        schedule.setGroupType(true);
        schedule.setTimeTable(timeTable);
        schedule.setOriginScheduleId(originScheduleId);
        schedule.setPublicType(isPublicTypeSchedule);

        schedule.setTimeTable(timeTable);
        Schedule savedSchedule = scheduleRepository.save(schedule);//@JoinColumn을 가지고 있는게 주인이므로 set은 Schedule이

        List<Schedule> scheduleList = timeTable.getScheduleList();
        scheduleList.add(savedSchedule);
        timeTable.calcAllWeekScheduleData();

        log.info("add Schedule"+ savedSchedule.getId() + " to Member" + member.getId());
    }

    @Override
    @Transactional
    public void saveScheduleByTeam(final Long teamId, final Long OwnerId,final boolean isPublicTypeSchedule, final TimeTableDto.SchedulePostDto schedulePostDto){
        Team team = teamRepository.findById(teamId).get();
        Time_Table teamTimeTable = team.getTimeTable();

        List<Long> memberIdList = new ArrayList<>();
        List<MemberTeam> teamMembers = team.getTeamMembers();
        for (MemberTeam teamMember : teamMembers) {     //그룹원들  id 긁어오기
            if(!OwnerId.equals(teamMember.getMember().getId()))
                memberIdList.add(teamMember.getMember().getId());
        }

        schedulePostDto.setGroupId(teamId);
        schedulePostDto.setGroupName(team.getName());
        schedulePostDto.setGroupType(true);
        Schedule schedule = this.convertDtoToSchedule(schedulePostDto);
        schedule.setGroupType(true);
        schedule.setTimeTable(teamTimeTable);
        schedule.setPublicType(isPublicTypeSchedule);

        Schedule savedSchedule = scheduleRepository.save(schedule);//@JoinColumn을 가지고 있는게 주인이므로 set은 Schedule이
        savedSchedule.setOriginScheduleId(savedSchedule.getId());   //원본id 저장
        log.info("team savedSchedule: "+ savedSchedule.getGroupName());

        List<Schedule> scheduleList = teamTimeTable.getScheduleList();
        scheduleList.add(savedSchedule);
        teamTimeTable.calcAllWeekScheduleData();

        this.saveScheduleInOwnerTimeTable(schedulePostDto,savedSchedule.getId(),isPublicTypeSchedule);    //그룹장 자신에게 저장

        //그룹원들에게 알림보내기
        fcmService.sendMessageToMemberList(memberIdList,"그룹 " +team.getName()+"의 일정 추가 요청","scheduleId: " + savedSchedule.getId());

        log.info("add Schedule id: "+ savedSchedule.getId() + " to Team id: " + team.getId());
    }

    private List<TimeTableDto.TeamScheduleDto> convertSchedulesToTeamScheduleDtoList(final List<Schedule> scheduleList){
        List<TimeTableDto.TeamScheduleDto> ret = new ArrayList<>();

        for (Schedule schedule: scheduleList) {
            boolean periodicType = true;
            if(schedule instanceof Non_Periodic_Schedule)
                periodicType = false;

            ret.add(TimeTableDto.TeamScheduleDto.builder()
                    .id(schedule.getId())
                    .body(schedule.getBody())
                    .periodicType(periodicType)
                    .name(schedule.getName())
                    .timeData(this.timeDataConverter.convertScheduleTimeDataToString(schedule))
                    .build());
        }

        return ret;
    }

    @Override
    public List<TimeTableDto.TeamScheduleDto> getTeamScheduleList(final Long teamId){
        Team team = teamRepository.findById(teamId).get();

        Time_Table timeTable = team.getTimeTable();
        List<Schedule> scheduleList = timeTable.getScheduleList();
        log.info("size: "+scheduleList.size());
        List<TimeTableDto.TeamScheduleDto> teamScheduleDtoList = this.convertSchedulesToTeamScheduleDtoList(scheduleList);

        return teamScheduleDtoList;
    }

    @Override
    public TimeTableDto.MemberAllTimeTableInfo getMemberAllTimeTableInfo(Long memberId, LocalDate startDate, LocalDate endDate){
        TimeTableDto.TimeTableInfo memberTimeTable = this.getMemberTimeTableByDayNum(memberId, startDate, endDate);
        List<TimeTableDto.GroupTimeTableInfo> groupOfMemberTimeTableList = this.getGroupOfMemberTimeTableList(memberId, startDate, endDate);

        return TimeTableDto.MemberAllTimeTableInfo.builder()
                .memberTimeTable(memberTimeTable)
                .groupTimeTableList(groupOfMemberTimeTableList)
                .build();
    }

    private List<TimeTableDto.GroupTimeTableInfo> getGroupOfMemberTimeTableList(Long memberId,LocalDate startDate, LocalDate endDate){
        List<TimeTableDto.GroupTimeTableInfo> groupTimeTableInfoList = new ArrayList<>();

        Member member = memberRepository.findById(memberId).get();

        List<Team> groupList = new ArrayList<>();
        for (MemberTeam team : member.getMemberTeam()) {
            groupList.add(team.getTeam());
        }

        for (Team team : groupList) {
            Time_Table teamTimeTable = team.getTimeTable();
            TimeTableDto.TimeTableInfo timeTableInfo = this.calcTimeTableDataPerWeek(startDate, endDate, teamTimeTable.getWeekScheduleData(), teamTimeTable.getScheduleList());
            groupTimeTableInfoList.add(TimeTableDto.GroupTimeTableInfo.builder()
                    .groupId(team.getId())
                    .timeTableInfo(timeTableInfo)
                    .build());
        }

        return groupTimeTableInfoList;
    }

    @Override
    public TimeTableDto.TimeTableInfo getMemberTimeTableByDayNum(Long memberId, LocalDate startDate, LocalDate endDate){
        Member member = memberRepository.findById(memberId).get();

        Time_Table timeTable = member.getTimeTable();
        timeTable.calcAllWeekScheduleData();
        long[] weekScheduleData = timeTable.getWeekScheduleData();
        final List<Schedule> scheduleList = timeTable.getScheduleList();

        return calcTimeTableDataPerWeek(startDate,endDate,weekScheduleData,scheduleList);
    }

    private List<List<Boolean>> convertLongListToBitListsPerDay(List<Long> bitDataPerDays){
        List<List<Boolean>> bitListsPerDay = new ArrayList<>();

        for (Long bits: bitDataPerDays) {
            List<Boolean> bitList = timeDataConverter.convertLongToBooleanList(bits);
            bitListsPerDay.add(bitList);
        }

        return bitListsPerDay;
    }

    private TimeTableDto.TimeTableInfo calcTimeTableDataPerWeek( final LocalDate startDate, final LocalDate endDate ,final long[] weekScheduleData, final List<Schedule> scheduleList){
        final int dayNum = (int) Duration.between(startDate.atStartOfDay(),endDate.atStartOfDay()).toDays() +1;
        final int startDayOfWeek = startDate.getDayOfWeek().getValue() -1; //월요일부터 0~6까지 정수

        List<Long> bitDataPerDays = new ArrayList<>();
        List<List<TimeTableDto.ScheduleDto>> scheduleListPerDays  = new ArrayList<List<TimeTableDto.ScheduleDto>>();

        int dayOfWeekIdx = startDayOfWeek;
        final int WEEK_MOD = 7;
        for (int i = 0; i < dayNum; i++) {  //주기 데이터 저장
            bitDataPerDays.add(weekScheduleData[dayOfWeekIdx]);
            ++dayOfWeekIdx;
            dayOfWeekIdx %= WEEK_MOD;
            scheduleListPerDays.add(new ArrayList<>());
        }

        List<Periodic_Schedule> periodicScheduleList = new ArrayList<>();
        List<Non_Periodic_Schedule> nonPeriodicScheduleList = new ArrayList<>();
        for (Schedule schedule: scheduleList) { //타입 분리
            if(schedule instanceof Periodic_Schedule){
                periodicScheduleList.add((Periodic_Schedule)schedule);
            }
            else {
                Non_Periodic_Schedule nonPeriodicSchedule = (Non_Periodic_Schedule) schedule;
                if((nonPeriodicSchedule.getStartTime().isAfter(startDate.atStartOfDay())
                        && nonPeriodicSchedule.getEndTime().isBefore(endDate.plusDays(1).atStartOfDay())) )  //날짜 범위 내의 데이터만 저장
                    nonPeriodicScheduleList.add((Non_Periodic_Schedule)schedule);
            }

        }

        List<List<TimeTableDto.ScheduleDto>> weekRoutines = new ArrayList<>();
        for (int i = 0; i <WEEK_MOD ; i++)    //init
            weekRoutines.add(new ArrayList<>());

        for (Periodic_Schedule schedule:periodicScheduleList) { //weekRoutine인 스케줄 저장
            long[] weekBits = schedule.getWeekScheduleData();
            for(int day =0; day< WEEK_MOD ; ++day)
                if(weekBits[day] >0) {  //Dto Convert
                    log.info("return: schedule.getGroupId: "+schedule.getGroupId());
                    weekRoutines.get(day).add(
                            TimeTableDto.ScheduleDto.builder()
                                    .id(schedule.getId())
                                    .name(schedule.getName())
                                    .body(schedule.getBody())
                                    .groupType(schedule.isGroupType())
                                    .groupId(schedule.getGroupId())
                                    .groupName(schedule.getGroupName())
                                    .timeData(this.timeDataConverter.convertLongToBooleanList(weekBits[day]))
                                    .timeStringData(timeDataConverter.bitTimeDataToStringData(weekBits[day]))
                                    .build());
                }
        }

        dayOfWeekIdx = startDayOfWeek;
        for (int day = 0; day <dayNum ; day++) {
            scheduleListPerDays.get(day).addAll(weekRoutines.get(dayOfWeekIdx));
            ++dayOfWeekIdx;
            dayOfWeekIdx %= WEEK_MOD;
        }

        for (Non_Periodic_Schedule schedule: nonPeriodicScheduleList ) {
            LocalDateTime scheduleStartTime = schedule.getStartTime();
            Long timeBitData = timeDataConverter.convertTimeToBit(scheduleStartTime, schedule.getEndTime());
            LocalDate startLocalDate = LocalDate.of(scheduleStartTime.getYear(), scheduleStartTime.getMonth(), scheduleStartTime.getDayOfMonth());
            int afterDayNumFromStart = (int) Duration.between(startDate.atStartOfDay(),startLocalDate.atStartOfDay()).toDays();

            scheduleListPerDays.get(afterDayNumFromStart).add(
                    TimeTableDto.ScheduleDto.builder()
                            .id(schedule.getId())
                            .name(schedule.getName())
                            .body(schedule.getBody())
                            .groupType(schedule.isGroupType())
                            .groupId(schedule.getGroupId())
                            .groupName(schedule.getGroupName())
                            .timeData(this.timeDataConverter.convertLongToBooleanList(timeBitData))
                            .timeStringData(timeDataConverter.bitTimeDataToStringData(timeBitData))
                            .build());

            Long targetBits = bitDataPerDays.get(afterDayNumFromStart);
            bitDataPerDays.set(afterDayNumFromStart,targetBits|timeBitData);
        }


        return TimeTableDto.TimeTableInfo.builder()
                .startDate(startDate)
                .endData(endDate)
                .bitListsPerDay(this.convertLongListToBitListsPerDay(bitDataPerDays))
                .scheduleListPerDays(scheduleListPerDays).build();
    }

    private long[] convertTimeStringsToBitsArray(List<String> periodicTimeStringList){
        long[] bitsArray = {0,0,0,0,0,0,0};
        for (String time: periodicTimeStringList) {
            String[] splitData = time.split(",");
            Integer dayNumber = dayToIntMap.get(splitData[0]);
            String[] duration = splitData[1].split("-");

            int startIdx = (int) (this.timeDataConverter.timeStringToFloat(duration[0])*2);
            int endIdx = (int) (this.timeDataConverter.timeStringToFloat(duration[1])*2);

            long moveBit =(1l << startIdx);
            for (int i = startIdx; i <endIdx ; i++) {
                bitsArray[dayNumber] |= moveBit;
                moveBit <<=1;
            }
        }

        return bitsArray;
    }

    private Schedule convertDtoToSchedule(TimeTableDto.SchedulePostDto schedulePostData){
        if(schedulePostData.getPeriodicType().equals("true")){
            Periodic_Schedule periodicSchedule = new Periodic_Schedule();
            periodicSchedule.setWeekScheduleData(this.convertTimeStringsToBitsArray(schedulePostData.getPeriodicTimeStringList()));
            periodicSchedule.setName(schedulePostData.getName());
            periodicSchedule.setBody(schedulePostData.getBody());
            periodicSchedule.setGroupId(schedulePostData.getGroupId());
            periodicSchedule.setGroupType(schedulePostData.getGroupType());
            periodicSchedule.setGroupName(schedulePostData.getGroupName());
            log.info("build Periodic Schedule " + periodicSchedule.toString());
            return periodicSchedule;
        }
        Non_Periodic_Schedule nonPeriodicSchedule = new Non_Periodic_Schedule();
        nonPeriodicSchedule.setName(schedulePostData.getName());
        nonPeriodicSchedule.setStartTime(schedulePostData.getStartTime());
        nonPeriodicSchedule.setEndTime(schedulePostData.getEndTime());
        nonPeriodicSchedule.setBody(schedulePostData.getBody());
        nonPeriodicSchedule.setGroupId(schedulePostData.getGroupId());
        nonPeriodicSchedule.setGroupType(schedulePostData.getGroupType());
        nonPeriodicSchedule.setGroupName(schedulePostData.getGroupName());
        log.info("build NonPeriodic Schedule "+ nonPeriodicSchedule.toString());
        return  nonPeriodicSchedule;
    }

    @Override
    @Transactional
    public void saveScheduleInDB(final Long memberId, final Long scheduleId){
        Member member = memberRepository.findById(memberId).get();
        Schedule schedule = this.getScheduleById(scheduleId);

        Time_Table timeTable = member.getTimeTable();
        schedule.setTimeTable(timeTable);
        Schedule savedSchedule = scheduleRepository.save(schedule); //복사본으로서 저장
        //@JoinColumn을 가지고 있는게 주인이므로 set은 Schedule이

        List<Schedule> scheduleList = timeTable.getScheduleList();
        scheduleList.add(savedSchedule);
        timeTable.calcAllWeekScheduleData();

        log.info("add Schedule"+ savedSchedule.getId() + " to Member" + member.getId());
    }

    //멤버로 스케줄 저장
    @Override
    @Transactional
    public void saveScheduleInTimeTable(final Long memberId, final TimeTableDto.SchedulePostDto schedulePostData){
        Member member = memberRepository.findById(memberId).get();
        Schedule schedule = this.convertDtoToSchedule(schedulePostData);

        Time_Table timeTable = member.getTimeTable();
        schedule.setTimeTable(timeTable);
        Schedule savedSchedule = scheduleRepository.save(schedule);//@JoinColumn을 가지고 있는게 주인이므로 set은 Schedule이
        log.info("savedSchedule: "+ savedSchedule.getGroupId());
        List<Schedule> scheduleList = timeTable.getScheduleList();
        scheduleList.add(savedSchedule);
        timeTable.calcAllWeekScheduleData();

        log.info("add Schedule"+ savedSchedule.getId() + " to Member" + member.getId());
    }

    //멤버로 수정
    @Override
    @Transactional
    public void updateScheduleInTimeTable(final Long scheduleId, TimeTableDto.SchedulePostDto updatePostData){
        Schedule schedule = this.getScheduleById(scheduleId);

        if(schedule instanceof Periodic_Schedule)
            updatePostData.setPeriodicType("true");

        Schedule updateData = this.convertDtoToSchedule(updatePostData);

        if(schedule instanceof Periodic_Schedule)
            updatePeriodicSchedule((Periodic_Schedule)schedule, (Periodic_Schedule)updateData);
        else
            updateNonPeriodicSchedule((Non_Periodic_Schedule)schedule, (Non_Periodic_Schedule)updateData);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        savedSchedule.getTimeTable().calcAllWeekScheduleData();
    }

    @Transactional
    private void updatePeriodicSchedule(Periodic_Schedule schedule, Periodic_Schedule updateData){
        if(updateData.getWeekScheduleData() != null)
            schedule.setWeekScheduleData(updateData.getWeekScheduleData());
        if(updateData.getName() != null)
            schedule.setName(updateData.getName());
        //일단 확인용으로 두개만
        log.info("update Schedule "+ schedule.getId() + " as " + updateData.toString());
    }

    @Transactional
    private void updateNonPeriodicSchedule(Non_Periodic_Schedule schedule, Non_Periodic_Schedule updateData){
        if(updateData.getName() != null)
            schedule.setName(updateData.getName());
        if(updateData.getStartTime() != null)
            schedule.setStartTime(updateData.getStartTime());
        if(updateData.getEndTime() != null)
            schedule.setEndTime(updateData.getEndTime());
        //일단 확인용
        log.info("update Schedule "+ schedule.getId() + " as " + updateData.toString());
    }

    @Override
    @Transactional
    public void updateTeamSchedule(final Long teamId,final Long scheduleId, TimeTableDto.SchedulePostDto updatePostData){
        Team team = groupService.getTeamById(teamId);
        Schedule schedule = this.getScheduleById(scheduleId);
        Long originScheduleId = schedule.getId();
        List<Schedule> copyScheduleList = scheduleRepository.findByOriginScheduleId(originScheduleId);
        for (Schedule copySchedule : copyScheduleList) {
            this.updateScheduleInTimeTable(copySchedule.getId(),updatePostData);
        }

        this.updateScheduleInTimeTable(originScheduleId,updatePostData);
        log.info("update team Schedule "+ scheduleId);
    }

    @Override
    @Transactional
    public void deleteScheduleInTimeTable(final Long scheduleId){
        em.flush();
        em.clear();

        Schedule schedule = this.getScheduleById(scheduleId);

        Time_Table timeTable = schedule.getTimeTable();
        timeTable.getScheduleList().remove(schedule);   //서로의 연관관계 끊기
        timeTable.calcAllWeekScheduleData();

        schedule.setTimeTable(null);                    //서로의 연관관계 끊기

        scheduleRepository.deleteById(scheduleId);
        log.info("delete Schedule "+ scheduleId);
    }

    @Override
    @Transactional
    public void deleteTeamSchedule(final Long teamId,final Long scheduleId){
        Team team = groupService.getTeamById(teamId);
        Schedule schedule = this.getScheduleById(scheduleId);
        Long originScheduleId = schedule.getId();
        List<Schedule> copyScheduleList = scheduleRepository.findByOriginScheduleId(originScheduleId);
        for (Schedule copySchedule : copyScheduleList) {
            this.deleteScheduleInTimeTable(copySchedule.getId());
        }

        log.info("delete team Schedule "+ scheduleId);
    }

}
