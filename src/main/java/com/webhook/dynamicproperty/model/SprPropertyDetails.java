package com.webhook.dynamicproperty.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Document(collection = "sprPropertyDetails")
public class SprPropertyDetails
{
    
    @Id
    private String id;
    @Indexed(unique = true)
    private String key;
    private String value;
    private boolean isSecure;
    private String _class;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;
    
}
