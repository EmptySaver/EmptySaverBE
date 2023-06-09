package com.example.emptySaver.controller;

import com.example.emptySaver.domain.dto.FriendDto;
import com.example.emptySaver.domain.dto.TimeTableDto;
import com.example.emptySaver.domain.entity.Team;
import com.example.emptySaver.repository.TeamRepository;
import com.example.emptySaver.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/timetable")
@RequiredArgsConstructor
@Slf4j
public class TimeTableController {
    private final TimeTableService timeTableService;
    private final MemberService memberService;
    private final ScheduleRecommendService scheduleRecommendService;
    private final ScheduleService scheduleService;
    private final GroupService groupService;

    private final TeamRepository teamRepository;


    @PostMapping("/team/findEmptyTime")
    @Operation(summary = "팀원들의 빈시간 정보 받기", description = "그룹의 멤버들의 스케줄를 분석해서, 모든 멤버가 겹치지 않는 시간을 문자열 정보로 넘김<br>" +
            "startTime과 endTime은 년,월,일 까지만 확실히 적고, 시간은 00:00:00처럼 채워만 두세요.(가능은 한 시간으로)")
    @Parameter(
            name ="groupId",
            description = "groupId"
    )
    public ResponseEntity<List<String>> getEmptyTimeOfTeam(final @RequestParam Long groupId, @RequestBody final TimeTableDto.ScheduleSearchRequestForm searchForm){
        TimeTableDto.checkLocalDateValid(searchForm.getStartTime(),searchForm.getEndTime());

        List<String> emptyDataList = scheduleRecommendService.findEmptyTimeOfTeam(groupId,
                searchForm.getStartTime().toLocalDate(), searchForm.getEndTime().toLocalDate());

        return new ResponseEntity<>(emptyDataList, HttpStatus.OK);
    }

    @PostMapping("/recommendSchedule")
    @Operation(summary = "시간표 정보로 스케줄 추천받기", description = "멤버의 시간표를 분석해서, 범위 내에서 겹치지 않는 스케줄을 찾아줌")
    @Parameter(
            name = "interestFilterOn",
            description = "true로 설정시 멤버의 관심사로 필터링된 스케줄을 받음 <br>" +
                    "true로 설정해도 관심사가 없는 멤버, 그리고 false로 설정한 경우 모든 스케줄을 추천 받음"
    )
    public ResponseEntity<List<TimeTableDto.SearchedScheduleDto>> getRecommendScheduleToMember(@RequestParam final boolean interestFilterOn,@RequestBody TimeTableDto.ScheduleSearchRequestForm requestForm){
        TimeTableDto.checkLocalDateValid(requestForm.getStartTime(),requestForm.getEndTime());
        List<TimeTableDto.SearchedScheduleDto> recommendScheduleList = scheduleRecommendService.getRecommendScheduleList(interestFilterOn, requestForm);
        return new ResponseEntity<>(recommendScheduleList, HttpStatus.OK);
    }

    @PostMapping("/team/saveGroupSchedule")
    @Operation(summary = "그룹의 스케줄을 멤버가 저장", description = "그룹의 스케줄이 저장되면, 수락 거절을 진행-> 수락시 scheduleId로 저장 요청")
    @Parameter(
            name ="scheduleId",
            description = "group schedule 저장시 멤버에게 알림으로 날라간 scheduleId를 담아 요청"
    )
    @Parameter(
            name = "hideType",
            description = "저장할때 숨기고 싶은지 여부를 같이 보내주세요~. 숨기고 싶다면 true입니다."
    )
    public ResponseEntity<String> saveGroupSchedule(final @RequestParam Long scheduleId, final @RequestParam boolean hideType){
        Long currentMemberId = memberService.getCurrentMemberId();
        timeTableService.saveScheduleInDB(currentMemberId, scheduleId, hideType);
        return new ResponseEntity<>("Group Schedule saved for member as hide: "+ hideType, HttpStatus.OK);
    }

    @PostMapping("/findSchedule")
    @Operation(summary = "시간내의 스케줄 정보 찾기", description = "정한 search 시간 활용해서 공개 스케줄을 검색해옴")
    public ResponseEntity<List<TimeTableDto.SearchedScheduleDto>> searchSchedule(@RequestBody TimeTableDto.ScheduleSearchRequestForm requestForm){
        TimeTableDto.checkLocalDateValid(requestForm.getStartTime(),requestForm.getEndTime());
        List<TimeTableDto.SearchedScheduleDto> searchedScheduleDtoList = timeTableService.getSearchedScheduleDtoList(requestForm);

        return new ResponseEntity<>(searchedScheduleDtoList, HttpStatus.OK);
    }

    @PostMapping("/getMemberAndGroupTimeTable")
    @Operation(summary = "자신과 자신의 그룹의 TimeTable 정보 가져오기", description = "로그인 한 유저, 자신과 자신의 그룹의 timeTable의 정보를 가져옴")
    @ApiResponse(responseCode = "200", description = "list의 첫번째 원소가 ")
    public ResponseEntity<TimeTableDto.MemberAllTimeTableInfo> getMemberAndGroupTimeTable(@RequestBody TimeTableDto.TimeTableRequestForm requestForm){
        TimeTableDto.checkTimeTableRequestFormValid(requestForm);

        Long currentMemberId = memberService.getCurrentMemberId();
        TimeTableDto.MemberAllTimeTableInfo memberAllTimeTableInfo = timeTableService.getMemberAllTimeTableInfo(currentMemberId, requestForm.getStartDate(), requestForm.getEndDate());
        return new ResponseEntity<>(memberAllTimeTableInfo, HttpStatus.OK);
    }

    @PostMapping("/getTimeTable")
    @Operation(summary = "TimeTable 정보 가져오기", description = "로그인 한 유저, 자신의 timeTable의 정보를 가져옴")
    public ResponseEntity<TimeTableDto.TimeTableInfo> getMemberTimeTable(@RequestBody TimeTableDto.TimeTableRequestForm requestForm){
        TimeTableDto.checkTimeTableRequestFormValid(requestForm);

        Long currentMemberId = memberService.getCurrentMemberId();
        //log.info("build: " + requestForm.toString());
        TimeTableDto.TimeTableInfo timeTableInfo
                = timeTableService.getMemberTimeTableByDayNum(currentMemberId, requestForm.getStartDate(), requestForm.getEndDate(), true);
        return new ResponseEntity<>(timeTableInfo, HttpStatus.OK);
    }

    @PostMapping("/saveSchedule")
    @Operation(summary = "timetable에 스케줄 저장", description = "로그인 한 유저가 스케줄 정보를 timetable에 추가<br>" +
            "가장 중요한 부분은 periodicType설정 임다. <br>" +
            " \"true\"로 하면 주기적 데이터로 인식하여 periodicTimeStringList를 반드식 넣어줘여한다. <br>" +
            "periodicTimeStringList = [\"화,00:30-01:30\",\"화,18:00-19:00\",\"금,19:00-24:00\"] 같이, [요일,시작시간-끝나는시간]으로 표기한다. <br>" +
            "주기 데이터라도 startTime과 endTime이 있다면 보내주면 됩니다. 없으면 안보내도 됩니다.  <br>" +
            "startTime과 endTime은 'yyyy-MM-dd'T'HH:mm:ss'형식의 String으로 보내면 인식됩니다. <br>"+
            "새로 추가된 ")
    public ResponseEntity<String> addMemberSchedule(@RequestBody TimeTableDto.SchedulePostDto schedulePostData){
        TimeTableDto.checkScheduleFormValid(schedulePostData);
        //TimeTableDto.checkSchedulePostDtoValid(schedulePostData);

        Long currentMemberId = memberService.getCurrentMemberId();
        log.info("build: " + schedulePostData.toString());
        timeTableService.saveScheduleInTimeTable(currentMemberId, schedulePostData);
        return new ResponseEntity<>("Schedule saved for member", HttpStatus.OK);
    }

    @PutMapping("/updateSchedule")
    @Operation(summary = "특정 스케줄 변경", description = "특정 스케줄을 scheduleId를 이용해서 변경. 단 주기적<->비주기적 변경 불가")
    @Parameter(
            name = "scheduleId",
            description = "body가 아닌 uri에 담아서 보내기.<br>" +
                    "반드시 db에 존재하는 scheduleId 이어야 하므로, " +
                    "getMemberTimeTable로 얻은 정보에서 scheduleId 추출해 사용."
    )
    public ResponseEntity<String> updateSchedule(final @RequestParam Long scheduleId, final @RequestBody TimeTableDto.SchedulePostDto updateData){
        TimeTableDto.checkScheduleFormValid(updateData);

        Long currentMemberId = memberService.getCurrentMemberId();
        timeTableService.updateScheduleInTimeTable(scheduleId, currentMemberId, updateData);
        return new ResponseEntity<>("Schedule update, id: " +scheduleId, HttpStatus.OK);
    }

    @DeleteMapping("/deleteSchedule")
    @Operation(summary = "특정 스케줄 삭제", description = "특정 스케줄을 id로 지움, 따라서 id는 절대 변경 안되도록")
    @Parameter(
            name = "scheduleId",
            description = "body가 아닌 uri에 담아서 보내기. <br>" +
                    "반드시 db에 존재하는 scheduleId 이어야 하므로, " +
                    "getMemberTimeTable로 얻은 정보에서 scheduleId 추출해 사용."
    )
    public ResponseEntity<String> deleteSchedule(final @RequestParam Long scheduleId){
        timeTableService.deleteScheduleInTimeTable(scheduleId);
        return new ResponseEntity<>("Schedule deleted, id: " + scheduleId, HttpStatus.OK);
    }

    @PostMapping("/team/saveSchedule")
    @Operation(summary = "Team의 스케줄 저장", description = "그룹장으로 유저인 스케줄 정보를 추가<br>"+
            "팀의 스케줄 저장은 멤버의 스케줄 저장과 단 하나가 다름니다.<br>" +
            "또한 중요한 부분은 periodicType설정 임다. <br>" +
            " \"true\"로 하면 주기적 데이터로 인식하여 periodicTimeStringList를 반드식 넣어줘여한다. <br>" +
            "periodicTimeStringList = [\"화,00:30-01:30\",\"화,18:00-19:00\",\"금,19:00-24:00\"] 같이, [요일,시작시간-끝나는시간]으로 표기한다.  <br>" +
            "주기 데이터라도 startTime과 endTime이 있다면 보내주면 됩니다. 없으면 안보내도 됩니다. <br>" +
            "startTime과 endTime은 'yyyy-MM-dd'T'HH:mm:ss'형식의 String으로 보내면 인식됩니다.")
    @Parameter(
            name = "groupId",
            description = "uri에 스캐줄을 추가할 그룹의 ID를 담는다."
    )
    @Parameter(
            name = "isPublicTypeSchedule",
            description = "이 파라미터가 바로 멤버의 스케줄 저장과 다른 부분입니다.<br>" +
                    "이 파라미터를 true(문자열도 괜찮)로 넘기면 공개 스케줄로서 저장됩니다.<br>" +
                    "공개 스케줄을 스케줄 검색을 통해 모든 유저에게 보일 수 있습니다."
    )
    public ResponseEntity<String> addTeamSchedule(final @RequestParam Long groupId,final @RequestParam boolean isPublicTypeSchedule, final @RequestBody TimeTableDto.SchedulePostDto schedulePostData){
        TimeTableDto.checkScheduleFormValid(schedulePostData);
        log.info("groupId:"+groupId);
        Long currentMemberId = memberService.getCurrentMemberId();
        //Team team = teamRepository.findById(groupId).get();

        if(!groupService.checkOwner(groupId)){   //group owner만 가능하도록
            log.info("request member is not Group owner");
            return new ResponseEntity<>("request member is not Group owner", HttpStatus.BAD_REQUEST);
        }

        //log.info("build: " + schedulePostData.toString());
        timeTableService.saveScheduleByTeam(groupId, currentMemberId,isPublicTypeSchedule,schedulePostData);
        return new ResponseEntity<>("Schedule saved for group", HttpStatus.OK);
    }

    @PutMapping("/team/updateSchedule")
    @Operation(summary = "Team의 스케줄 업데이트", description = "그룹의 id, 스케줄 id, 업데이트 내용으로 요청<br>모든 팀원들에게 반영됨. 단 주기적<->비주기적 변경 불가")
    @Parameter(
            name = "scheduleId",
            description = "각각의 팀원들에게는 내용만 복사된 독립된 스케줄로 저장되기 때문에, 반드시 team의 timetable에서 가져온 id이어야한다."
    )
    public ResponseEntity<String> updateTeamSchedule(final @RequestParam Long groupId,final @RequestParam Long scheduleId,@RequestBody TimeTableDto.SchedulePostDto updatePostData){
        TimeTableDto.checkScheduleFormValid(updatePostData);

        Long currentMemberId = memberService.getCurrentMemberId();
        Team team = teamRepository.findById(groupId).get();

        if(!currentMemberId.equals(team.getOwner().getId())){   //group owner만 가능하도록
            log.info("request member is not Group owner");
            return new ResponseEntity<>("request member is not Group owner", HttpStatus.BAD_REQUEST);
        }

        timeTableService.updateTeamSchedule(groupId,currentMemberId,scheduleId,updatePostData);
        return new ResponseEntity<>("team schedule updated", HttpStatus.OK);
    }

    @DeleteMapping("/team/deleteSchedule")
    @Operation(summary = "group의 스케줄 삭제", description = "그룹의 id, 스케줄 id로 삭제요청<br>모든 팀원들의 timetable에서 삭제됨")
    @Parameter(
            name = "groupId",
            description = "변경할 group의 id"
    )
    @Parameter(
            name = "scheduleId",
            description = "각각의 팀원들에게는 내용만 복사된 독립된 스케줄로 저장되기 때문에, 반드시 team의 timetable에서 가져온 id이어야한다."
    )
    public ResponseEntity<String> deleteTeamSchedule(final @RequestParam Long groupId,final @RequestParam Long scheduleId){
        Long currentMemberId = memberService.getCurrentMemberId();
        Team team = teamRepository.findById(groupId).get();

        if(!currentMemberId.equals(team.getOwner().getId())){   //group owner만 가능하도록
            log.info("request member is not Group owner");
            return new ResponseEntity<>("request member is not Group owner", HttpStatus.BAD_REQUEST);
        }

        timeTableService.deleteTeamSchedule(groupId,scheduleId);
        return new ResponseEntity<>("team schedule deleted", HttpStatus.OK);
    }

    @GetMapping("/team/getScheduleList")
    @Operation(summary = "Team의 스케줄들을 받아오기", description = "그룹의 id로 스케줄 정보를 받아옴")
    @Parameter(
            name = "groupId",
            description = "uri에 스캐줄을 검색할 그룹의 ID를 담는다."
    )
    public ResponseEntity<List<TimeTableDto.TeamScheduleDto>> getTeamScheduleList(final @RequestParam Long groupId){
        List<TimeTableDto.TeamScheduleDto> teamScheduleList = timeTableService.getTeamScheduleList(groupId);
        return new ResponseEntity<>(teamScheduleList, HttpStatus.OK);
    }

    @PostMapping("/team/readSchedule")
    @Operation(summary = "Team의 스케줄 읽음 표시", description = "읽음 표시한 스케줄은 멤버의 스케줄에 추가됩니다.")
    @Parameter(
            name = "scheduleId",
            description = "읽음 표시할 스케줄의 Id"
    )
    @Parameter(
            name = "accept",
            description = "수락/ 거절 여부를 boolean으로"
    )
    @Parameter(
            name = "hideType",
            description = "저장할때 숨기고 싶은지 여부를 같이 보내주세요~. 숨기고 싶다면 true입니다."
    )
    public ResponseEntity<String> setCheckTeamSchedule(final @RequestParam Long scheduleId, final @RequestParam boolean accept
            ,final @RequestParam boolean hideType){
        timeTableService.setCheckTeamSchedule(scheduleId, accept, hideType);
        return new ResponseEntity<>("set read schedule", HttpStatus.OK);
    }

    @GetMapping("/getMovieScheduleList")
    @Operation(summary = "오늘 상영 영화의 스케줄들을 받아오기", description = "일단 오늘 것만 가능하게 구현했습니다.")
    public ResponseEntity<List<TimeTableDto.TeamScheduleDto>> getMovieScheduleList(){
        List<TimeTableDto.TeamScheduleDto> movieScheduleToDay = scheduleService.getMovieScheduleToDay();
        return new ResponseEntity<>(movieScheduleToDay, HttpStatus.OK);
    }

    @PostMapping("saveScheduleByCopy")
    @Operation(summary = "스케줄 id로 개인 스케줄에 복사시켜 저장하기", description = "영화 스케줄 저장, 팀 스케줄 저장등 스케줄 id값만 있으면,<br>" +
            "데이터를 복사해서 개인 스케줄에 저장시킨다.")
    @Parameter(
            name ="scheduleId",
            description = "schedule 저장 시 저장시키고 싶은 원본 scheduleId를 담아 요청"
    )
    @Parameter(
            name = "hideType",
            description = "저장할때 숨기고 싶은지 여부를 같이 보내주세요~. 숨기고 싶다면 true입니다."
    )
    public ResponseEntity<String> saveScheduleByCopy(final @RequestParam Long scheduleId, final @RequestParam boolean hideType){
        Long currentMemberId = memberService.getCurrentMemberId();
        timeTableService.saveScheduleInDB(currentMemberId, scheduleId, hideType);
        return new ResponseEntity<>("Schedule copy saved for member as hide: "+ hideType, HttpStatus.OK);
    }

    @PostMapping("findFriendsHaveTime")
    @Operation(summary = "해당 시간이 빈 친구 목록 받기", description = "자신의 친구 중에서 설정한 시간이 빈 친구들의 정보를 받아옵니다.")
    public ResponseEntity<List<FriendDto.FriendInfo>> getFriendsHaveTime(final @RequestBody TimeTableDto.ScheduleSearchRequestForm searchForm){
        TimeTableDto.checkLocalDateValid(searchForm.getStartTime(), searchForm.getEndTime());
        List<FriendDto.FriendInfo> friendInfos = scheduleRecommendService.matchingFriends(searchForm.getStartTime(), searchForm.getEndTime());
        return new ResponseEntity<>(friendInfos, HttpStatus.OK);
   }

    @PostMapping("findEmptyTimeWithAllFriends")
    @Operation(summary = "자신의 모든 친구와 빈 시간 찾기", description = "설정한 시간 내에서 자신의 모든 친구와 빈 시간 찾아서 string으로 반환해줍니다.")
    public ResponseEntity<List<String>> getEmptyTimeWithAllFriends(final @RequestBody TimeTableDto.TimeTableRequestForm searchForm ){
        TimeTableDto.checkTimeTableRequestFormValid(searchForm);
        List<String> emptyTimeWithFriends = scheduleRecommendService.findEmptyTimeWithFriends(searchForm.getStartDate(), searchForm.getEndDate());
        return new ResponseEntity<>(emptyTimeWithFriends, HttpStatus.OK);
   }

    @PostMapping("findEmptyTimeWithSelectedFriends")
    @Operation(summary = "자신이 선택한 친구와 빈 시간 찾기", description = "설정한 시간 내에서 자신이 선택한 친구와 빈 시간 찾아서 string으로 반환해줍니다.<br>" +
            "이때 선택할 FriendMemberIdList에는 친구들의 FriendMemberId를 담아서 보내야 합니다.")
    public ResponseEntity<List<String>> getEmptyTimeWithSelectedFriends(final @RequestBody FriendDto.FriendMemberIdList friendMemberIdList
            ,final @RequestBody TimeTableDto.TimeTableRequestForm searchForm ){
        TimeTableDto.checkTimeTableRequestFormValid(searchForm);
        List<String> emptyTimeWithSelectedFriends = scheduleRecommendService.findEmptyTimeWithSelectedFriends(friendMemberIdList.getFriendMemberIdLst()
                , searchForm.getStartDate(), searchForm.getEndDate());
        return new ResponseEntity<>(emptyTimeWithSelectedFriends, HttpStatus.OK);
   }

}
