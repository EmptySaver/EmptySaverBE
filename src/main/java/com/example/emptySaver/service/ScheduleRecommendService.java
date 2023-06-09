package com.example.emptySaver.service;

import com.example.emptySaver.domain.dto.FriendDto;
import com.example.emptySaver.domain.dto.TimeTableDto;
import com.example.emptySaver.domain.entity.*;
import com.example.emptySaver.repository.*;
import com.example.emptySaver.utils.TimeDataSuperUltraConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleRecommendService {
    private final MemberService memberService;
    private final TimeTableService timeTableService;
    private final GroupService groupService;
    private final CategoryService categoryService;
    private final FriendService friendService;

    private final TimeDataSuperUltraConverter timeDataConverter;
    private final PeriodicScheduleRepository periodicScheduleRepository;
    private final MemberRepository memberRepository;
    private final NonPeriodicScheduleRepository nonPeriodicScheduleRepository;
    private final MemberInterestRepository memberInterestRepository;

    private final static int WEEK_MOD = 7;

    //TODO: 지금은 모든 팀원이 빈 시간만 서칭 -> 팀원을 선정하면 그 팀원끼리 빈시간 서칭도 바로 가능, 근데 언제 몇명이 비었는지 모두 알려주기는 힘들듯
    public List<String> findEmptyTimeOfTeam(final Long teamId, final LocalDate startDate, final LocalDate endDate){
        Team team = groupService.getTeamById(teamId);

        List<Member> memberList = new ArrayList<>();    //팀 멤버들 가져옴
        for (MemberTeam teamMember :  team.getTeamMembers()) {
            memberList.add(teamMember.getMember());
        }

        return this.findEmptyTimeOfMemberList(memberList,startDate,endDate);
    }

    public List<String> findEmptyTimeWithFriends(final LocalDate startDate, final LocalDate endDate){
        List<Member> memberList = friendService.getFriendByMemberEntityList();
        memberList.add(memberService.getMember());    //자기 자신의 timetable도 제공하야 하므로 list에 넣음

        return this.findEmptyTimeOfMemberList(memberList,startDate,endDate);
    }

    public List<String> findEmptyTimeWithSelectedFriends(final List<Long> friendMemberIdList,final LocalDate startDate, final LocalDate endDate){
        List<Member> memberList = new ArrayList<>();
        for (Long friendMemberId : friendMemberIdList) {
            Optional<Member> byId = memberRepository.findById(friendMemberId);

            if(!byId.isEmpty())
                memberList.add(byId.get());
        }

        memberList.add(memberService.getMember());    //자기 자신의 timetable도 제공하야 하므로 list에 넣음

        return this.findEmptyTimeOfMemberList(memberList,startDate,endDate);
    }

    public List<FriendDto.FriendInfo> matchingFriends(final LocalDateTime startTime, final LocalDateTime endTime){
        List<FriendDto.FriendInfo> friendInfoList=new ArrayList<>();

        List<Friend> friendEntityList = friendService.getFriendEntityList();
        for (Friend friend : friendEntityList) {
            Time_Table timeTable = friend.getFriendMember().getTimeTable();

            if(timeTable.isTimeNotOverlapWithExistSchedule(startTime,endTime))    //친구가 빈 시간이 있는 지 확인
                friendInfoList.add(friendService.friendToFriendDto(friend));
        }

        return friendInfoList;
    }

    private List<String> findEmptyTimeOfMemberList(List<Member> memberList, final LocalDate startDate, final LocalDate endDate){
        List<Periodic_Schedule> membersPeriodicHaveTimeScheduleList = new ArrayList<>();
        List<Non_Periodic_Schedule> membersNonPeriodicScheduleList = new ArrayList<>();
        List<long[]> memberWeekDataList = new ArrayList<>();    //일단 주기 데이터 가져옴
        for (Member member : memberList) {
            Time_Table timeTable = member.getTimeTable();
            memberWeekDataList.add(timeTable.calcPeriodicScheduleInBound(true));
            membersPeriodicHaveTimeScheduleList.addAll(timeTable.getPeriodicScheduleOverlap(startDate.atStartOfDay(),endDate.plusDays(1).atStartOfDay(), true));
            membersNonPeriodicScheduleList.addAll(timeTable.getNonPeriodicScheduleInBound(startDate.atStartOfDay(),endDate.plusDays(1).atStartOfDay(), true));
        }

        long[] filledWeekTimeBit = this.calcFilledWeekTimeBit(memberWeekDataList);    //안겹치는 데이토
        final int dayNum = (int) Duration.between(startDate.atStartOfDay(),endDate.atStartOfDay()).toDays() +1;     //시작일로 부터 몇일간의 데이터인가

        int dayOfWeek = startDate.getDayOfWeek().getValue() -1;
        long[] filledDayBitArr = new long[dayNum];               //채워진 시간을 복사시켜둠
        for (int i = 0; i < filledDayBitArr.length; i++) {
            filledDayBitArr[i] = filledWeekTimeBit[dayOfWeek];
            dayOfWeek++;
            dayOfWeek %= WEEK_MOD;
        }

        long[] filledNonPeriodicBits = this.fillNonPeriodicMatchBits(startDate.atStartOfDay(), membersNonPeriodicScheduleList, filledDayBitArr);
        long[] filledFinalBits = this.fillBitOfPeriodicHaveTime(startDate.atStartOfDay(), dayNum,membersPeriodicHaveTimeScheduleList, filledNonPeriodicBits);
        List<String> emptyStringDataList = new ArrayList<>();

        for (int i = 0; i < filledFinalBits.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            LocalDate date = startDate.plusDays(i);
            stringBuilder.append(date.toString());
            stringBuilder.append(": ");
            stringBuilder.append(timeDataConverter.bitTimeDataToStringData(~filledFinalBits[i]));
            emptyStringDataList.add(stringBuilder.toString());
        }

        return emptyStringDataList;
    }

    private long[] fillBitOfPeriodicHaveTime(final LocalDateTime startTime,final int dayNum ,final List<Periodic_Schedule> periodicScheduleHaveTimeList,  long[] filledBits){
        int dayOfWeek;

        for (Periodic_Schedule periodicSchedule : periodicScheduleHaveTimeList) {
            int startDay = (int) Duration.between(startTime,periodicSchedule.getStartTime().toLocalDate().atStartOfDay()).toDays();
            dayOfWeek = periodicSchedule.getStartTime().toLocalDate().getDayOfWeek().getValue() -1;
            if(startDay <0){
                startDay = 0;
                dayOfWeek = startTime.toLocalDate().getDayOfWeek().getValue() -1;
            }

            int endDay = (int) Duration.between(startTime,periodicSchedule.getEndTime().toLocalDate().atStartOfDay()).toDays() +1;
            if(endDay> dayNum)
                endDay = dayNum;

            long[] scheduleBits = periodicSchedule.getWeekScheduleData();
            for (int i = 0; i < endDay; i++) {
                filledBits[i] |= scheduleBits[dayOfWeek];
                dayOfWeek++;
                dayOfWeek %= WEEK_MOD;
            }
        }

        return filledBits;
    }

    private long[] fillNonPeriodicMatchBits(final LocalDateTime startTime,final List<Non_Periodic_Schedule> nonPeriodicScheduleList, long[] filledDayBitArr){

        for (Non_Periodic_Schedule nonPeriodicSchedule : nonPeriodicScheduleList) {
            int dayNum = (int) Duration.between(startTime,nonPeriodicSchedule.getEndTime()).toDays();     //시작일로 부터 몇 idx 떨어져있는가

            Long nonPeriodicScheduleBit = timeDataConverter.convertTimeToBit(nonPeriodicSchedule.getStartTime(), nonPeriodicSchedule.getEndTime());
            filledDayBitArr[dayNum] |= nonPeriodicScheduleBit;
        }

        return filledDayBitArr;
    }


    private long[] calcFilledWeekTimeBit(final List<long[]> memberWeekDataList){
        long[] filledBitArr = new long[]{0,0,0,0,0,0,0};

        for (long[] weekData : memberWeekDataList) {
            for (int i = 0; i < weekData.length; i++) {
                filledBitArr[i] = filledBitArr[i] | weekData[i];
            }
        }

        return filledBitArr;
    }


    public List<TimeTableDto.SearchedScheduleDto> getRecommendScheduleList(final boolean interestFilterOn, final TimeTableDto.ScheduleSearchRequestForm requestForm){
        Member member = memberService.getMember();
        List<Schedule> recommendByMemberTimeTable = this.getRecommendByMemberTimeTable(member.getId(), requestForm.getStartTime(), requestForm.getEndTime());

        if(interestFilterOn)    //관심사로 filtering
            recommendByMemberTimeTable = this.filterMemberInterest(recommendByMemberTimeTable);

        return timeTableService.convertScheduleListToSearchedScheduleDtoList(recommendByMemberTimeTable);
    }

    private List<Schedule> filterMemberInterest(List<Schedule> scheduleList){

        Member member = memberService.getMember();
        List<MemberInterest> withCategoryByMember = memberInterestRepository.findWithCategoryByMember(member);
        List<String> interestList = new ArrayList<>();
        for (MemberInterest memberInterest : withCategoryByMember) {
            interestList.add(categoryService.getLabelByCategory(memberInterest.getCategory()));
        }

        if(withCategoryByMember.isEmpty())  //관심사가 없으면 그냥 그대로 반환
            return scheduleList;

        //관심사가 있으면 filtering 적용
        List<Schedule> filterScheduleList = scheduleList.stream()
                .filter(sch -> sch.getSubCategory() != null)
                .filter(sch -> interestList.contains(sch.getSubCategory()))
                .collect(Collectors.toList());

        return filterScheduleList;
    }

    private List<Periodic_Schedule> getPeriodicScheduleListNotOverlap(final long[] memberWeekData, List<Periodic_Schedule> periodicScheduleOverlapList ){
        long[] notWeekData = {0,0,0,0,0,0,0};
        for (int i = 0; i < notWeekData.length; i++) {
            notWeekData[i] = ~memberWeekData[i];
        }

        List<Periodic_Schedule> includedScheduleList = new ArrayList<>();

        List<Periodic_Schedule> periodicScheduleList = periodicScheduleRepository.findByPublicType(true);
        //log.info("public schedule size: " + periodicScheduleList.size());
        for (Periodic_Schedule periodicSchedule : periodicScheduleList) {
            if(!this.checkBitTimeDataIsOverlap(periodicSchedule.getWeekScheduleData(), memberWeekData))
               includedScheduleList.add(periodicSchedule);
        }
        //log.info("includedScheduleList size : "+ includedScheduleList.size());

        List<Periodic_Schedule> finalScheduleList = new ArrayList<>();

        for (Periodic_Schedule schedule : includedScheduleList) {   //member의 overlap 주기 스케줄과 한번더 filteri
            boolean isOkay = true;
            for (Periodic_Schedule overlapSchedule : periodicScheduleOverlapList) {
                if (schedule.getStartTime() !=null){ //시간 주기와 시간 주기의 filter
                    if(checkPeriodicScheduleTimeHaveOverlap(schedule,overlapSchedule)
                            && this.checkBitTimeDataIsOverlap(schedule.getWeekScheduleData(), overlapSchedule.getWeekScheduleData())){
                        isOkay = false;
                        break;
                    }
                }else{ //시간x 주기와 시간 주기의 filter
                    if(this.checkBitTimeDataIsOverlap(schedule.getWeekScheduleData(), overlapSchedule.getWeekScheduleData())){
                        isOkay = false;
                        break;
                    }
                }
            }
            if(isOkay)
                finalScheduleList.add(schedule);
        }
        //log.info("finalScheduleList size : "+finalScheduleList.size());

        return finalScheduleList;
    }

    private boolean checkPeriodicScheduleTimeHaveOverlap(final Periodic_Schedule a, final Periodic_Schedule b){
        if((a.getStartTime().isBefore(b.getEndTime()) && a.getEndTime().isAfter(b.getEndTime().minusMinutes(1)))
                ||(b.getStartTime().isBefore(a.getEndTime()) && a.getEndTime().isBefore(b.getEndTime().plusMinutes(1))))
            return true;
        return false;
    }

    private boolean checkBitTimeDataIsOverlap(final long[] a, final long[] b){
        //boolean isOverlap = false;
        for (int i = 0; i < a.length; i++) {
            //log.info("b:" + Long.toBinaryString(b[i]));
            //log.info("a:" + Long.toBinaryString(a[i]));
            if((b[i] & a[i]) != 0){
                return true;
            }
        }

        return false;
    }

    public List<Schedule> getRecommendByMemberTimeTable(final Long memberId, final LocalDateTime startTime, final LocalDateTime endTime){
        Member member = memberRepository.findById(memberId).get();
        Time_Table timeTable = member.getTimeTable();

        //timeTable.calcAllWeekScheduleData();
        final long[] weekScheduleData = timeTable.calcPeriodicScheduleInBound(true);


        List<Non_Periodic_Schedule> memberNonPeriodicScheduleList = timeTable.getNonPeriodicScheduleInBound(startTime,endTime, true); //new ArrayList<>();    // 멤버의 비주기 스케줄 가져옴
        List<Periodic_Schedule> memberPeriodicScheduleList = timeTable.getPeriodicScheduleInBound(true); // new ArrayList<>();    // 멤버의 주기 스케줄 가져옴
        List<Periodic_Schedule> periodicScheduleOverlap = timeTable.getPeriodicScheduleOverlap(startTime, endTime, true);
        //log.info("ovelap size = " + periodicScheduleOverlap.size());


        List<Periodic_Schedule> passedPeriodicScheduleList = getMatchedPeriodicScheduleList(weekScheduleData, memberNonPeriodicScheduleList, periodicScheduleOverlap);

        List<Non_Periodic_Schedule> passedNonPeriodicScheduleList = getMatchedNonPeriodicScheduleList(startTime, endTime, weekScheduleData, memberNonPeriodicScheduleList, periodicScheduleOverlap);

        List<Schedule> recommendScheduleList = new ArrayList<>();
        recommendScheduleList.addAll(passedPeriodicScheduleList);
        recommendScheduleList.addAll(passedNonPeriodicScheduleList);

        return recommendScheduleList;
    }

    private List<Non_Periodic_Schedule> getMatchedNonPeriodicScheduleList(LocalDateTime startTime, LocalDateTime endTime, long[] weekScheduleData
            , List<Non_Periodic_Schedule> memberNonPeriodicScheduleList, final List<Periodic_Schedule> periodicScheduleOverlapList) {
        List<Non_Periodic_Schedule> sortMemberNonPeriodicScheduleList = memberNonPeriodicScheduleList.stream()
                .sorted(Comparator.comparing(Non_Periodic_Schedule::getStartTime).thenComparing(Non_Periodic_Schedule::getEndTime)).collect(Collectors.toList());
        List<Non_Periodic_Schedule> nonPeriodicScheduleList = nonPeriodicScheduleRepository
                .findSortByPublicTypeAndStartTimeBetween(true, startTime, endTime,
                        Sort.by(Sort.Direction.ASC, "startTime","endTime"));

        boolean[] isPassedArr = new boolean[nonPeriodicScheduleList.size()];
        int memberPointer=0, dbPointer =0;  //member 스케줄이 항상 앞에 있게 만듬
        int idx = 0;
        while(memberPointer < sortMemberNonPeriodicScheduleList.size() && dbPointer<nonPeriodicScheduleList.size()){
            Non_Periodic_Schedule memberSchedule = sortMemberNonPeriodicScheduleList.get(memberPointer);
            Non_Periodic_Schedule dbSchedule = nonPeriodicScheduleList.get(dbPointer);
            if(memberSchedule.getEndTime().isBefore(dbSchedule.getStartTime().plusMinutes(1))){ //memberSchedule이 db보다 늦게 시작 -> 절대 안 겹칩
                memberPointer++;    //memberSchedule 땡겨줌
            }else if(memberSchedule.getStartTime().isAfter(dbSchedule.getEndTime().minusMinutes(1))){   // dbSchedule이 memberSchedule보다 늦게 시작 ->안 겹침
                dbPointer++; //db Schedule을 땡겨줌
            }else{  //db스케줄이 겹치는 상황 -> 다음db로 넘어가ㅣㅁ
                isPassedArr[dbPointer] = true;
                dbPointer++;
            }
        }

        List<Non_Periodic_Schedule> matchedNonPeriodicScheduleList = new ArrayList<>();
        for (int i = 0; i < isPassedArr.length; i++) {  //슬라이드 하고 남은거
            if(!isPassedArr[i])
                matchedNonPeriodicScheduleList.add(nonPeriodicScheduleList.get(i));
        }
       log.info("non Periodic size: "+ matchedNonPeriodicScheduleList.size());

        List<Non_Periodic_Schedule> passedNonPeriodicScheduleList = new ArrayList<>();
        for (int i = 0; i < matchedNonPeriodicScheduleList.size(); i++) {
            Non_Periodic_Schedule nonPeriodicSchedule = matchedNonPeriodicScheduleList.get(i);
            int dayOfWeek = nonPeriodicSchedule.getEndTime().getDayOfWeek().getValue() -1;
            long notBits = ~weekScheduleData[dayOfWeek];
            long ret = notBits | timeDataConverter.convertTimeToBit(nonPeriodicSchedule.getStartTime(), nonPeriodicSchedule.getEndTime());

            if(ret == notBits)  //같아야 빈칸에 들어감
                passedNonPeriodicScheduleList.add(nonPeriodicSchedule);
        }
        log.info("passedNonPeriodicScheduleList size: "+ passedNonPeriodicScheduleList.size());

        List<Non_Periodic_Schedule> finalNonPeriodicScheduleList = new ArrayList<>();
        for (Non_Periodic_Schedule nonPeriodicSchedule : passedNonPeriodicScheduleList) {   //시간 있는 주기랑도 안겹친게 최종 통과
            boolean isOkay =true;
            for (Periodic_Schedule periodicSchedule : periodicScheduleOverlapList) {
                if((periodicSchedule.getStartTime().isBefore(nonPeriodicSchedule.getStartTime().plusMinutes(1))
                        && periodicSchedule.getEndTime().isAfter(nonPeriodicSchedule.getEndTime().minusMinutes(1)))){
                    isOkay = false;
                    break;
                }
            }
            if(isOkay)
                finalNonPeriodicScheduleList.add(nonPeriodicSchedule);
        }
        log.info("finalNonPeriodicScheduleList size: "+ finalNonPeriodicScheduleList.size());
        return finalNonPeriodicScheduleList;
    }

    private List<Periodic_Schedule> getMatchedPeriodicScheduleList(final long[] weekScheduleData, List<Non_Periodic_Schedule> memberNonPeriodicScheduleList, List<Periodic_Schedule> periodicScheduleOverlapList) {
        List<Periodic_Schedule> passedPeriodicScheduleList = new ArrayList<>();
        List<Periodic_Schedule> periodicScheduleListNotOverlap = this.getPeriodicScheduleListNotOverlap(weekScheduleData, periodicScheduleOverlapList);  // db의 주기적 스케줄 가져옴
        //log.info("overlap calc: " + periodicScheduleListNotOverlap.size());

        //log.info("memberNonPeriodicSchedule size : " + memberNonPeriodicScheduleList.size());
        boolean[] isPassedArr = new boolean[periodicScheduleListNotOverlap.size()];
        for (Non_Periodic_Schedule memberNonPeriodicSchedule : memberNonPeriodicScheduleList) {
            int dayOfWeek = memberNonPeriodicSchedule.getEndTime().getDayOfWeek().getValue() -1;

            for (int i=0; i<periodicScheduleListNotOverlap.size() ; ++i) {
                if(isPassedArr[i])
                    continue;

                Periodic_Schedule periodicSchedule = periodicScheduleListNotOverlap.get(i);
                if(periodicSchedule.getStartTime() != null){    //시간이 있는 주기 스케줄과 비주기가 겹치지 않는 경우
                    if(periodicSchedule.getStartTime().isBefore(memberNonPeriodicSchedule.getStartTime().plusMinutes(1))
                            && periodicSchedule.getEndTime().isAfter(memberNonPeriodicSchedule.getEndTime().minusMinutes(1))) {   //겹치는 경우 상세 시간도 겹치는지
                        Long nonPeriodicBitData = timeDataConverter.convertTimeToBit(memberNonPeriodicSchedule.getStartTime(), memberNonPeriodicSchedule.getEndTime());
                        long periodicBitData = periodicSchedule.getWeekScheduleData()[dayOfWeek];
                        if((periodicBitData & nonPeriodicBitData) != 0)
                            isPassedArr[i] = true;
                    }
                    continue;
                }

                long periodicBitData = periodicSchedule.getWeekScheduleData()[dayOfWeek];

                if(periodicBitData ==0)
                    continue;

                Long nonPeriodicBitData = timeDataConverter.convertTimeToBit(memberNonPeriodicSchedule.getStartTime(), memberNonPeriodicSchedule.getEndTime());

                if((periodicBitData & nonPeriodicBitData) != 0)
                    isPassedArr[i] = true;
            }


        }
        //주기적은 모두 비교가 끝난것만 담음
        for (int i=0; i<periodicScheduleListNotOverlap.size() ; ++i) {
            if(!isPassedArr[i])
                passedPeriodicScheduleList.add(periodicScheduleListNotOverlap.get(i));
        }

        return passedPeriodicScheduleList;
    }
}
