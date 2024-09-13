package com.yfckevin.InkCloud.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.*;
import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.dto.SearchDTO;
import com.yfckevin.InkCloud.entity.Book;
import com.yfckevin.InkCloud.repository.BookRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService{
    Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);
    private final ImageAnnotatorClient visionClient;
    private final BookRepository bookRepository;
    private final ConfigProperties configProperties;
    private final MongoTemplate mongoTemplate;

    public BookServiceImpl(BookRepository bookRepository, ConfigProperties configProperties, MongoTemplate mongoTemplate) throws IOException {
        this.configProperties = configProperties;
        this.mongoTemplate = mongoTemplate;
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> ServiceAccountCredentials.fromStream(new FileInputStream(this.configProperties.jsonPath + "bingBao-vision-secret-key.json")))
                .build();
        visionClient = ImageAnnotatorClient.create(settings);
        this.bookRepository = bookRepository;
    }

    @Override
    public String extractText(List<AnnotateImageRequest> requests) {
        AnnotateImageResponse response = visionClient.batchAnnotateImages(requests).getResponsesList().get(0);

        if (response.hasError()) {
            logger.error("Google Vision發生錯誤：" + response.getError().getMessage());
            throw new RuntimeException("error");
        }

        TextAnnotation annotations = response.getFullTextAnnotation();
        final String text = annotations.getText();

        return text;
    }

    @Override
    public List<Book> saveAll(List<Book> bookList) {
        return bookRepository.saveAll(bookList);
    }

    @Override
    public Optional<Book> findById(String id) {
        return bookRepository.findById(id);
    }

    @Override
    public void save(Book book) {
        bookRepository.save(book);
    }

    @Override
    public List<Book> findBook(SearchDTO searchDTO) {
        final String keyword = searchDTO.getKeyword().trim();
        List<Criteria> orCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(keyword)) {
            Criteria criteria_author = Criteria.where("author").regex(keyword, "i");
            Criteria criteria_publisher = Criteria.where("publisher").regex(keyword, "i");
            Criteria criteria_title = Criteria.where("title").regex(keyword, "i");
            orCriterias.add(criteria_author);
            orCriterias.add(criteria_publisher);
            orCriterias.add(criteria_title);
        }

        if(!orCriterias.isEmpty()) {
            criteria = criteria.orOperator(orCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);

        return mongoTemplate.find(query, Book.class);
    }
}
