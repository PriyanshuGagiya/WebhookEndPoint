package com.webhook.dynamicproperty.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.model.TimeandCommit;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class JobCleanerGithub {

    private static final Logger logger = LoggerFactory.getLogger(JobCleanerGithub.class);

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.api.time.url}")
    private String githubApi;

    @Value("${github.api.commitdetailsapiurl}")
    private String commitDetailsApi;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private GithubService githubService;
    private LocalDateTime prev;
    private final MongoConfig mongoConfig;

    
    public JobCleanerGithub(MongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }
    
    @Scheduled(fixedRate = 60*1000)
    public void robustnessCheck() 
    {
        prev=getprev();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String formattedPrev = formatDateTime(prev);
        String formattedNow = formatDateTime(now);
        String completeGithubApi = githubApi + formattedPrev + "&until=" + formattedNow + "&sha=main";

        logger.info("Fetching commits from {} to {}", formattedPrev, formattedNow);

        List<JsonNode> commits = fetchCommits(completeGithubApi);

        if (commits != null) {
            for (JsonNode commit : commits) {
                processCommit(commit);
            }
        }

        setprev(now);
    }

    private void processCommit(JsonNode commitNode) {
        String commitSha = commitNode.path("sha").asText();

        OffsetDateTime commitDateTime = OffsetDateTime.parse(
                commitNode.path("commit").path("author").path("date").asText(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDateTime localCommitDateTime = commitDateTime.toLocalDateTime().plusHours(5).plusMinutes(30);


        if (containsCommit(commitSha)) {
            
            removeCommit(commitSha);
            return;
        }
        
        JsonNode commitDetails = fetchCommitDetails(commitSha);
        
        if (commitDetails == null) {
           logger.error("Error fetching commit details for {}", commitSha);
            return;
        }
        JsonNode files = commitDetails.get("files");
        for (JsonNode file : files) 
        {
            boolean deleted = file.get("status").asText().equals("removed");

            String filename = file.get("filename").asText();

            if (deleted) 
            {
                String rawUrl = file.get("raw_url").asText();
                String[] parts = rawUrl.split("/");
                String sha = parts[parts.length - 2];

                if(githubService.processProperty(filename, sha, localCommitDateTime, deleted)==false)
                {
                    logger.error("Error processing property for {}", filename);
                }
            } 
            else 
            {
                if(githubService.processProperty(filename, commitSha, localCommitDateTime, deleted)==false)
                {
                    logger.error("Error processing property for {}", filename);
                }
            }
        }
    }

    private List<JsonNode> fetchCommits(String url) 
    {
        List<JsonNode> allCommits = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        while (url != null) {
            try {
                ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<JsonNode>>() {
                        });

                List<JsonNode> commits = response.getBody();
                if (commits != null) {
                    allCommits.addAll(commits);
                }

                url = getNextPageUrl(response.getHeaders());
            } catch (Exception e) {
                logger.error("Error fetching commits from GitHub API: {}", e.getMessage());
                break;
            }
        }

        return allCommits;
    }

    private String getNextPageUrl(HttpHeaders headers) 
    {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return null;
        }

        for (String linkHeader : linkHeaders) {
            String[] links = linkHeader.split(", ");
            for (String link : links) {
                String[] parts = link.split("; ");
                if (parts.length == 2 && parts[1].equals("rel=\"next\"")) {
                    return parts[0].substring(1, parts[0].length() - 1);
                }
            }
        }

        return null;
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

    private void setprev(LocalDateTime prev)
    {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommitGithub");
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(activeProfile));
        Update update = new Update();
        update.set("dateTime", prev);
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), TimeandCommit.class);
    }
    private Boolean containsCommit(String commitSha)
    {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommitGithub");
        Query query = new Query();
        System.out.println("activeProfile"+activeProfile);
        query.addCriteria(Criteria.where("key").is(activeProfile));
        TimeandCommit timeandCommit = mongoTemplate.findOne(query, TimeandCommit.class);
        System.out.println("timeandCommit"+timeandCommit);
        if(timeandCommit==null)
        {
            return false;
        }
        if(timeandCommit.getCommitProcessed()!=null && timeandCommit.getCommitProcessed().contains(commitSha))
        {
            return true;
        }
        return false;
    }
    private void removeCommit(String commitSha)
    {
       
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommitGithub");
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(activeProfile));
        Update update = new Update();
        update.pull("commitProcessed", commitSha);
        mongoTemplate.findAndModify(query, update, TimeandCommit.class);
    }
    private LocalDateTime getprev()
    {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommitGithub");
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(activeProfile));
        Update update = new Update();
        update.setOnInsert("key", activeProfile);
        update.setOnInsert("dateTime", LocalDateTime.now(ZoneOffset.UTC));
        TimeandCommit timeandCommit = mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), TimeandCommit.class);
        return timeandCommit.getDateTime();
    }
}
