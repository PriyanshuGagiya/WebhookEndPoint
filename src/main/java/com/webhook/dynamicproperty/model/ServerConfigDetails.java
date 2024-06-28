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
@Document(collection = "serverConfigDetails")
public class ServerConfigDetails implements SprinklrProperty
{
    @Id
    private String id;
    private String dbName;
    private String url;
    private long partnerId;
    private long clientId;
    private String serverCategory;
    private String serverType;
    private boolean deleted;
    @Indexed(unique = true)
    private String name;
    private String _class;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Override
    public
    Update createUpdateFromPropertyOninsert(LocalDateTime createdDateTime)
    {
        Update update = new Update();
        update.setOnInsert("dbName", dbName);
        update.setOnInsert("url", url);
        update.setOnInsert("partnerId", partnerId);
        update.setOnInsert("clientId", clientId);
        update.setOnInsert("serverCategory", serverCategory);
        update.setOnInsert("serverType", serverType);
        update.setOnInsert("name", name);
        update.setOnInsert("_class", _class);
        update.setOnInsert("createdDate", createdDateTime);
        update.setOnInsert("modifiedDate", createdDateTime);
        update.setOnInsert("deleted", deleted);
        return update;
    }

    @Override
    public 
    Update createUpdateFromProperty()
    {
        Update update = new Update();
        update.set("dbName", dbName);
        update.set("url", url);
        update.set("partnerId", partnerId);
        update.set("clientId", clientId);
        update.set("serverCategory", serverCategory);
        update.set("serverType", serverType);
        update.set("name", name);
        update.set("_class", _class);
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

