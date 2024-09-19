package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VideoRepository extends MongoRepository<Video, String> {
}
