package com.webhook.dynamicproperty.model;

import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "partnerLevelConfigDetails")
public class PartnerLevelConfigBeanDetails 
{
    @Id
    private String id;
    private String authorName;
    private String authorEmail;
    private Map<String, Object> config;
    private String _class;
    @CreatedDate
    private String createdDate;
    @LastModifiedDate
    private String modifiedDate;
}
