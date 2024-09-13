package com.yfckevin.InkCloud.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.yfckevin.InkCloud.dto.SearchDTO;
import com.yfckevin.InkCloud.entity.Book;

import java.util.List;
import java.util.Optional;

public interface BookService {
    String extractText(List<AnnotateImageRequest> requests);

    List<Book> saveAll(List<Book> bookList);

    Optional<Book> findById(String id);

    void save(Book book);

    List<Book> findBook(SearchDTO searchDTO);
}
