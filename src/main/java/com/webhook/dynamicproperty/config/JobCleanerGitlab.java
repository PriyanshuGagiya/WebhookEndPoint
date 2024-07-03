package com.webhook.dynamicproperty.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.model.TimeandCommit;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.query.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class JobCleanerGitlab {

    private static final Logger logger = LoggerFactory.getLogger(JobCleanerGitlab.class);

    @Value("${spring.profiles.active}")
    private String activeProfile;

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
    private final MongoConfig mongoConfig;
   
    public JobCleanerGitlab(MongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }

    @Scheduled(fixedRate = 25*1000)
    public void robustnessCheck() {
        prev=getprev();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String formattedPrev = formatDateTime(prev);
        String formattedNow = formatDateTime(now);
        String completeGitlabApi = gitlabApi + formattedPrev + "&until=" + formattedNow + "&sha=main";
        
        logger.info("Fetching commits from {} to {}", formattedPrev, formattedNow);
       
        List<JsonNode> commits = fetchCommits(completeGitlabApi);
        
        if (commits != null) {
            for (JsonNode commit : commits) {
                processCommit(commit);
            }
        }

        setprev(now);
    }

    private void processCommit(JsonNode commitNode) {
        String commitSha = commitNode.get("id").asText();

        OffsetDateTime commitDateTime = OffsetDateTime.parse(
                commitNode.get("committed_date").asText(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDateTime localCommitDateTime = commitDateTime.toLocalDateTime().plusHours(5).plusMinutes(30);

        if (containsCommit(commitSha)) {
            
            removeCommit(commitSha);
            return;
        }

        JsonNode commitDetails = fetchCommitDetails(commitSha);
        String prevsha=commitNode.get("parent_ids").get(0).asText();
       
        if (commitDetails == null) 
        {
            logger.error("Commit details are null");
            return;
        }
        JsonNode Files=commitDetails;
        for(JsonNode file : Files)
        {
            boolean deleted=file.get("deleted_file").asBoolean();
            String filename=file.get("new_path").asText();
            if(deleted)
            {
                
                gitlabService.processProperty(filename, prevsha, localCommitDateTime, deleted);
            }
            else
            {
                 gitlabService.processProperty(filename,commitSha,localCommitDateTime,deleted);
            }
            
        }

    }

    private List<JsonNode> fetchCommits(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(gitlabToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<JsonNode> allCommits = new ArrayList<>();
        try {
            while (url != null) {
                ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<JsonNode>>() {});

                if (response.getBody() != null) {
                    allCommits.addAll(response.getBody());
                }
                
                url = getNextPageUrl(response.getHeaders());
            }
        } catch (Exception e) {
            logger.error("Error fetching commits from GitLab API: {}", e.getMessage());
        }

        return allCommits;
    }

    private String getNextPageUrl(HttpHeaders headers) {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders != null && !linkHeaders.isEmpty()) {
            for (String linkHeader : linkHeaders) {
                String[] links = linkHeader.split(",");
                for (String link : links) {
                    String[] parts = link.split(";");
                    if (parts.length > 1 && parts[1].contains("rel=\"next\"")) {
                        return parts[0].trim().replaceAll("<|>", "");
                    }
                }
            }
        }
        return null;
    }

    private JsonNode fetchCommitDetails(String sha) {
    
        String url = commitDetailsApi + sha+"/diff";
       
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
     private void setprev(LocalDateTime prev)
    {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommit");
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(activeProfile));
        Update update = new Update();
        update.set("dateTime", prev);
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), TimeandCommit.class);
    }
    private Boolean containsCommit(String commitSha)
    {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommit");
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.andOperator(Criteria.where("key").is(activeProfile));
        TimeandCommit timeandCommit = mongoTemplate.findOne(query, TimeandCommit.class);
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
       
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommit");
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.andOperator(Criteria.where("key").is(activeProfile));
        query.addCriteria(criteria);
        Update update = new Update();
        update.pull("commitProcessed", commitSha);
        mongoTemplate.findAndModify(query, update, TimeandCommit.class);
    }
    private LocalDateTime getprev()
    {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase("timeAndCommit");
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(activeProfile));
        Update update = new Update();
        update.setOnInsert("key", activeProfile);
        update.setOnInsert("dateTime", LocalDateTime.now(ZoneOffset.UTC));
        TimeandCommit timeandCommit = mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), TimeandCommit.class);
        return timeandCommit.getDateTime();
    }

}
