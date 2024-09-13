package com.yfckevin.InkCloud.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "book")
public class Book {
    @Id
    private String id;
    private String title;
    private String author;
    private String publisher;   //出版社
    private String sourceCoverName;
    private String coverName;
    private String creationDate;
    private String deletionDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getDeletionDate() {
        return deletionDate;
    }

    public void setDeletionDate(String deletionDate) {
        this.deletionDate = deletionDate;
    }

    public String getSourceCoverName() {
        return sourceCoverName;
    }

    public void setSourceCoverName(String sourceCoverName) {
        this.sourceCoverName = sourceCoverName;
    }

    public String getCoverName() {
        return coverName;
    }

    public void setCoverName(String coverName) {
        this.coverName = coverName;
    }
}
