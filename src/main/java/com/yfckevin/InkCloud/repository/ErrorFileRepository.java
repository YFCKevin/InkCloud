package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.ErrorFile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ErrorFileRepository extends MongoRepository<ErrorFile, String> {
}
