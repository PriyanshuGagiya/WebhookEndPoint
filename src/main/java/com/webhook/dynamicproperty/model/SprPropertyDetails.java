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
@Document(collection = "sprPropertyDetails")
public class SprPropertyDetails implements SprinklrProperty
{
    
    @Id
    private String id;
    @Indexed(unique = true)
    private String key;
    private String value;
    private boolean isSecure;
    private String _class;
    private boolean deleted;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Override
    public Update createUpdateFromPropertyOninsert(LocalDateTime createdDateTime)
    {
        Update update = new Update();
        update.setOnInsert("key", key);
        update.setOnInsert("value", value);
        update.setOnInsert("isSecure", isSecure);
        update.setOnInsert("_class", _class);
        update.setOnInsert("createdDate", createdDateTime);
        update.setOnInsert("modifiedDate", createdDateTime);
        update.setOnInsert("deleted", deleted);
        return update;
    }

    @Override
    public Update createUpdateFromProperty()
    {
        Update update = new Update();
        update.set("key", key);
        update.set("value", value);
        update.set("isSecure", isSecure);
        update.set("modifiedDate", modifiedDate);
        update.set("deleted", deleted);
        return update;
    }
    @Override
    public LocalDateTime getModifiedDateTime()
    {
        return modifiedDate;
    }

}
