package com.webhook.dynamicproperty.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;


import lombok.Data;


@Data
@Document(collection = "dynamicPropertyDetails")
public class DynamicPropertyDetails 
{
    @Id
    private String id;
    private String authorName;
    private String authorEmail;
    @Indexed(unique = true)
    private String key;
    private String property;
    private String value;
    private String reason;
    private boolean deleted;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;
    
}
