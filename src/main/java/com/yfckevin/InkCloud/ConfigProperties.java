package com.yfckevin.InkCloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigProperties {
    @Value("${config.apiKey}")
    public String apiKey;
    @Value("${spring.security.user.name}")
    public String username;
    @Value("${spring.security.user.password}")
    public String password;
    @Value("${config.jsonPath}")
    public String jsonPath;
    @Value("${config.fileSavePath}")
    public String fileSavePath;
    @Value("${config.picSavePath}")
    public String picSavePath;
    @Value("${config.picShowPath}")
    public String picShowPath;
    @Value("${config.audioSavePath}")
    public String audioSavePath;
    @Value("${config.videoSavePath}")
    public String videoSavePath;
    @Value("${config.videoShowPath}")
    public String videoShowPath;
    @Value("${config.globalDomain}")
    public String globalDomain;
    @Value("${spring.data.mongodb.uri}")
    public String mongodbUri;
    @Value("${spring.rabbitmq.host}")
    public String rabbitmqHost;
    @Value("${spring.rabbitmq.username}")
    public String rabbitmqUserName;
    @Value("${spring.rabbitmq.password}")
    public String rabbitmqPassword;
    @Value("${config.aiPicSavePath}")
    public String aiPicSavePath;
    @Value("${config.aiPicShowPath}")
    public String aiPicShowPath;
    @Value("${spring.security.oauth2.client.registration.line.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.line.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.line.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.line.user-info-uri}")
    private String userInfoUri;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getFileSavePath() {
        return fileSavePath;
    }

    public String getPicSavePath() {
        return picSavePath;
    }

    public String getPicShowPath() {
        return picShowPath;
    }

    public String getGlobalDomain() {
        return globalDomain;
    }

    public String getMongodbUri() {
        return mongodbUri;
    }

    public String getAiPicSavePath() {
        return aiPicSavePath;
    }

    public String getAiPicShowPath() {
        return aiPicShowPath;
    }

    public String getVideoSavePath() {
        return videoSavePath;
    }

    public String getAudioSavePath() {
        return audioSavePath;
    }

    public String getRabbitmqHost() {
        return rabbitmqHost;
    }

    public String getRabbitmqUserName() {
        return rabbitmqUserName;
    }

    public String getRabbitmqPassword() {
        return rabbitmqPassword;
    }

    public String getVideoShowPath() {
        return videoShowPath;
    }
}
