package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.config.RabbitMQConfig;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Audio;
import com.yfckevin.InkCloud.entity.Narration;
import com.yfckevin.InkCloud.entity.Video;
import com.yfckevin.InkCloud.exception.ResultStatus;
import com.yfckevin.InkCloud.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VideoServiceImpl implements VideoService{
    Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    private final VideoRepository videoRepository;
    private final ConfigProperties configProperties;
    private final SimpleDateFormat sdf;

    public VideoServiceImpl(VideoRepository videoRepository, ConfigProperties configProperties, @Qualifier("sdf") SimpleDateFormat sdf) {
        this.videoRepository = videoRepository;
        this.configProperties = configProperties;
        this.sdf = sdf;
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.VIDEO_QUEUE)
    public void generateVideo(WorkFlowDTO workFlowDTO) {

        ResultStatus resultStatus = new ResultStatus();

        final Optional<Video> opt = videoRepository.findById(workFlowDTO.getVideoId());
        if (opt.isEmpty()) {
            resultStatus.setCode("C002");
            resultStatus.setMessage("查無影片");
        } else {
            final Video video = opt.get();
            final String bookName = workFlowDTO.getBookName();
            final String narrationId = workFlowDTO.getNarrationId();
            final String audioId = workFlowDTO.getAudioId();
            final String audioPath = workFlowDTO.getAudioPath();
            final String imageNames = workFlowDTO.getImageName();
//        final List<String> imageNameList = Arrays.asList(imageNames.split(","));

            String outputPath = configProperties.getVideoSavePath() + bookName + "_" + System.currentTimeMillis() + ".mp4";

//        File imageListFile = new File("image_list.txt");
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(imageListFile))) {
//            for (String imageName : imageNameList) {
//                String imagePath = Paths.get(configProperties.getPicSavePath(), imageName).toString();
//                writer.write("file '" + imagePath + "'");
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            resultStatus.setCode("C999");
//            resultStatus.setMessage(e.getMessage());
//            logger.error("寫入失敗：{}", e.getMessage());
//        }

            // FFmpeg 命令
            String[] command = {
                    "ffmpeg",
                    "-loop", "1",                      // 循環圖片
                    "-i", configProperties.getPicSavePath() + imageNames, // 輸入圖片
                    "-i", audioPath,                  // 輸入音訊
                    "-c:v", "libx264",                // 設置視頻編碼器
                    "-tune", "stillimage",            // 調整為靜態圖片
                    "-c:a", "aac",                    // 設置音訊編碼器
                    "-b:a", "192k",                   // 設置音訊比特率
                    "-pix_fmt", "yuv420p",            // 設置像素格式
                    "-shortest",                       // 使視頻和音訊長度一致
                    outputPath                         // 輸出文件路徑
            };

            // 执行 FFmpeg 命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();

                // 输出 FFmpeg 命令的標準輸出
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }

                // 等待 FFmpeg 命令完成
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    video.setImageName(imageNames);
                    video.setCreationDate(sdf.format(new Date()));
                    video.setPath(outputPath);
                    video.setSourceAudioId(audioId);
                    video.setSourceNarrationId(narrationId);
                    File videoFile = new File(outputPath);
                    if (videoFile.exists()) {
                        video.setName(videoFile.getName());
                        video.setSize(Files.size(videoFile.toPath()));
                    }
                    videoRepository.save(video);
                    resultStatus.setCode("C000");
                    resultStatus.setMessage("成功");
                    logger.info("影片製作完成");
                } else {
                    resultStatus.setCode("C999");
                    resultStatus.setMessage("FFmpeg失敗");
                    video.setError("FFmpeg失敗");
                    logger.error("FFmpeg失敗，退出指令：{}", exitCode);
                }

            } catch (IOException | InterruptedException e) {
                logger.error("例外發生：{}", e.getMessage());
                resultStatus.setCode("C999");
                resultStatus.setMessage("例外發生");
                video.setError("FFmpeg例外發生");
            }
//        finally {
//            if (imageListFile.exists()) {
//                imageListFile.delete();
//            }
//        }
        }
    }

    @Override
    public Video save(Video video) {
        return videoRepository.save(video);
    }

    @Override
    public Optional<Video> findById(String videoId) {
        return videoRepository.findById(videoId);
    }
}
