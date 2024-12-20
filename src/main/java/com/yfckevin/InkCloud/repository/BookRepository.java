package com.yfckevin.InkCloud.repository;

import com.yfckevin.InkCloud.entity.Book;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findByTypeAndDeletionDateIsNull(String type);

    Optional<Book> findByIdAndMemberId(String id, String memberId);
}
