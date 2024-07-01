package com.webhook.dynamicproperty.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.service.GithubService;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class JobCleanerGithub {

    private static final Logger logger = LoggerFactory.getLogger(JobCleanerGithub.class);

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.api.time.url}")
    private String githubApi;

    @Value("${github.api.commitdetailsapiurl}")
    private String commitDetailsApi;

    @Autowired
    private  RestTemplate restTemplate;
    @Autowired
    private GithubService githubService;
    private LocalDateTime prev;

    public JobCleanerGithub() {
        prev = LocalDateTime.now(ZoneOffset.UTC);
    }

    @Scheduled(fixedRate = 30000)
    public void robustnessCheck() {
        
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String formattedPrev = formatDateTime(prev);
        String formattedNow = formatDateTime(now);
        String completeGithubApi = githubApi + formattedPrev + "&until=" + formattedNow + "&sha=main";
       // System.out.println(completeGithubApi);
        logger.info("Fetching commits from {} to {}", formattedPrev, formattedNow);
        List<JsonNode> commits = fetchCommits(completeGithubApi);
        
        if (commits != null) {
            for (JsonNode commit : commits) {
                processCommit(commit);
            }
        }

        prev = now;
    }

    private void processCommit(JsonNode commitNode) {
        String commitSha = commitNode.path("sha").asText();
        OffsetDateTime commitDateTime = OffsetDateTime.parse(
                commitNode.path("commit").path("author").path("date").asText(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDateTime localCommitDateTime = commitDateTime.toLocalDateTime().plusHours(5).plusMinutes(30);

        if (githubService.containsCommit(commitSha)) {
            githubService.removeCommit(commitSha);
            return;
        }

        JsonNode commitDetails = fetchCommitDetails(commitSha);
        if (commitDetails == null) {
            System.out.println("Commit details are null");
            return;
        }
        JsonNode Files=commitDetails.get("files");
        for(JsonNode file : Files)
        {
            boolean isRemoved=file.get("status").asText().equals("removed");
            String filename=file.get("filename").asText();
            if(isRemoved)
            {
                String raw_url=file.get("raw_url").asText();
                String[] parts=raw_url.split("/");
                String sha=parts[parts.length-2];
                githubService.processFiles(filename, sha, localCommitDateTime, isRemoved);
            }
            else
            {
                 githubService.processFiles(filename,commitSha,localCommitDateTime,isRemoved);
            }
            
        }

    }

    private List<JsonNode> fetchCommits(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<JsonNode>>() {});

            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching commits from GitHub API: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode fetchCommitDetails(String sha) {
        String url = commitDetailsApi + sha;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);


        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class);

            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching commit details for {} from GitHub API: {}", sha, e.getMessage());
            return null;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.toString() + "Z";
    }
}
