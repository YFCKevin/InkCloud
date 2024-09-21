package com.yfckevin.InkCloud.controller;

import com.yfckevin.InkCloud.entity.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class MemberController {

    @GetMapping("/memberInfo")
    public Member memberInfo (HttpSession session) {
        final Member member = (Member) session.getAttribute("member");
        return Objects.requireNonNullElseGet(member, Member::new);
    }
}
