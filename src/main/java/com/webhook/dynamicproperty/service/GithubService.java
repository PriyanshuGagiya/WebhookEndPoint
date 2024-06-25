package com.webhook.dynamicproperty.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.PartnerLevelConfigBeanDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GithubService {

    private static final Logger logger = LoggerFactory.getLogger(GithubService.class);

    @Autowired
    private YamlToJsonService yamlToJsonService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.api.url}")
    private String githubDownloadUrl;

    public void processWebhookPayload(JsonNode jsonNode) {
        try {
            JsonNode commits = jsonNode.get("commits");

            for (JsonNode commit : commits) {
                String commitId = commit.get("id").asText();
                JsonNode addedFiles = commit.get("added");
                JsonNode modifiedFiles = commit.get("modified");

                OffsetDateTime offsetDateTime = OffsetDateTime.parse(commit.get("timestamp").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
                processFiles(addedFiles, commitId, localDateTime);
                processFiles(modifiedFiles, commitId, localDateTime);
            }
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook payload", e);
        }
    }

    private void processFiles(JsonNode files, String commitId, LocalDateTime commitTime) {
        for (JsonNode file : files) {
            String filePath = file.asText();
            String[] filesPathSplit = filePath.split("/");
            if (filesPathSplit.length < 4) {
                if (filesPathSplit.length == 3) {
                    processGlobalFiles(filesPathSplit, commitId, commitTime);
                }
                continue;
            }
            String databaseName = filesPathSplit[2];
            if (!databaseName.equals(activeProfile)) {
                continue;
            }
            String collectionName = filesPathSplit[1];
            String url = githubDownloadUrl + commitId + "/" + filePath;

            JsonNode content = fetchFileContent(url);
            if (content == null) {
                continue;
            }

            switch (collectionName) {
                case "dynamicProperty":
                    handleDynamicProperty(commitTime, collectionName, content);
                    break;
                case "serverConfig":
                    handleServerConfig(commitTime, collectionName, content);
                    break;
                case "sprProperty":
                    handleSprProperty(commitTime, collectionName, content);
                    break;
                case "partnerLevelConfigBean":
                    handlePartnerLevelConfigBean(commitTime, collectionName, content);
                    break;
                default:
                    logger.warn("Unknown collection: {}", collectionName);
            }
        }
    }

    private void processGlobalFiles(String[] filesPathSplit, String commitId, LocalDateTime commitTime) {
        String collectionName = filesPathSplit[1];
        String url = githubDownloadUrl + commitId + "/" + filesPathSplit[0] + "/" + filesPathSplit[1] + "/" + filesPathSplit[2];
        JsonNode content = fetchFileContent(url);
        if (content == null) {
            return;
        }
        switch (collectionName) {
            case "dynamicProperty":
                handleDynamicProperty(commitTime, collectionName, content);
                break;
            case "serverConfig":
                handleServerConfig(commitTime, collectionName, content);
                break;
            case "sprProperty":
                handleSprProperty(commitTime, collectionName, content);
                break;
            case "partnerLevelConfigBean":
                handlePartnerLevelConfigBean(commitTime, collectionName, content);
                break;
            default:
                logger.warn("Unknown collection: {}", collectionName);
        }
    }

    private JsonNode fetchFileContent(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + githubToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String content = response.getBody();
            return yamlToJsonService.convertYamlToJson(content);
        } catch (Exception e) {
            logger.error("Error fetching file content from GitHub: {}", url, e);
            return null;
        }
    }

    private void handleDynamicProperty(LocalDateTime commitTime, String collectionName, JsonNode content) {
        DynamicPropertyDetails dynamicPropertyDetails = new DynamicPropertyDetails();
        dynamicPropertyDetails.setModifiedDate(commitTime);
        dynamicPropertyDetails.setKey(content.get("key").asText());
        dynamicPropertyDetails.setProperty(content.get("property").asText());
        dynamicPropertyDetails.setValue(content.get("value").asText());
        dynamicPropertyDetails.setReason(content.get("reason").asText());
        dynamicPropertyDetails.setDeleted(content.get("deleted").asBoolean());
        propertyService.saveProperty(dynamicPropertyDetails, collectionName, "key");
    }

    private void handleServerConfig(LocalDateTime commitTime, String collectionName, JsonNode content) {
        ServerConfigDetails serverConfigDetails = new ServerConfigDetails();
        serverConfigDetails.setModifiedDate(commitTime);
        serverConfigDetails.setDbName(content.get("dbName").asText());
        serverConfigDetails.setUrl(content.get("url").asText());
        serverConfigDetails.setPartnerId(content.get("partnerId").asLong());
        serverConfigDetails.setClientId(content.get("clientId").asLong());
        serverConfigDetails.setServerCategory(content.get("serverCategory").asText());
        serverConfigDetails.setServerType(content.get("serverType").asText());
        serverConfigDetails.setName(content.get("name").asText());
        serverConfigDetails.set_class(content.get("_class").asText());
        propertyService.saveProperty(serverConfigDetails, collectionName, "name");
    }

    private void handleSprProperty(LocalDateTime commitTime, String collectionName, JsonNode content) {
        SprPropertyDetails sprPropertyDetails = new SprPropertyDetails();
        sprPropertyDetails.setModifiedDate(commitTime);
        sprPropertyDetails.setKey(content.get("key").asText());
        sprPropertyDetails.setValue(content.get("value").asText());
        sprPropertyDetails.setSecure(content.get("isSecure").asBoolean());
        sprPropertyDetails.set_class(content.get("_class").asText());
        propertyService.saveProperty(sprPropertyDetails, collectionName, "key");
    }

    private void handlePartnerLevelConfigBean(LocalDateTime commitTime, String collectionName, JsonNode content) {
        PartnerLevelConfigBeanDetails partnerLevelConfigBean = new PartnerLevelConfigBeanDetails();
        partnerLevelConfigBean.setModifiedDate(commitTime);
        Map<String, Object> config = convertJsonNodeToMap(content.get("config"));
        partnerLevelConfigBean.setConfig(config);
        partnerLevelConfigBean.set_class(content.get("_class").asText());
        propertyService.saveProperty(partnerLevelConfigBean, collectionName, List.of("config.module", "config.type", "config.configClassName"));
    }

    private Map<String, Object> convertJsonNodeToMap(JsonNode jsonNode) {
        Map<String, Object> map = new HashMap<>();
        jsonNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isObject()) {
                map.put(entry.getKey(), convertJsonNodeToMap(entry.getValue()));
            } else if (entry.getValue().isArray()) {
                List<Object> list = new ArrayList<>();
                entry.getValue().forEach(item -> {
                    if (item.isObject()) {
                        list.add(convertJsonNodeToMap(item));
                    } else {
                        list.add(item.asText());
                    }
                });
                map.put(entry.getKey(), list);
            } else {
                if (entry.getValue().isInt()) {
                    map.put(entry.getKey(), entry.getValue().intValue());
                } else if (entry.getValue().isLong()) {
                    map.put(entry.getKey(), entry.getValue().longValue());
                } else if (entry.getValue().isDouble()) {
                    map.put(entry.getKey(), entry.getValue().doubleValue());
                } else if (entry.getValue().isBoolean()) {
                    map.put(entry.getKey(), entry.getValue().booleanValue());
                } else {
                    map.put(entry.getKey(), entry.getValue().asText());
                }
            }
        });
        return map;
    }
}
