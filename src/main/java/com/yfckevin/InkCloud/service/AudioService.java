package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Audio;
import com.yfckevin.InkCloud.exception.ResultStatus;

import java.util.Map;

public interface AudioService {
    Audio save(Audio audio);

    void textToSpeech(WorkFlowDTO workFlowDTO) throws JsonProcessingException;
}
