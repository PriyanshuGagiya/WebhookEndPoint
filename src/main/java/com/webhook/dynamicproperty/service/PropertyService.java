package com.webhook.dynamicproperty.service;

import java.util.List;


import com.webhook.dynamicproperty.model.SprinklrProperty;

public interface PropertyService 
{
    String save(SprinklrProperty property, String collectionName, String uniqueFieldName,String uniqueField);
    String save(SprinklrProperty property, String collectionName, List<String> uniqueFieldNames,List<String> uniqueFields);
}
