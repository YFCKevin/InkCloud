package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yfckevin.InkCloud.entity.Book;
import com.yfckevin.InkCloud.exception.ResultStatus;

public interface OpenAiService {
    ResultStatus<String> completion(String textBuilder) throws JsonProcessingException;
}
