package com.webhook.dynamicproperty.model;

import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "partnerLevelConfigDetails")
public class PartnerLevelConfigDetails 
{
    private String id;
    private Map<String, Object> config;
}
