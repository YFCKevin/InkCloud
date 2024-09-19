package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Video;
import com.yfckevin.InkCloud.exception.ResultStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VideoService {
    void generateVideo(WorkFlowDTO workFlowDTO) throws JsonProcessingException;

    Video save(Video video);

    Optional<Video> findById(String videoId);

    List<Video> findByDeletionDateIsNull();

    Optional<Video> findBySourceBookId(String bookId);
}
