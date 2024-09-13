package com.yfckevin.InkCloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.dto.ChatCompletionResponse;
import com.yfckevin.InkCloud.entity.Book;
import com.yfckevin.InkCloud.exception.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiServiceImpl implements OpenAiService {
    Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);
    private final ConfigProperties configProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OpenAiServiceImpl(ConfigProperties configProperties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.configProperties = configProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public final String prompt =
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

    @Override
    public ResultStatus<String> completion(String rawText) {
        ResultStatus<String> resultStatus = new ResultStatus<>();
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getApiKey());

        String data = createPayload(rawText + "\n" + prompt);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ChatCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("OpenAI回傳的status code: {}", response);
            ChatCompletionResponse responseBody = response.getBody();
            String content = extractContent(responseBody);
            System.out.println("GPT回傳資料 ======> " + content);

            if ("資料不符合".equals(content)) {
                logger.error("提供資料不符合");
                resultStatus.setCode("C998");
                resultStatus.setMessage("提供資料不符合");
                return resultStatus;
            }

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


    private String extractContent(ChatCompletionResponse responseBody) {
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
}
