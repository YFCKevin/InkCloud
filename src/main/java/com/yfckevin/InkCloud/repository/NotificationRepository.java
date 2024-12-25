package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByMemberIdOrderByCreationDateDesc(String memberId);
}