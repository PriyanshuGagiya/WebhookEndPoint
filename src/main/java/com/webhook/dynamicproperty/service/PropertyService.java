package com.webhook.dynamicproperty.service;

import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;

public interface PropertyService 
{
    public String saveProperty(DynamicPropertyDetails dynamicPropertyDetails, String databaseName, String collectionName, String uniqueFieldName);
    public String saveProperty(SprPropertyDetails sprPropertyDetails, String databaseName, String collectionName, String uniqueFieldName);
    public String saveProperty(ServerConfigDetails serverConfigDetails, String databaseName, String collectionName, String uniqueFieldName);
}
