package com.yfckevin.InkCloud.service;

import com.yfckevin.InkCloud.entity.ErrorFile;
import com.yfckevin.InkCloud.repository.ErrorFileRepository;
import org.springframework.stereotype.Service;

@Service
public class ErrorFileServiceImpl implements ErrorFileService{
    private final ErrorFileRepository errorFileRepository;

    public ErrorFileServiceImpl(ErrorFileRepository errorFileRepository) {
        this.errorFileRepository = errorFileRepository;
    }

    @Override
    public ErrorFile save(ErrorFile errorFile) {
        return errorFileRepository.save(errorFile);
    }
}
