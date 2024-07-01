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

    private HashSet<String> Commits = new HashSet<String>();
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
            String prevCommit=jsonNode.get("before").asText();
            for (JsonNode commit : commits) {
                String commitId = commit.get("id").asText();
                JsonNode addedFiles = commit.get("added");
                JsonNode modifiedFiles = commit.get("modified");
                JsonNode removedFiles = commit.get("removed");
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(commit.get("timestamp").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
                for(JsonNode file : addedFiles)
                {
                    processFiles(file.asText(), commitId, localDateTime,false);
                }
                for(JsonNode file : modifiedFiles)
                {
                    processFiles(file.asText(), commitId, localDateTime,false);
                }
                for(JsonNode file : removedFiles)
                {
                    processFiles(file.asText(), prevCommit, localDateTime,true);
                }
                Commits.add(commitId);
            }
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook payload", e);
        }
    }

    public void processFiles(String  filePath, String commitId, LocalDateTime commitTime,boolean isRemoved) {
      
            String[] filesPathSplit = filePath.split("/");
            if (filesPathSplit.length < 4) 
            {
                if (filesPathSplit.length == 3) 
                {
                    processGlobalFiles(filesPathSplit, commitId, commitTime,isRemoved);
                }
                return;
            }
            String databaseName = filesPathSplit[2];
            if (!databaseName.equals(activeProfile)) {
                return;
            }
            String collectionName = filesPathSplit[1];
            String url = githubDownloadUrl + commitId + "/" + filePath;

            JsonNode content = fetchFileContent(url);
            if (content == null) {
                return;
            }

            switch (collectionName) {
                case "dynamicProperty":
                    handleDynamicProperty(commitTime, collectionName, content,isRemoved);
                    break;
                case "serverConfig":
                    handleServerConfig(commitTime, collectionName, content,isRemoved);
                    break;
                case "sprProperty":
                    handleSprProperty(commitTime, collectionName, content,isRemoved);
                    break;
                case "partnerLevelConfigBean":
                    handlePartnerLevelConfigBean(commitTime, collectionName, content,isRemoved);
                    break;
                default:
                    logger.warn("Unknown collection: {}", collectionName);
            }
       
    }

    private void processGlobalFiles(String[] filesPathSplit, String commitId, LocalDateTime commitTime,boolean isRemoved) {
        String collectionName = filesPathSplit[1];
        String url = githubDownloadUrl + commitId + "/" + filesPathSplit[0] + "/" + filesPathSplit[1] + "/" + filesPathSplit[2];
        JsonNode content = fetchFileContent(url);
        if (content == null) {
            return;
        }
        switch (collectionName) {
            case "dynamicProperty":
                handleDynamicProperty(commitTime, collectionName, content,isRemoved);
                break;
            case "serverConfig":
                handleServerConfig(commitTime, collectionName, content,isRemoved);
                break;
            case "sprProperty":
                handleSprProperty(commitTime, collectionName, content,isRemoved);
                break;
            case "partnerLevelConfigBean":
                handlePartnerLevelConfigBean(commitTime, collectionName, content,isRemoved);
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

    private void handleDynamicProperty(LocalDateTime commitTime, String collectionName, JsonNode content,boolean isRemoved) {
        DynamicPropertyDetails dynamicPropertyDetails = new DynamicPropertyDetails();
        dynamicPropertyDetails.setModifiedDateTime(commitTime);
        dynamicPropertyDetails.setKey(content.get("key").asText());
        dynamicPropertyDetails.setProperty(content.get("property").asText());
        dynamicPropertyDetails.setValue(content.get("value").asText());
        dynamicPropertyDetails.setReason(content.get("reason").asText());
        if(isRemoved)
        {
            dynamicPropertyDetails.setDeleted(true);
        }
        else
        {
            dynamicPropertyDetails.setDeleted(content.get("deleted").asBoolean());
        }
        propertyService.save(dynamicPropertyDetails, collectionName, "key", dynamicPropertyDetails.getKey());
    }

    private void handleServerConfig(LocalDateTime commitTime, String collectionName, JsonNode content,boolean isRemoved) {
        ServerConfigDetails serverConfigDetails = new ServerConfigDetails();
        serverConfigDetails.setModifiedDateTime(commitTime);
        serverConfigDetails.setDbName(content.get("dbName").asText());
        serverConfigDetails.setUrl(content.get("url").asText());
        serverConfigDetails.setPartnerId(content.get("partnerId").asLong());
        serverConfigDetails.setClientId(content.get("clientId").asLong());
        serverConfigDetails.setServerCategory(content.get("serverCategory").asText());
        serverConfigDetails.setServerType(content.get("serverType").asText());
        serverConfigDetails.setName(content.get("name").asText());
        serverConfigDetails.set_class(content.get("_class").asText());
        serverConfigDetails.setDeleted(isRemoved);
        propertyService.save(serverConfigDetails, collectionName, "name", serverConfigDetails.getName());
    }

    private void handleSprProperty(LocalDateTime commitTime, String collectionName, JsonNode content,boolean isRemoved) {
        SprPropertyDetails sprPropertyDetails = new SprPropertyDetails();
        sprPropertyDetails.setModifiedDateTime(commitTime);
        sprPropertyDetails.setKey(content.get("key").asText());
        sprPropertyDetails.setValue(content.get("value").asText());
        sprPropertyDetails.setSecure(content.get("isSecure").asBoolean());
        sprPropertyDetails.set_class(content.get("_class").asText());
        sprPropertyDetails.setDeleted(isRemoved);
        propertyService.save(sprPropertyDetails, collectionName, "key", sprPropertyDetails.getKey());
    }

    private void handlePartnerLevelConfigBean(LocalDateTime commitTime, String collectionName, JsonNode content,boolean isRemoved) {
        PartnerLevelConfigBeanDetails partnerLevelConfigBean = new PartnerLevelConfigBeanDetails();
        partnerLevelConfigBean.setModifiedDateTime(commitTime);
        Map<String, Object> config = convertJsonNodeToMap(content.get("config"));
        partnerLevelConfigBean.setConfig(config);
        partnerLevelConfigBean.set_class(content.get("_class").asText());
        List<String> uniqueFieldNames = new ArrayList<>();
        uniqueFieldNames.add("config.module");
        uniqueFieldNames.add("config.type");
        uniqueFieldNames.add("config.configClassName");
        List<String> uniqueFields = new ArrayList<>();
        uniqueFields.add((String) config.get("module"));
        uniqueFields.add((String) config.get("type"));
        uniqueFields.add((String) config.get("configClassName"));
        partnerLevelConfigBean.setDeleted(isRemoved);
        propertyService.save(partnerLevelConfigBean, collectionName, uniqueFieldNames, uniqueFields);
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
    public boolean containsCommit(String commitId) {
        return Commits.contains(commitId);
    }
    public void removeCommit(String commitId) {
        Commits.remove(commitId);
    }
}
