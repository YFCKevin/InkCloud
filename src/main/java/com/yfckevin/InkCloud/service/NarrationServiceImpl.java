package com.yfckevin.InkCloud.service;

import com.yfckevin.InkCloud.entity.Narration;
import com.yfckevin.InkCloud.repository.NarrationRepository;
import org.springframework.stereotype.Service;

@Service
public class NarrationServiceImpl implements NarrationService{
    private final NarrationRepository narrationRepository;

    public NarrationServiceImpl(NarrationRepository narrationRepository) {
        this.narrationRepository = narrationRepository;
    }

    @Override
    public Narration save(Narration narration) {
        return narrationRepository.save(narration);
    }
}
