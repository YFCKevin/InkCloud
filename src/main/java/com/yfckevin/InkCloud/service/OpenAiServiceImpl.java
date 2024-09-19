package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.config.RabbitMQConfig;
import com.yfckevin.InkCloud.dto.ChatCompletionResponse;
import com.yfckevin.InkCloud.dto.ImageCompletionResponse;
import com.yfckevin.InkCloud.dto.NarrationMsgDTO;
import com.yfckevin.InkCloud.dto.WorkFlowDTO;
import com.yfckevin.InkCloud.entity.Narration;
import com.yfckevin.InkCloud.entity.Video;
import com.yfckevin.InkCloud.exception.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OpenAiServiceImpl implements OpenAiService {
    Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);
    private final ConfigProperties configProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final NarrationService narrationService;
    private final VideoService videoService;
    private final SimpleDateFormat sdf;
    private final RabbitTemplate rabbitTemplate;

    public OpenAiServiceImpl(ConfigProperties configProperties, ObjectMapper objectMapper, RestTemplate restTemplate, NarrationService narrationService, VideoService videoService, @Qualifier("sdf") SimpleDateFormat sdf, RabbitTemplate rabbitTemplate) {
        this.configProperties = configProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.narrationService = narrationService;
        this.videoService = videoService;
        this.sdf = sdf;
        this.rabbitTemplate = rabbitTemplate;
    }

    public final String prompt_book =
            "1. 書籍如上，我要把資訊整理出 JSON 檔，欄位包括：\n" +
                    "- title：書名\n" +
                    "- author：作者，書籍的作者名稱，可以是單個或多個作者，若有多個用逗號相連\n" +
                    "- publisher：出版社，出版該書籍的出版社名稱\n" +
                    "2. 內容文字可能包含多本書籍資訊，必須分開成獨立的 JSON 物件。\n" +
                    "每本書籍資訊應用 {} 包含，並且書籍之間應用 , 分隔。\n" +
                    "3. 每本書籍資訊只包括以下三個欄位：\n" +
                    "- title (String)\n" +
                    "- author (String)\n" +
                    "- publisher (String)\n" +
                    "4. 請確保輸出為有效的 JSON 格式，並且滿足如下要求：\n" +
                    "- 若作者有多個，請用逗號分隔\n" +
                    "- 任何其他非書籍的文本內容應忽略\n" +
                    "5. 輸入的書籍資訊文本範例如下：\n" +
                    "書名: 初心\n" +
                    "作者: 江振誠\n" +
                    "出版社: 平安叢書\n\n" +
                    "書名: 舌尖上的古代中國\n" +
                    "作者: 古人很潮\n" +
                    "出版社: 遠流\n\n" +
                    "6. 輸出格式範例如下：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"title\": \"初心\",\n" +
                    "    \"author\": \"江振誠\",\n" +
                    "    \"publisher\": \"平安叢書\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"title\": \"舌尖上的古代中國\",\n" +
                    "    \"author\": \"古人很潮\",\n" +
                    "    \"publisher\": \"遠流\"\n" +
                    "  }\n" +
                    "]\n\n" +
                    "7. 確保輸出只有 JSON 格式，無需多餘的描述或文本。\n" +
                    "8. 如果某本書的 title、author 或 publisher 欄位缺失，該欄位設定為空字串。";

    public final String prompt_video =
            "1.書名與作者如上，我要你給我200字的摘要文章，內容包含：\n" +
                    "書籍主題或核心思想\n" +
                    "主要情節\n" +
                    "重點內容\n" +
                    "從中學到什麼或感受到什麼\n" +
                    "寫作風格或特色\n" +
                    "為何值得一讀";


    @Override
    public ResultStatus<String> getBookInfo(String rawText) {
        ResultStatus<String> resultStatus = new ResultStatus<>();
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getApiKey());

        String data = createPayload(rawText + "\n" + prompt_book);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ChatCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("OpenAI回傳的status code: {}", response);
            ChatCompletionResponse responseBody = response.getBody();
            String content = extractJsonContent(responseBody);
            System.out.println("GPT回傳資料 ======> " + content);

            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(content);
        } else {
            logger.error("openAI錯誤發生");
            resultStatus.setCode("C999");
            resultStatus.setMessage("異常發生");
        }
        return resultStatus;
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.LLM_QUEUE)
    public void getNarration(NarrationMsgDTO dto) {

        final Video video = videoService.findById(dto.getVideoId()).get();

        WorkFlowDTO workFlowDTO = new WorkFlowDTO();

        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getApiKey());

        String data = createPayload(dto.getText() + "\n" + prompt_video);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ChatCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("OpenAI回傳的status code: {}", response);
            ChatCompletionResponse responseBody = response.getBody();
            String content = extractTextContent(responseBody);
            System.out.println("GPT回傳資料 ======> " + content);

            Narration narration = new Narration();
            narration.setSourceBookId(dto.getBookId());
            narration.setText(content);
            narration.setCreationDate(sdf.format(new Date()));
            Narration savedNarration = narrationService.save(narration);

            if (savedNarration != null) {
                workFlowDTO.setCode("C000");
                workFlowDTO.setMsg("成功");
                workFlowDTO.setBookId(dto.getBookId());
                workFlowDTO.setBookName(dto.getBookName());
                workFlowDTO.setNarrationId(savedNarration.getId());
                workFlowDTO.setNarration(savedNarration.getText());
                workFlowDTO.setVideoId(dto.getVideoId());
                logger.info("旁白儲存成功，繼續執行製作mp3");
                rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.audio", workFlowDTO);
            } else {
                workFlowDTO.setCode("C999");
                workFlowDTO.setMsg("旁白儲存失敗");
                video.setError("旁白儲存失敗");
                logger.error("旁白儲存失敗，導向錯誤");
                rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.error", workFlowDTO);
            }
        } else {
            workFlowDTO.setCode("C999");
            workFlowDTO.setMsg("openAI錯誤發生");
            video.setError("[旁白] openAI錯誤發生");
            logger.error("[旁白] openAI錯誤發生，狀態碼：{}，導向錯誤", response.getStatusCode());
            rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.error", workFlowDTO);
        }
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.IMAGE_QUEUE)
    public void generateImage(WorkFlowDTO workFlowDTO) {

        final Video video = videoService.findById(workFlowDTO.getVideoId()).get();

        final String bookName = workFlowDTO.getBookName();
        final String content = workFlowDTO.getNarration();

        String url = "https://api.openai.com/v1/images/generations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getApiKey());

        String payload = createImagePayload(content);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<ImageCompletionResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ImageCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("OpenAI回傳的status code: {}", response);
            List<Path> downloadedPaths = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            ImageCompletionResponse responseBody = response.getBody();
            for (ImageCompletionResponse.DataDTO imgUrl : responseBody.getData()) {
                try {
                    URL imageUrl = new URL(imgUrl.getUrl());
                    try (InputStream inputStream = imageUrl.openStream()) {
                        String fileName = Paths.get(imageUrl.getPath()).getFileName().toString();
                        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : ".jpg";
                        String newFileName = bookName + "_" + System.currentTimeMillis() + extension;
                        Path filePath = Paths.get(configProperties.getPicSavePath()).resolve(newFileName);
                        Files.copy(inputStream, filePath);
                        System.out.println("圖片已下載: " + filePath);

                        downloadedPaths.add(filePath);
                        fileNames.add(newFileName);
                    }
                } catch (IOException e) {
                    workFlowDTO.setCode("C999");
                    workFlowDTO.setMsg("下載圖片時發生錯誤");
                    video.setError("下載圖片時發生錯誤");
                    logger.error("下載圖片時發生錯誤: {}", e.getMessage());
                    rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.error", workFlowDTO);
                }
            }

            boolean allFilesValid = downloadedPaths.stream()
                    .allMatch(path -> {
                        try {
                            return Files.exists(path) && Files.size(path) > 0;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });


            if (allFilesValid) {
                logger.info("圖片儲存成功，繼續執行製作影片");
                workFlowDTO.setCode("C000");
                workFlowDTO.setMsg("成功");
                workFlowDTO.setImageName(fileNames.get(0)); //只取第一張 String.join(",", fileNames)
                System.out.println("fileNames.get(0): " + fileNames.get(0));
                rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.video", workFlowDTO);
            } else {
                workFlowDTO.setCode("C999");
                workFlowDTO.setMsg("圖片儲存失敗");
                video.setError("圖片儲存失敗");
                logger.error("圖片儲存失敗，導向錯誤");
                rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.error", workFlowDTO);
            }

        } else {
            workFlowDTO.setCode("C999");
            workFlowDTO.setMsg("openAI錯誤發生");
            video.setError("[產圖] openAI錯誤發生");
            logger.error("openAI錯誤發生，狀態碼：{}，導向錯誤", response.getStatusCode());
            rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_EXCHANGE, "workflow.error", workFlowDTO);
        }
    }


    private String createPayload(String prompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-4o-mini");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        payload.put("messages", new Object[]{message});

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    private String createImagePayload(String prompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "dall-e-2");
        payload.put("prompt", prompt);
        payload.put("n", 1);
        payload.put("size", "512x512");

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    private String extractJsonContent(ChatCompletionResponse responseBody) {
        if (responseBody != null && !responseBody.getChoices().isEmpty()) {
            ChatCompletionResponse.Choice choice = responseBody.getChoices().get(0);
            if (choice != null && choice.getMessage() != null) {
                String content = choice.getMessage().getContent().trim();

                // 去掉反引號
                if (content != null) {
                    content = content.replace("```json", "").replace("```", "").trim();
                }

                return content;
            }
        }
        return null;
    }


    private String extractTextContent(ChatCompletionResponse responseBody) {
        if (responseBody != null && !responseBody.getChoices().isEmpty()) {
            ChatCompletionResponse.Choice choice = responseBody.getChoices().get(0);
            if (choice != null && choice.getMessage() != null) {
                return choice.getMessage().getContent().trim();
            }
        }
        return null;
    }
}
