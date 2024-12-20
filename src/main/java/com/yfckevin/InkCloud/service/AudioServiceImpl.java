package com.yfckevin.InkCloud.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.config.RabbitMQConfig;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Audio;
import com.yfckevin.InkCloud.entity.Video;
import com.yfckevin.InkCloud.repository.AudioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AudioServiceImpl implements AudioService{
    Logger logger = LoggerFactory.getLogger(AudioServiceImpl.class);
    private final AudioRepository audioRepository;
    private final ConfigProperties configProperties;
    private final SimpleDateFormat sdf;
    private final RabbitTemplate rabbitTemplate;
    private final VideoService videoService;
    public AudioServiceImpl(AudioRepository audioRepository, ConfigProperties configProperties, @Qualifier("sdf") SimpleDateFormat sdf, RabbitTemplate rabbitTemplate, VideoService videoService) {
        this.audioRepository = audioRepository;
        this.configProperties = configProperties;
        this.sdf = sdf;
        this.rabbitTemplate = rabbitTemplate;
        this.videoService = videoService;
    }

    @Override
    public Audio save(Audio audio) {
        return audioRepository.save(audio);
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.AUDIO_QUEUE)
    public void textToSpeech(WorkFlowDTO workFlowDTO) {

        final Video video = videoService.findById(workFlowDTO.getVideoId()).get();

        final String bookId = workFlowDTO.getBookId();
        final String bookName = workFlowDTO.getBookName();
        final String content = workFlowDTO.getNarration();
        final String memberId = workFlowDTO.getMemberId();

        FileSystemResource resource = new FileSystemResource(configProperties.getJsonPath() + "text-and-speech-secret-key.json");

        // 取得憑證資料
        try (InputStream inputStream = resource.getInputStream()) {
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> ServiceAccountCredentials.fromStream(inputStream))
                    .build();

            // 使用 TextToSpeechClient 打 API
            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {

                // construct request
                SynthesisInput input = SynthesisInput.newBuilder().setText(content).build();
                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode("zh-TW")
                        .setSsmlGender(SsmlVoiceGender.FEMALE)
                        .build();
                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .build();

                // 打 Text-to-Speech API
                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

                byte[] audioContents = response.getAudioContent().toByteArray();
                try (OutputStream out = new FileOutputStream(configProperties.getAudioSavePath() + bookName + ".mp3")) {
                    out.write(audioContents);
                    Audio audio = new Audio();
                    audio.setPath(configProperties.getAudioSavePath() + bookName + ".mp3");
                    final Path filePath = Paths.get(configProperties.getAudioSavePath() + bookName + ".mp3");
                    audio.setSize(Files.size(filePath));
                    audio.setSourceBookId(bookId);
                    audio.setCreationDate(sdf.format(new Date()));
                    audio.setName(bookName + "_" + System.currentTimeMillis());
                    audio.setMemberId(memberId);
                    Audio savedAudio = audioRepository.save(audio);

                    try {
                        if (savedAudio != null) {
                            workFlowDTO.setCode("C000");
                            workFlowDTO.setMsg("成功");
                            workFlowDTO.setAudioId(savedAudio.getId());
                            workFlowDTO.setAudioPath(savedAudio.getPath());
                            logger.info("音訊儲存成功，繼續執行生成圖片");
                            rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.image", workFlowDTO);
                        } else {
                            workFlowDTO.setCode("C999");
                            workFlowDTO.setMsg("音訊儲存失敗");
                            video.setError("音訊儲存失敗");
                            logger.error("音訊儲存失敗，導向錯誤");
                            rabbitTemplate.convertAndSend(RabbitMQConfig.ERROR_EXCHANGE, "error.audio", workFlowDTO);
                        }
                    } catch (Exception e) {
                        workFlowDTO.setCode("C999");
                        workFlowDTO.setMsg("保存音訊時發生錯誤");
                        video.setError("保存音訊時發生錯誤");
                        logger.error("保存音訊時發生錯誤: {}", e.getMessage());
                        rabbitTemplate.convertAndSend(RabbitMQConfig.ERROR_EXCHANGE, "error.audio", workFlowDTO);
                    }
                }
            }
        } catch (IOException e) {
            workFlowDTO.setCode("C999");
            workFlowDTO.setMsg(e.getMessage());
            video.setError("[音訊] google發生錯誤");
            logger.error(e.getMessage(), e);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ERROR_EXCHANGE, "error.audio", workFlowDTO);
        }
    }
}
