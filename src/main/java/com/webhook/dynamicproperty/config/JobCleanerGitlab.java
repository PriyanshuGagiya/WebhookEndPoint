package com.webhook.dynamicproperty.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.service.GitlabService;

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
public class JobCleanerGitlab {

    private static final Logger logger = LoggerFactory.getLogger(JobCleanerGitlab.class);

    @Value("${gitlab.token}")
    private String gitlabToken;

    @Value("${gitlab.api.time.url}")
    private String gitlabApi;

    @Value("${gitlab.api.commitdetailsapiurl}")
    private String commitDetailsApi;

    @Autowired
    private  RestTemplate restTemplate;
    @Autowired
    private GitlabService gitlabService;
    private LocalDateTime prev;

    public JobCleanerGitlab() {
        prev = LocalDateTime.now(ZoneOffset.UTC);
    }

    @Scheduled(fixedRate = 30000)
    public void robustnessCheck() {
        
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String formattedPrev = formatDateTime(prev);
        String formattedNow = formatDateTime(now);
        String completeGitlabApi = gitlabApi + formattedPrev + "&until=" + formattedNow + "&sha=main";
        completeGitlabApi="https://gitlab.com/api/v4/projects/59307924/repository/commits?since=2024-07-01T06:37:38.093626Z&until=2024-07-01T06:38:08.110934Z&sha=main";
        logger.info("Fetching commits from {} to {}", formattedPrev, formattedNow);
        System.out.println("url is "+completeGitlabApi);
        List<JsonNode> commits = fetchCommits(completeGitlabApi);
        
        if (commits != null) {
            for (JsonNode commit : commits) {
                processCommit(commit);
            }
        }

        prev = now;
    }

    private void processCommit(JsonNode commitNode) {
        String commitSha = commitNode.get("id").asText();
        OffsetDateTime commitDateTime = OffsetDateTime.parse(
                commitNode.get("committed_date").asText(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDateTime localCommitDateTime = commitDateTime.toLocalDateTime().plusHours(5).plusMinutes(30);

        if (gitlabService.containsCommit(commitSha)) {
            gitlabService.removeCommit(commitSha);
            return;
        }
        JsonNode commitDetails = fetchCommitDetails(commitSha);
        String prevsha=commitNode.get("parent_ids").get(0).asText();
       // System.out.println("commit details are "+commitDetails);
        if (commitDetails == null) {
            System.out.println("Commit details are null");
            return;
        }
        JsonNode Files=commitDetails;
        for(JsonNode file : Files)
        {
            boolean isRemoved=file.get("deleted_file").asBoolean();
            String filename=file.get("new_path").asText();
            if(isRemoved)
            {
                
                gitlabService.processFiles(filename, prevsha, localCommitDateTime, isRemoved);
            }
            else
            {
                 gitlabService.processFiles(filename,commitSha,localCommitDateTime,isRemoved);
            }
            
        }

    }

    private List<JsonNode> fetchCommits(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(gitlabToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<JsonNode>>() {});

            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching commits from Gitlab API: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode fetchCommitDetails(String sha) {
    
        String url = commitDetailsApi + sha+"/diff";
        System.out.println("url is "+url);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(gitlabToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);


        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class);

            return response.getBody();
        } catch (Exception e) {
            
            logger.error("Error fetching commit details for {} from Gitlab API: {}", sha, e.getMessage());
            return null;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.toString() + "Z";
    }
}
