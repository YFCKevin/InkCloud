package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Narration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NarrationRepository extends MongoRepository<Narration, String> {
}
