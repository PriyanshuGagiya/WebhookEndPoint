package com.webhook.dynamicproperty.model;

import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Document(collection = "partnerLevelConfigDetails")
public class PartnerLevelConfigBeanDetails 
{
    @Id
    private String id;
    private Map<String, Object> config;
    private String _class;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;
}
