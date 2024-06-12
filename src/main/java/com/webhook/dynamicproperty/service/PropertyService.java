package com.webhook.dynamicproperty.service;

import java.util.List;

import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.PartnerLevelConfigBeanDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;

public interface PropertyService 
{
    public String saveProperty(DynamicPropertyDetails dynamicPropertyDetails, String databaseName, String collectionName, String uniqueFieldName);
    public String saveProperty(SprPropertyDetails sprPropertyDetails, String databaseName, String collectionName, String uniqueFieldName);
    public String saveProperty(ServerConfigDetails serverConfigDetails, String databaseName, String collectionName, String uniqueFieldName);
    public String saveProperty(PartnerLevelConfigBeanDetails partnerLevelConfigBeanDetails, String databaseName, String collectionName, List<String> uniqueFieldNames);
}
