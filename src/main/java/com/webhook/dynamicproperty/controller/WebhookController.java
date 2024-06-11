package com.webhook.dynamicproperty.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;
import com.webhook.dynamicproperty.service.PropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final RestTemplate restTemplate;

    private final PropertyService propertyService;

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.api.url:https://raw.githubusercontent.com/PriyanshuGagiya/DP/}")
    private String githubDownloadUrl;

    public WebhookController(RestTemplate restTemplate, PropertyService propertyService) {
        this.restTemplate = restTemplate;
        this.propertyService = propertyService;
    }

    @PostMapping("/github")
    public void gitWebhook(@RequestBody JsonNode jsonNode) {
        try 
        {
            
          JsonNode commits = jsonNode.get("commits");
            for (JsonNode commit : commits) 
            {
                JsonNode added = commit.get("added");
                JsonNode modified = commit.get("modified");
                String commitId=commit.get("id").asText();
                String commitTime=commit.get("timestamp").asText();
                for (JsonNode file : added) 
                {
                    String filePath = file.asText();
                    String[] filesPathSplit=filePath.split("/");
                    if(filesPathSplit.length<4)
                    {
                        continue;
                    }
                    String databaseName=filesPathSplit[2];
                    String collectionName=filesPathSplit[1];
                    String url = githubDownloadUrl + commitId + "/" + filePath;
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + githubToken);
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                    String content=response.getBody();
                    
                    if(collectionName.equals("dynamicProperty"))
                    {
                        DynamicPropertyDetails dynamicPropertyDetails = new DynamicPropertyDetails();
                        dynamicPropertyDetails.setAuthorName(commit.get("author").get("name").asText());
                        dynamicPropertyDetails.setAuthorEmail(commit.get("author").get("email").asText());
                        dynamicPropertyDetails.setModifiedDate(commitTime);
                        String[] contentSplit=content.split("\n");
                        for(String line:contentSplit)
                        {
                            String[] keyValue=line.split(":");
                            if(keyValue.length==2)
                            {
                                String key=keyValue[0].trim();
                                String value=keyValue[1].trim();
                                if(key.equals("key"))
                                {
                                    dynamicPropertyDetails.setKey(value);
                                }
                                else if(key.equals("property"))
                                {
                                    dynamicPropertyDetails.setProperty(value);
                                }
                                else if(key.equals("value"))
                                {
                                    dynamicPropertyDetails.setValue(value);
                                }
                                else if(key.equals("reason"))
                                {
                                    dynamicPropertyDetails.setReason(value);
                                }
                                else if(key.equals("deleted"))
                                {
                                    dynamicPropertyDetails.setDeleted(Boolean.parseBoolean(value));
                                }
                            }
                        }
                        // System.out.println(dynamicPropertyDetails);
                        // System.out.println(databaseName);
                        // System.out.println(collectionName);
                       String res=propertyService.saveProperty(dynamicPropertyDetails, databaseName, collectionName, "key");
                          System.out.println(res);

                    }
                    else if(collectionName.equals("serverConfig"))
                    {
                        ServerConfigDetails serverConfigDetails = new ServerConfigDetails();
                        serverConfigDetails.setAuthorName(commit.get("author").get("name").asText());
                        serverConfigDetails.setAuthorEmail(commit.get("author").get("email").asText());
                        
                        serverConfigDetails.setModifiedDate(commitTime);
                        String[] contentSplit=content.split("\n");
                        for(String line:contentSplit)
                        {
                            String[] keyValue=line.split(":");
                            if(keyValue.length==2)
                            {
                                String key=keyValue[0].trim();
                                String value=keyValue[1].trim();
                                if(key.equals("dbName"))
                                {
                                    serverConfigDetails.setDbName(value);
                                }
                                else if(key.equals("url"))
                                {
                                    serverConfigDetails.setUrl(value);
                                }
                                else if(key.equals("partnerId"))
                                {
                                    serverConfigDetails.setPartnerId(Long.parseLong(value));
                                }
                                else if(key.equals("clientId"))
                                {
                                    serverConfigDetails.setClientId(Long.parseLong(value));
                                }
                                else if(key.equals("serverCategory"))
                                {
                                    serverConfigDetails.setServerCategory(value);
                                }
                                else if(key.equals("serverType"))
                                {
                                    serverConfigDetails.setServerType(value);
                                }
                                else if(key.equals("name"))
                                {
                                    serverConfigDetails.setName(value);
                                }
                                else if(key.equals("_class"))
                                {
                                    serverConfigDetails.set_class(value);
                                }
                            }
                        }
                        // System.out.println(serverConfigDetails);
                        // System.out.println(databaseName);
                        // System.out.println(collectionName);
                        String res=propertyService.saveProperty(serverConfigDetails, databaseName, collectionName, "name");
                        System.out.println(res);

                        
                    }
                    else if(collectionName.equals("sprProperty"))
                    {
                        SprPropertyDetails sprPropertyDetails = new SprPropertyDetails();
                        sprPropertyDetails.setAuthorName(commit.get("author").get("name").asText());
                        sprPropertyDetails.setAuthorEmail(commit.get("author").get("email").asText());
                       
                        sprPropertyDetails.setModifiedDate(commitTime);
                        String[] contentSplit=content.split("\n");
                        for(String line:contentSplit)
                        {
                            String[] keyValue=line.split(":");
                            if(keyValue.length==2)
                            {
                                String key=keyValue[0].trim();
                                String value=keyValue[1].trim();
                                if(key.equals("key"))
                                {
                                    sprPropertyDetails.setKey(value);
                                }
                                else if(key.equals("value"))
                                {
                                    sprPropertyDetails.setValue(value);
                                }
                                else if(key.equals("isSecure"))
                                {
                                    sprPropertyDetails.setSecure(Boolean.parseBoolean(value));
                                }
                                else if(key.equals("_class"))
                                {
                                    sprPropertyDetails.set_class(value);
                                }
                            }
                        }
                       
                        
                       String res=propertyService.saveProperty(sprPropertyDetails, databaseName, collectionName, "key");
                          System.out.println(res);

                        
                    }
                     // else if(collectionName.equals("partnerLevelConfig"))
                        // {
                            
                        //     PartnerLevelConfigDetails partnerLevelConfigDetails = new PartnerLevelConfigDetails();
                        //     partnerLevelConfigDetails.setAuthorName(commit.get("author").get("name").asText());
                        //     partnerLevelConfigDetails.setAuthorEmail(commit.get("author").get("email").asText());
                        //     partnerLevelConfigDetails.setModifiedDate(commitTime);
                        //     String[] contentSplit=content.split("\n");
                            
                        //     for(String line:contentSplit)
                        //     {
                        //         String[] keyValue=line.split(":");
                        //         if(keyValue.length==2)
                        //         {
                        //             String key=keyValue[0].trim();
                        //             String value=keyValue[1].trim();
                        //             if(key.equals("config"))
                        //             {
                        //                 partnerLevelConfigDetails.setConfig(value);
                        //             }
                        //         }
                        //     }
                        //     String res=propertyService.saveProperty(partnerLevelConfigDetails, databaseName, collectionName, "config");
                        //     System.out.println(res);
                        // }

                }
                for (JsonNode file : modified) 
                {
                    String filePath = file.asText();
                    String[] filesPathSplit=filePath.split("/");
                    if(filesPathSplit.length<4)
                    {
                        continue;
                    }
                    String databaseName=filesPathSplit[2];
                    String collectionName=filesPathSplit[1];
                    String url = githubDownloadUrl + commitId + "/" + filePath;
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + githubToken);
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                    String content=response.getBody();
                    
                    if(collectionName.equals("dynamicProperty"))
                    {
                        DynamicPropertyDetails dynamicPropertyDetails = new DynamicPropertyDetails();
                        dynamicPropertyDetails.setAuthorName(commit.get("author").get("name").asText());
                        dynamicPropertyDetails.setAuthorEmail(commit.get("author").get("email").asText());
                        dynamicPropertyDetails.setModifiedDate(commitTime);
                        String[] contentSplit=content.split("\n");
                        for(String line:contentSplit)
                        {
                            String[] keyValue=line.split(":");
                            if(keyValue.length==2)
                            {
                                String key=keyValue[0].trim();
                                String value=keyValue[1].trim();
                                if(key.equals("key"))
                                {
                                    dynamicPropertyDetails.setKey(value);
                                }
                                else if(key.equals("property"))
                                {
                                    dynamicPropertyDetails.setProperty(value);
                                }
                                else if(key.equals("value"))
                                {
                                    dynamicPropertyDetails.setValue(value);
                                }
                                else if(key.equals("reason"))
                                {
                                    dynamicPropertyDetails.setReason(value);
                                }
                                else if(key.equals("deleted"))
                                {
                                    dynamicPropertyDetails.setDeleted(Boolean.parseBoolean(value));
                                }
                            }
                        }
                        // System.out.println(dynamicPropertyDetails);
                        // System.out.println(databaseName);
                        // System.out.println(collectionName);
                       String res=propertyService.saveProperty(dynamicPropertyDetails, databaseName, collectionName, "key");
                          System.out.println(res);

                    }
                    else if(collectionName.equals("serverConfig"))
                    {
                        ServerConfigDetails serverConfigDetails = new ServerConfigDetails();
                        serverConfigDetails.setAuthorName(commit.get("author").get("name").asText());
                        serverConfigDetails.setAuthorEmail(commit.get("author").get("email").asText());
                        
                        serverConfigDetails.setModifiedDate(commitTime);
                        String[] contentSplit=content.split("\n");
                        for(String line:contentSplit)
                        {
                            String[] keyValue=line.split(":");
                            if(keyValue.length==2)
                            {
                                String key=keyValue[0].trim();
                                String value=keyValue[1].trim();
                                if(key.equals("dbName"))
                                {
                                    serverConfigDetails.setDbName(value);
                                }
                                else if(key.equals("url"))
                                {
                                    serverConfigDetails.setUrl(value);
                                }
                                else if(key.equals("partnerId"))
                                {
                                    serverConfigDetails.setPartnerId(Long.parseLong(value));
                                }
                                else if(key.equals("clientId"))
                                {
                                    serverConfigDetails.setClientId(Long.parseLong(value));
                                }
                                else if(key.equals("serverCategory"))
                                {
                                    serverConfigDetails.setServerCategory(value);
                                }
                                else if(key.equals("serverType"))
                                {
                                    serverConfigDetails.setServerType(value);
                                }
                                else if(key.equals("name"))
                                {
                                    serverConfigDetails.setName(value);
                                }
                                else if(key.equals("_class"))
                                {
                                    serverConfigDetails.set_class(value);
                                }
                            }
                        }
                        // System.out.println(serverConfigDetails);
                        // System.out.println(databaseName);
                        // System.out.println(collectionName);
                        String res=propertyService.saveProperty(serverConfigDetails, databaseName, collectionName, "name");
                        System.out.println(res);

                        
                    }
                    else if(collectionName.equals("sprProperty"))
                    {
                        SprPropertyDetails sprPropertyDetails = new SprPropertyDetails();
                        sprPropertyDetails.setAuthorName(commit.get("author").get("name").asText());
                        sprPropertyDetails.setAuthorEmail(commit.get("author").get("email").asText());
                       
                        sprPropertyDetails.setModifiedDate(commitTime);
                        String[] contentSplit=content.split("\n");
                        for(String line:contentSplit)
                        {
                            String[] keyValue=line.split(":");
                            if(keyValue.length==2)
                            {
                                String key=keyValue[0].trim();
                                String value=keyValue[1].trim();
                                if(key.equals("key"))
                                {
                                    sprPropertyDetails.setKey(value);
                                }
                                else if(key.equals("value"))
                                {
                                    sprPropertyDetails.setValue(value);
                                }
                                else if(key.equals("isSecure"))
                                {
                                    sprPropertyDetails.setSecure(Boolean.parseBoolean(value));
                                }
                                else if(key.equals("_class"))
                                {
                                    sprPropertyDetails.set_class(value);
                                }
                            }
                        }
                        // System.out.println(sprPropertyDetails);
                        // System.out.println(databaseName);
                        // System.out.println(collectionName);
                       String res=propertyService.saveProperty(sprPropertyDetails, databaseName, collectionName, "key");
                          System.out.println(res);

                        
                    }
                    

                    
                }
            }
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook payload", e);
        }
    }
    public void processfile(JsonNode file)
    {

    }

    

    
}

