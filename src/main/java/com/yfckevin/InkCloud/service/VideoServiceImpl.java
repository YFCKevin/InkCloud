package com.yfckevin.InkCloud.service;

import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.config.RabbitMQConfig;
import com.yfckevin.InkCloud.dto.NoticeDTO;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Video;
import com.yfckevin.InkCloud.exception.ResultStatus;
import com.yfckevin.InkCloud.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VideoServiceImpl implements VideoService{
    Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    private final VideoRepository videoRepository;
    private final ConfigProperties configProperties;
    private final SimpleDateFormat sdf;
    private final RabbitTemplate rabbitTemplate;

    public VideoServiceImpl(VideoRepository videoRepository, ConfigProperties configProperties, @Qualifier("sdf") SimpleDateFormat sdf, RabbitTemplate rabbitTemplate) {
        this.videoRepository = videoRepository;
        this.configProperties = configProperties;
        this.sdf = sdf;
        this.rabbitTemplate = rabbitTemplate;
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

                    NoticeDTO noticeDTO = new NoticeDTO();
                    noticeDTO.setMessage("您的書籍 [" + workFlowDTO.getBookName() + "] 試聽影片製作完成，歡迎點擊試聽！");
                    noticeDTO.setMemberId(video.getMemberId());
                    rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE,
                            "notice.video",
                            noticeDTO,
                            message -> {
                                message.getMessageProperties().setDelay(1000);
                                return message;
                            }
                    );
                } else {
                    resultStatus.setCode("C999");
                    resultStatus.setMessage("FFmpeg失敗");
                    video.setError("FFmpeg失敗");
                    videoRepository.save(video);
                    logger.error("FFmpeg失敗，退出指令：{}", exitCode);
                }

            } catch (IOException | InterruptedException e) {
                logger.error("例外發生：{}", e.getMessage());
                resultStatus.setCode("C999");
                resultStatus.setMessage("例外發生");
                video.setError("FFmpeg例外發生");
                videoRepository.save(video);
            }
//        finally {
//            if (imageListFile.exists()) {
//                imageListFile.delete();
//            }
//        }
        }
    }


    @RabbitListener(queues = RabbitMQConfig.ERROR_QUEUE)
    public void errorQueueHandler(WorkFlowDTO workFlowDTO) {
        final String videoId = workFlowDTO.getVideoId();
        final Video video = videoRepository.findById(videoId).get();
        video.setError(workFlowDTO.getMsg());
        videoRepository.save(video);
    }

    @Override
    public Video save(Video video) {
        return videoRepository.save(video);
    }

    @Override
    public Optional<Video> findById(String videoId) {
        return videoRepository.findById(videoId);
    }

    @Override
    public List<Video> findByDeletionDateIsNull() {
        return videoRepository.findByDeletionDateIsNull();
    }

    @Override
    public Optional<Video> findBySourceBookId(String bookId) {
        return videoRepository.findBySourceBookId(bookId);
    }
}
