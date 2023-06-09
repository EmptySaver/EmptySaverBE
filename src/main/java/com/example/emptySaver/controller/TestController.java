package com.example.emptySaver.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class TestController {
    @GetMapping("/helloTest")
    public String helloTest(Model model){
        log.info("Called HelloTest");
        model.addAttribute("greet","hello from Controller~");
        return "test/hello";
    }
    @GetMapping("/needAuth")
    @ResponseBody
    public String needAuth(){
        log.info("Called HelloTest");
        return "you are authenticated";
    }
    @GetMapping("/authTest")
    public String authTest(Model model){
        log.error("Called AuthTest");
        //model.addAttribute("code","Not Yet");
        return "test/authTest";
    }
}
