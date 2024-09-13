package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Book;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookRepository extends MongoRepository<Book, String> {
}
