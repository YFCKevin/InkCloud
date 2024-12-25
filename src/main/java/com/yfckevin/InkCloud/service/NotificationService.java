package com.yfckevin.InkCloud.service;

import com.yfckevin.InkCloud.entity.Notification;

import java.util.List;

public interface NotificationService {

    List<Notification> findByMemberIdOrderByCreationDateDesc(String memberId);
}
