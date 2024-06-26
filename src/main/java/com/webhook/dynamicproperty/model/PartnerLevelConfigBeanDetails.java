package com.webhook.dynamicproperty.model;

import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import lombok.Data;

@Data
@Document(collection = "partnerLevelConfigDetails")
public class PartnerLevelConfigBeanDetails implements SprinklrProperty
{
    @Id
    private String id;
    private Map<String, Object> config;
    private String _class;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Override
    public Update createUpdateFromPropertyOninsert(LocalDateTime createdDateTime)
    {
        Update update = new Update();
        update.setOnInsert("config", config);
        update.setOnInsert("_class", _class);
        update.setOnInsert("createdDate", createdDateTime);
        update.setOnInsert("modifiedDate", createdDateTime);
        return update;
    }

    @Override
    public Update createUpdateFromProperty()
    {
        Update update = new Update();
        update.set("config", config);
        update.set("_class", _class);
        update.set("modifiedDate", modifiedDate);
        return update;
    }
    @Override
    public LocalDateTime getModifiedDateTime()
    {
        return modifiedDate;
    }
    
}
