package com.yfckevin.InkCloud.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "video")
public class Video {
    @Id
    private String id;
    private String name;
    private String path;    //檔案路徑
    private String sourceBookId;    //哪一本書
    private String sourceNarrationId;
    private String sourceAudioId;
    private String imageName; //用逗號相連
    private long size;    //檔案大小(byte)
    private String memberId;
    private String error;   //紀錄錯誤訊息
    private String creationDate;
    private String creator;
    private String deletionDate;

    public String getDeletionDate() {
        return deletionDate;
    }

    public void setDeletionDate(String deletionDate) {
        this.deletionDate = deletionDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSourceBookId() {
        return sourceBookId;
    }

    public void setSourceBookId(String sourceBookId) {
        this.sourceBookId = sourceBookId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getSourceNarrationId() {
        return sourceNarrationId;
    }

    public void setSourceNarrationId(String sourceNarrationId) {
        this.sourceNarrationId = sourceNarrationId;
    }

    public String getSourceAudioId() {
        return sourceAudioId;
    }

    public void setSourceAudioId(String sourceAudioId) {
        this.sourceAudioId = sourceAudioId;
    }
}
