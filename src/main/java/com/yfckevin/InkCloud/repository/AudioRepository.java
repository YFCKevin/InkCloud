package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Audio;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AudioRepository extends MongoRepository<Audio, String> {
}
