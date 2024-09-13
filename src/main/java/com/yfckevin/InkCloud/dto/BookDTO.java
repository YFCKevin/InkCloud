package com.yfckevin.InkCloud.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

public class BookDTO {
    private String id;
    private String title;
    private String author;
    private String publisher;   //出版社
    private String sourceCoverPath;
    private String coverPath;
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

    public String getSourceCoverPath() {
        return sourceCoverPath;
    }

    public void setSourceCoverPath(String sourceCoverPath) {
        this.sourceCoverPath = sourceCoverPath;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    @Override
    public String toString() {
        return "BookDTO{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", sourceCoverPath='" + sourceCoverPath + '\'' +
                ", coverPath='" + coverPath + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", deletionDate='" + deletionDate + '\'' +
                '}';
    }
}
