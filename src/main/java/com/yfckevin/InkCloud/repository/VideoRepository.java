package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends MongoRepository<Video, String> {
    List<Video> findByDeletionDateIsNull();

    Optional<Video> findBySourceBookId(String bookId);
}
