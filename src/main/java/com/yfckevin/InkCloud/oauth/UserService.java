package com.yfckevin.InkCloud.oauth;

import com.yfckevin.InkCloud.entity.Member;
import com.yfckevin.InkCloud.enums.Provider;
import com.yfckevin.InkCloud.service.MemberService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Service
public class UserService {

    private final MemberService memberService;
    private final SimpleDateFormat sdf;

    public UserService(MemberService memberService, @Qualifier("sdf") SimpleDateFormat sdf) {
        this.memberService = memberService;
        this.sdf = sdf;
    }

    public Member processOAuthPostLogin(String email, String name, String oauth2ClientName) {

        //取得系統上是不是有這個帳號
        Optional<Member> opt = memberService.findByEmail(email);
        Member member = new Member();

        //取得是GOOGLE或FACEBOOK登入
        Provider authType = Provider.valueOf(oauth2ClientName.toUpperCase());
        System.out.println("authType==>" + authType);

        if (opt.isEmpty()) { //如果沒有註冊過就新增
            member.setName(name);
            member.setEmail(email);
            member.setCreationDate(sdf.format(new Date()));
            member.setProvider(authType);
            memberService.save(member);
            System.out.println("尚未註冊");
        } else {
            member = opt.get();
        }
        return member;
    }

}
