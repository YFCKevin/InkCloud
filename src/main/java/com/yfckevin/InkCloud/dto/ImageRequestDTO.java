package com.yfckevin.InkCloud.dto;

import java.util.ArrayList;
import java.util.List;

public class ImageRequestDTO {
    private List<String> images = new ArrayList<>();
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
