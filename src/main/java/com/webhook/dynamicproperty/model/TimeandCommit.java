package com.webhook.dynamicproperty.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import lombok.Data;

@Data
@Document(collection = "timeandcommit")
public class TimeandCommit 
{
    @Id
    private String id;
    private String key;
    HashSet<String> commitProcessed;
    LocalDateTime dateTime;
    
}
