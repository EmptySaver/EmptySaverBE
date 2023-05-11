package com.example.emptySaver.service;

import com.example.emptySaver.domain.dto.CrawlDto;
import com.example.emptySaver.domain.entity.NonSubject;
import com.example.emptySaver.domain.entity.Recruiting;
import com.example.emptySaver.repository.NonSubjectRepository;
import com.example.emptySaver.repository.RecruitingRepository;
//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlService {
    private final RecruitingRepository recruitingRepository;
    private final NonSubjectRepository nonSubjectRepository;
    String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
    @Value("${portal.id}")
    String id;
    @Value("${portal.password}")
    String password;
    Map<String,String> formData=new HashMap<>();
    Map<String,String> sameHeader=new HashMap<>();
    Connection.Response first;


    @PostConstruct
    public void CrawlService() throws IOException {
        //깃헙올라갈때 빌드 오류 방지
        if(id.equals("fake"))
            return;
        this.InitCrawl();
        this.CrawlRecruiting();
        this.CrawlNonSubject();
    }

    @Scheduled(cron = "0 0 5 * * *",zone = "Asia/Seoul")
    @Transactional
    public void ScheduleCrawl() throws IOException {
        log.info("Schedule Called Crawl Uostory!");
        this.InitCrawl();
        this.CrawlRecruiting();
        this.CrawlNonSubject();
    }
    public void InitCrawl() throws IOException{
        log.info("crawl construct start");
        formData.put("_enpass_login_","submit");
        formData.put("langKnd","ko");
        formData.put("loginType","normal");
        formData.put("returnUrl","https://uostory.uos.ac.kr/");
        formData.put("ssoId",id);
        formData.put("password",password);

        sameHeader.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        sameHeader.put("Accept-Encoding", "gzip, deflate, br");
        sameHeader.put("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");

        first= Jsoup.connect("https://uostory.uos.ac.kr/index.jsp").method(Connection.Method.GET)
                .followRedirects(false)
                .userAgent(userAgent).headers(sameHeader).execute()
        ;

        Connection.Response response=Jsoup.connect("https://portal.uos.ac.kr/user/loginProcess.face").userAgent(userAgent).timeout(5000).data(formData)
                .method(Connection.Method.POST).headers(sameHeader).execute();
        System.out.println("Now again index.jsp ");
        Connection.Response indexJSPFirst = Jsoup.connect("https://uostory.uos.ac.kr/index.jsp").method(Connection.Method.GET)
                .cookie("JSESSIONID",first.cookie("JSESSIONID"))
                .followRedirects(false)
                .userAgent(userAgent).headers(sameHeader).execute()
                ;
        System.out.println("Now PSSO ");
        Connection.Response psso = Jsoup.connect("https://psso.uos.ac.kr/enpass/login?gateway=client&service=https://uostory.uos.ac.kr/index.jsp")
                .method(Connection.Method.GET).followRedirects(false).userAgent(userAgent).headers(sameHeader)
                .cookie("ENPASSTGC",response.cookie("ENPASSTGC")).cookie("JSESSIONID",response.cookie("JSESSIONID"))
                .execute();
        System.out.println("Now in index jsp with epTicket");
        String location = psso.headers().get("Location");
        URL url = new URL(location);
        String query = url.getQuery();
        System.out.println("query:"+query);
        Connection.Response indexJSPWithTicket = Jsoup.connect("https://uostory.uos.ac.kr/index.jsp?" + query).method(Connection.Method.GET).followRedirects(true)
                .userAgent(userAgent).headers(sameHeader).cookie("JSESSIONID",first.cookie("JSESSIONID")).execute();
        System.out.println("Now in logon");
        Connection.Response logon = Jsoup.connect("https://uostory.uos.ac.kr/site/member/logon").method(Connection.Method.GET).followRedirects(true)
                .userAgent(userAgent).headers(sameHeader).header("Referer", String.valueOf(indexJSPWithTicket.url())).cookie("JSESSIONID",first.cookie("JSESSIONID")).execute();
    }

//    @Transactional
    public void CrawlRecruiting() throws IOException {
        recruitingRepository.deleteAll();
        boolean isFin=false;
        int i=1;
        List<Recruiting> recruitingList=new ArrayList<>();

        while (!isFin){
            String url="https://uostory.uos.ac.kr/site/reservation/lecture/lectureList?menuid=001003002002&reservegroupid=1&viewtype=L&rectype=J&thumbnail=Y&currentpage="+i++;
            Document document = Jsoup.connect(url)
                    .userAgent(userAgent).headers(sameHeader).cookie("JSESSIONID", first.cookie("JSESSIONID")).get();
            Elements ul = document.select("#searchForm > div.list_tyle_h1.mt10 > ul");
            System.out.println("ul selected");
            Elements li = ul.select("li");

            for (Element liElement : li) {
                //li : 정보 1개
                Elements tbody = liElement.select("tbody");
                Elements trs = tbody.select("tr");
                boolean isPass=false;
                Recruiting recruiting=new Recruiting();
                for (int k = 0; k < trs.size(); k++) {
                    Element tr = trs.get(k);
//                    System.out.println(tr.text());
                    if(k==1){
                        //신청가능여부
                        if(!tr.text().equals("신청가능")){
                            isFin=isPass=true;
                            break;
                        }
                        else continue;
                    }
                    String[] s = tr.text().split(" ");
                    String tagName = s[0];
                    String tagValue = "";
                    if (s.length > 1) {
                        for (int j = 1; j < s.length; j++) {
                            tagValue += s[j] + " ";
                        }
                    }
                    if (tagName.equals("과정명")) {
                        Elements a = tr.select("a");
                        String href = a.attr("href");
                        String substring = href.substring(1, href.length());
                        String hrefUrl = "https://uostory.uos.ac.kr/site/reservation/lecture" + substring;
                        recruiting.setUrl(hrefUrl);
                        recruiting.setCourseName(tagValue);
                    } else if(tagName.equals("신청기간")){
                        recruiting.setApplyDate(tagValue);
                    } else if(tagName.equals("운영기간")){
                        recruiting.setRunDate(tagValue);
                    } else if(tagName.equals("대상학과"))
                        recruiting.setTargetDepartment(tagValue);
                    else if(tagName.equals("대상학년"))
                        recruiting.setTargetGrade(tagValue);
                }
                if(!isPass) {
                    recruitingList.add(recruiting);
                }

            }
        }
        recruitingRepository.saveAll(recruitingList);

    }
//    @Transactional
    public void CrawlNonSubject() throws IOException {
        nonSubjectRepository.deleteAll();
        List<NonSubject> nonSubjectList=new ArrayList<>();

        for(int i=1;i<10;i++){
            String url="https://uostory.uos.ac.kr/site/reservation/lecture/lectureList?menuid=001004002001&reservegroupid=1&viewtype=L&rectype=L&thumbnail=Y&currentpage="+i;
            Document document = Jsoup.connect(url)
                    .userAgent(userAgent).headers(sameHeader).cookie("JSESSIONID", first.cookie("JSESSIONID")).get();
            Elements ul = document.select("#searchForm > div.list_tyle_h1.mt10 > ul");
            System.out.println("ul selected");
            Elements li = ul.select("li");

            for (Element liElement : li) {
                //li : 정보 1개
                Elements tbody = liElement.select("tbody");
                Elements trs = tbody.select("tr");
                boolean isPass=false;
                NonSubject nonSubject = new NonSubject();
                for (int k = 0; k < trs.size(); k++) {
                    Element tr = trs.get(k);
                    System.out.println(tr.text());
                    if(k==1){
                        //신청가능여부
                        if(!tr.text().equals("신청가능")){
                            isPass=true;
                            break;
                        }
                        else continue;
                    }
                    String[] s = tr.text().split(" ");
                    String tagName = s[0];
                    String tagValue = "";
                    if (s.length > 1) {
                        for (int j = 1; j < s.length; j++) {
                            tagValue += s[j] + " ";
                        }
                    }
                    if (tagName.equals("과정명")) {
                        Elements a = tr.select("a");
                        String href = a.attr("href");
                        String substring = href.substring(1, href.length());
                        String hrefUrl = "https://uostory.uos.ac.kr/site/reservation/lecture" + substring;
                        nonSubject.setUrl(hrefUrl);
                        nonSubject.setCourseName(tagValue);
                    } else if(tagName.equals("신청기간")){
                        nonSubject.setApplyDate(tagValue);
                    } else if(tagName.equals("운영기간")){
                        nonSubject.setRunDate(tagValue);
                    } else if(tagName.equals("대상학과"))
                        nonSubject.setTargetDepartment(tagValue);
                    else if(tagName.equals("대상학년"))
                        nonSubject.setTargetGrade(tagValue);
                }
                if(!isPass) {
                    nonSubjectList.add(nonSubject);
                }

            }
        }
        nonSubjectRepository.saveAll(nonSubjectList);

    }



    public List<CrawlDto.crawlData> getPagedNonSubjects(int pageNum){
        Page<NonSubject> pages = nonSubjectRepository.findAll(PageRequest.of(pageNum, 15));
        List<CrawlDto.crawlData> result=new ArrayList<>();
        pages.toList().stream().forEach( n->result.add(CrawlDto.crawlData.builder().courseName(n.getCourseName())
                        .applyDate(n.getApplyDate()).runDate(n.getRunDate()).targetDepartment(n.getTargetDepartment())
                        .targetGrade(n.getTargetGrade()).url(n.getUrl())
                .build()));
        return result;
    }
    public List<CrawlDto.crawlData> getPagedRecruiting(int pageNum){
        Page<Recruiting> pages = recruitingRepository.findAll(PageRequest.of(pageNum, 15));
        List<CrawlDto.crawlData> result=new ArrayList<>();
        pages.toList().stream().forEach( n->result.add(CrawlDto.crawlData.builder().courseName(n.getCourseName())
                .applyDate(n.getApplyDate()).runDate(n.getRunDate()).targetDepartment(n.getTargetDepartment())
                .targetGrade(n.getTargetGrade()).url(n.getUrl())
                .build()));
        return result;
    }
}