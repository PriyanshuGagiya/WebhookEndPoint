package com.webhook.dynamicproperty.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;


import lombok.Data;


@Data
@Document(collection = "dynamicPropertyDetails")
public class DynamicPropertyDetails implements SprinklrProperty
{
    @Id
    private String id;
    @Indexed(unique = true)
    private String key;
    private String property;
    private String value;
    private String reason;
    private boolean deleted;
    @CreatedDate
    private LocalDateTime createdDateTime;
    @LastModifiedDate
    private LocalDateTime modifiedDateTime;

    @Override
    public Update createUpdateFromPropertyOninsert(LocalDateTime createdDateTime)
    {
        Update update = new Update();
        update.setOnInsert("key", key);
        update.setOnInsert("property", property);
        update.setOnInsert("value", value);
        update.setOnInsert("reason", reason);
        update.setOnInsert("deleted", deleted);
        update.setOnInsert("createdDateTime", createdDateTime);
        update.setOnInsert("modifiedDateTime", createdDateTime);
        return update;
    }

    @Override
    public Update createUpdateFromProperty()
    {
        Update update = new Update();
        update.set("key", key);
        update.set("property", property);
        update.set("value", value);
        update.set("reason", reason);
        update.set("deleted", deleted);
        update.set("modifiedDateTime", modifiedDateTime);
        return update;
    }

    @Override
    public LocalDateTime getModifiedDateTime()
    {
        return modifiedDateTime;
    }
    
}
