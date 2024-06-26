package com.webhook.dynamicproperty.service;

import java.util.List;

import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.PartnerLevelConfigBeanDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;

public interface PropertyService 
{
    public String saveProperty(DynamicPropertyDetails dynamicPropertyDetails,  String collectionName, String uniqueFieldName, String uniqueField);
    public String saveProperty(SprPropertyDetails sprPropertyDetails, String collectionName, String uniqueFieldName, String uniqueField);
    public String saveProperty(ServerConfigDetails serverConfigDetails,  String collectionName, String uniqueFieldName, String uniqueField);
    public String saveProperty(PartnerLevelConfigBeanDetails partnerLevelConfigBeanDetails, String collectionName, List<String> uniqueFieldNames, List<String> uniqueFields);
}
