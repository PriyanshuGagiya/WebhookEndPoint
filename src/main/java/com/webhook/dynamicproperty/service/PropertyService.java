package com.webhook.dynamicproperty.service;

import java.util.List;


import com.webhook.dynamicproperty.model.SprinklrProperty;

public interface PropertyService 
{
    Boolean save(SprinklrProperty property, String collectionName, String uniqueFieldName,String uniqueField);
    Boolean save(SprinklrProperty property, String collectionName, List<String> uniqueFieldNames,List<String> uniqueFields);
}
