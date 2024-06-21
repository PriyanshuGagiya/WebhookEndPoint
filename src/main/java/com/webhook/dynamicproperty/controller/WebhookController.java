package com.webhook.dynamicproperty.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;
import com.webhook.dynamicproperty.service.PropertyService;
import com.webhook.dynamicproperty.service.YamlToJsonService;
import com.webhook.dynamicproperty.model.PartnerLevelConfigBeanDetails;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/webhook")
public class WebhookController 
{

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private YamlToJsonService yamlToJsonService;

    private final RestTemplate restTemplate;
    private final PropertyService propertyService;

    @Value("${spring.profiles.active}")
    String activeProfile;

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.api.url}")
    private String githubDownloadUrl;

    public WebhookController(RestTemplate restTemplate, PropertyService propertyService) {
        this.restTemplate = restTemplate;
        this.propertyService = propertyService;
    }

    @PostMapping("/github")
    public void gitWebhook(@RequestBody JsonNode jsonNode) {
        try {
            // System.out.println(jsonNode);
            JsonNode commits = jsonNode.get("commits");
            
            for (JsonNode commit : commits) {
                //System.out.println(commit.get("id").asText());
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(commit.get("timestamp").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
                processFiles(commit.get("author").get("name").asText(),commit.get("author").get("email").asText(), commit.get("added"), commit.get("id").asText(), localDateTime);
                processFiles(commit.get("author").get("name").asText(),commit.get("author").get("email").asText(), commit.get("modified"), commit.get("id").asText(),
                localDateTime);
            }
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook payload", e);
        }
    }

    private void processFiles(String AuthorName,String AuthorEmail, JsonNode files, String commitId,LocalDateTime commitTime) 
    {
        for (JsonNode file : files) 
        {
            String filePath = file.asText();
            String[] filesPathSplit = filePath.split("/");
            // charts/dynamicProperty/DP-1.yaml
            if (filesPathSplit.length < 4) 
            {
                if(filesPathSplit.length==3)
                {
                    processGlobalFiles(AuthorName,AuthorEmail, filesPathSplit, commitId, commitTime);
                }
                continue;
            }
            String databaseName = filesPathSplit[2];
            if(!databaseName.equals(activeProfile))
            {
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
                    handleDynamicProperty(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
                    break;
                case "serverConfig":
                    handleServerConfig(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
                    break;
                case "sprProperty":
                    handleSprProperty(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
                    break;
                case "partnerLevelConfigBean":
                    handlePartnerLevelConfigBean(AuthorName, AuthorEmail, commitTime, databaseName, collectionName,
                            content);
                    break;
                default:
                    logger.warn("Unknown collection: {}", collectionName);
            }
        }
    }
    private void processGlobalFiles(String AuthorName,String AuthorEmail, String[] filesPathSplit, String commitId, LocalDateTime commitTime) {

        String databaseName=activeProfile;
        String collectionName = filesPathSplit[1];
        String url = githubDownloadUrl + commitId + "/" + filesPathSplit[0] + "/" + filesPathSplit[1] + "/" + filesPathSplit[2];
        JsonNode content = fetchFileContent(url);
        if (content == null) {
            return;
        }
        switch (collectionName) {
            case "dynamicProperty":
                handleDynamicProperty(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
                break;
            case "serverConfig":
                handleServerConfig(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
                break;
            case "sprProperty":
                handleSprProperty(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
                break;
            case "partnerLevelConfigBean":
                handlePartnerLevelConfigBean(AuthorName, AuthorEmail, commitTime, databaseName, collectionName, content);
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

    private void handleDynamicProperty(String AuthorName, String AuthorEmail, LocalDateTime commitTime, String databaseName,
            String collectionName, JsonNode content) {
        DynamicPropertyDetails dynamicPropertyDetails = new DynamicPropertyDetails();
        dynamicPropertyDetails.setAuthorName(AuthorName);
        dynamicPropertyDetails.setAuthorEmail(AuthorEmail);
        
        dynamicPropertyDetails.setModifiedDate(commitTime); 
        dynamicPropertyDetails.setKey(content.get("key").asText());
        dynamicPropertyDetails.setProperty(content.get("property").asText());
        dynamicPropertyDetails.setValue(content.get("value").asText());
        dynamicPropertyDetails.setReason(content.get("reason").asText());
        dynamicPropertyDetails.setDeleted(content.get("deleted").asBoolean());
        propertyService.saveProperty(dynamicPropertyDetails, collectionName, "key");
    }

    private void handleServerConfig(String AuthorName, String AuthorEmail, LocalDateTime commitTime, String databaseName,
        String collectionName, JsonNode content) {
        ServerConfigDetails serverConfigDetails = new ServerConfigDetails();
        serverConfigDetails.setAuthorName(AuthorName);
        serverConfigDetails.setAuthorEmail(AuthorEmail);
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

    private void handleSprProperty(String AuthorName, String AuthorEmail, LocalDateTime commitTime, String databaseName,
        String collectionName, JsonNode content) {
        SprPropertyDetails sprPropertyDetails = new SprPropertyDetails();
        sprPropertyDetails.setAuthorName(AuthorName);
        sprPropertyDetails.setAuthorEmail(AuthorEmail);
        sprPropertyDetails.setModifiedDate(commitTime);
        sprPropertyDetails.setKey(content.get("key").asText());
        sprPropertyDetails.setValue(content.get("value").asText());
        sprPropertyDetails.setSecure(content.get("isSecure").asBoolean());
        sprPropertyDetails.set_class(content.get("_class").asText());

        propertyService.saveProperty(sprPropertyDetails, collectionName, "key");
    }

   
    private void handlePartnerLevelConfigBean(String authorName, String authorEmail, LocalDateTime commitTime, String databaseName, String collectionName, JsonNode content) {
        PartnerLevelConfigBeanDetails partnerLevelConfigBean = new PartnerLevelConfigBeanDetails();
        partnerLevelConfigBean.setAuthorName(authorName);
        partnerLevelConfigBean.setAuthorEmail(authorEmail);
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
            } else if (entry.getValue().isArray()) 
            {
                List<Object> list = new ArrayList<>();
                entry.getValue().forEach(item -> {
                    if (item.isObject()) {
                        list.add(convertJsonNodeToMap(item));
                    } else {
                        list.add(item.asText());
                    }
                });
                map.put(entry.getKey(), list);
            } 
            else 
            {
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
