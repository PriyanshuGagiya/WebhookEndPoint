package com.webhook.dynamicproperty.model;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;


@Data
@Document(collection = "serverConfigDetails")
public class ServerConfigDetails 
{
    @Id
    private String id;
    private String authorName;
    private String authorEmail;
    private String dbName;
    private String url;
    private long partnerId;
    private long clientId;
    private String serverCategory;
    private String serverType;
    @Indexed(unique = true)
    private String name;
    private String _class;
    @CreatedDate
    private String createdDate;
    @LastModifiedDate
    private String modifiedDate;
}

