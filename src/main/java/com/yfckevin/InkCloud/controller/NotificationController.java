package com.yfckevin.InkCloud.controller;

import com.yfckevin.InkCloud.entity.Member;
import com.yfckevin.InkCloud.entity.Notification;
import com.yfckevin.InkCloud.exception.ResultStatus;
import com.yfckevin.InkCloud.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationController {
    Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 呈現個人的訊息通知
     * @param memberId
     * @param session
     * @return
     */
    @GetMapping("/notifications/{memberId}")
    public ResponseEntity<?> notifications (@PathVariable String memberId, HttpSession session){

        final Member member = (Member) session.getAttribute("member");
        ResultStatus resultStatus = new ResultStatus();
        if (member != null) {
            logger.info("[" + member.getName() + "]" + "[notifications]");
        }

        List<Notification> notificationList = notificationService.findByMemberIdOrderByCreationDateDesc(memberId);

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(notificationList);
        return ResponseEntity.ok(resultStatus);
    }
}
