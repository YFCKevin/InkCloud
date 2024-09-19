package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yfckevin.InkCloud.dto.NarrationMsgDTO;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Book;
import com.yfckevin.InkCloud.exception.ResultStatus;

import java.util.Map;

public interface OpenAiService {
    ResultStatus<String> getBookInfo(String textBuilder) throws JsonProcessingException;

    void getNarration(NarrationMsgDTO narrationMsgDTO);

    void generateImage(WorkFlowDTO workFlowDTO) throws JsonProcessingException;
}
