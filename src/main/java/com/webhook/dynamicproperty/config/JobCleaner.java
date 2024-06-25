package com.webhook.dynamicproperty.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.controller.WebhookController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// @Configuration
// @EnableScheduling
public class JobCleaner {

    @Value("${github.token}")
    private String githubToken;

    private final RestTemplate restTemplate;
    private final WebhookController webhookController;
    private LocalDateTime prev;

    @Value("${github.api.time.url}")
    private String githubApi;

    @Value("${github.api.commitdetailsapiurl}")
    private String commitDetailsApi ;

    public JobCleaner(RestTemplate restTemplate, WebhookController webhookController) {
        this.restTemplate = restTemplate;
        this.webhookController = webhookController;
        
        prev=LocalDateTime.now(ZoneOffset.UTC);
    }

    
    @Scheduled(fixedRate =  30*1000 )
    public void RobustnessCheck() {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        
        String formattedPrev = prev.toString()+"Z";
        String formattedNow = now.toString()+"Z";
      
        String completeGithubApi = githubApi + formattedPrev + "&until=" + formattedNow + "&sha=main";
       
        System.out.println(completeGithubApi);
        List<JsonNode> commits = FetchCommits(completeGithubApi);
        if (commits != null) {
            for (JsonNode commit : commits) {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(
                        commit.path("commit").path("author").path("date").asText(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
                localDateTime = localDateTime.plusHours(5).plusMinutes(30);
                System.out.println(localDateTime);
                String commitSha = commit.path("sha").asText();
                if (webhookController.CommitIds.contains(commitSha)) {
                    webhookController.CommitIds.remove(commitSha);
                    System.out.println("hell yeah");
                    continue;
                }
                JsonNode commitDetails = FetchCommitDetails(commitSha);
                
               processCommit(commitDetails, localDateTime);
            }
        }
        prev = now;
    }

    private void processCommit(JsonNode commit, LocalDateTime commitTime) {
        String commitId = commit.path("sha").asText();

        JsonNode files = commit.path("files");
        System.out.println(files);
        System.out.println(commitTime);
        if (files != null) {
            for (JsonNode file : files) {
                String status = file.path("status").asText();
                if ("added".equals(status) || "modified".equals(status)) {
                    webhookController.processFiles(
                            file,
                            commitId,
                            commitTime

                    );
                }
            }
        }
    }

    private List<JsonNode> FetchCommits(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<JsonNode>>() {
                });

        return response.getBody();
    }

    private JsonNode FetchCommitDetails(String sha) {
        String url = commitDetailsApi + sha;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class);

        return response.getBody();
    }
}
