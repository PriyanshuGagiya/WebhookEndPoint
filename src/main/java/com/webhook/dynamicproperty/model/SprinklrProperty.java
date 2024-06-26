package com.webhook.dynamicproperty.model;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.query.Update;

public interface SprinklrProperty 
{
    Update createUpdateFromPropertyOninsert(LocalDateTime createdDateTime);
    Update createUpdateFromProperty();
    LocalDateTime getModifiedDateTime();
}
