package com.yfckevin.InkCloud.oauth;

import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.entity.Member;
import com.yfckevin.InkCloud.exception.ResultStatus;
import com.yfckevin.InkCloud.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Controller
public class Oauth2Controller {
    private final ConfigProperties configProperties;
    private final MemberService memberService;
    private final RestTemplate restTemplate;

    public Oauth2Controller(ConfigProperties configProperties, MemberService memberService, RestTemplate restTemplate) {
        this.configProperties = configProperties;
        this.memberService = memberService;
        this.restTemplate = restTemplate;
    }


    @GetMapping("/callback")
    public String handleOAuth2Callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session,
            Model model
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = "grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + configProperties.getGlobalDomain() + "callback" +
                "&client_id=" + configProperties.getClientId() +
                "&client_secret=" + configProperties.getClientSecret();

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                configProperties.getTokenUri(),
                HttpMethod.POST,
                requestEntity,
                Map.class);

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userRequestEntity = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                configProperties.getUserInfoUri(),
                HttpMethod.GET,
                userRequestEntity,
                Map.class);

        Map<String, Object> userInfo = userResponse.getBody();

        String userId = (String) userInfo.get("userId");
        String userName = (String) userInfo.get("displayName");
        final String pictureUrl = (String) userInfo.get("pictureUrl");

        Optional<Member> memberOpt = memberService.findByUserId(userId);
        Member member;
        if (memberOpt.isEmpty()) {
            member = new Member();
            member.setName(userName);
            member.setPictureUrl(pictureUrl);
            member.setUserId(userId);
            memberService.save(member);
        } else {
            member = memberOpt.get();
        }
        session.setAttribute("member", member);
        return "redirect:index.html";
    }
}
