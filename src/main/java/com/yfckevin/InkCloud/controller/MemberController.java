package com.yfckevin.InkCloud.controller;

import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.entity.Member;
import com.yfckevin.InkCloud.exception.ResultStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class MemberController {
    private final ConfigProperties configProperties;

    public MemberController(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @GetMapping("/memberInfo")
    public Member memberInfo (HttpSession session) {
        final Member member = (Member) session.getAttribute("member");
        return Objects.requireNonNullElseGet(member, Member::new);
    }


    @GetMapping("/logout")
    public ResponseEntity<?> logout (HttpSession session){
        session.removeAttribute("member");
        ResultStatus resultStatus = new ResultStatus();
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(configProperties.getGlobalDomain() + "index.html");
        return ResponseEntity.ok(resultStatus);
    }
}
