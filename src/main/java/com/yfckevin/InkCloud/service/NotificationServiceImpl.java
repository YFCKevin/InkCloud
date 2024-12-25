package com.yfckevin.InkCloud.service;

import com.yfckevin.InkCloud.config.RabbitMQConfig;
import com.yfckevin.InkCloud.dto.NoticeDTO;
import com.yfckevin.InkCloud.entity.Notification;
import com.yfckevin.InkCloud.repository.NotificationRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService{

    private final NotificationRepository notificationRepository;
    private final SimpleDateFormat sdf;

    public NotificationServiceImpl(NotificationRepository notificationRepository, @Qualifier("sdf") SimpleDateFormat sdf) {
        this.notificationRepository = notificationRepository;
        this.sdf = sdf;
    }

    @RabbitListener(queues = RabbitMQConfig.DELAY_QUEUE)
    public void notice (NoticeDTO noticeDTO){
        final String memberId = noticeDTO.getMemberId();
        final String message = noticeDTO.getMessage();
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setMemberId(memberId);
        notification.setCreationDate(sdf.format(new Date()));
        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> findByMemberIdOrderByCreationDateDesc(String memberId) {
        return notificationRepository.findByMemberIdOrderByCreationDateDesc(memberId);
    }
}
