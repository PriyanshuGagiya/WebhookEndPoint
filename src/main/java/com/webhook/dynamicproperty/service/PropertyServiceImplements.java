package com.webhook.dynamicproperty.service;

import com.webhook.dynamicproperty.config.MongoConfig;
import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;
import com.webhook.dynamicproperty.model.SprinklrProperty;
import com.webhook.dynamicproperty.model.PartnerLevelConfigBeanDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PropertyServiceImplements implements PropertyService {

    private final MongoConfig mongoConfig;

    public PropertyServiceImplements(MongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }

    @Override
    public String saveProperty(DynamicPropertyDetails dynamicPropertyDetails, String collectionName, String uniqueFieldName, String uniqueField) {
        return save(dynamicPropertyDetails, collectionName, uniqueFieldName, uniqueField);
    }

    @Override
    public String saveProperty(ServerConfigDetails serverConfigDetails, String collectionName, String uniqueFieldName, String uniqueField) {
        return save(serverConfigDetails, collectionName, uniqueFieldName, uniqueField);
    }

    @Override
    public String saveProperty(SprPropertyDetails sprPropertyDetails, String collectionName, String uniqueFieldName, String uniqueField) {
        return save(sprPropertyDetails, collectionName, uniqueFieldName, uniqueField);
    }

    @Override
    public String saveProperty(PartnerLevelConfigBeanDetails partnerLevelConfigBeanDetails, String collectionName, List<String> uniqueFieldNames, List<String> uniqueFields) {
        return save(partnerLevelConfigBeanDetails, collectionName, uniqueFieldNames, uniqueFields);
    }

    private String save(SprinklrProperty property, String collectionName, String uniqueFieldName,String uniqueField) {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase();
        findAndModify(mongoTemplate, property, collectionName, uniqueFieldName, property.getModifiedDateTime(),uniqueField);
        
        return "saved";
    }

    private String save(SprinklrProperty property, String collectionName, List<String> uniqueFieldNames,List<String> uniqueFields) {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase();
        findAndModify(mongoTemplate, property, collectionName, uniqueFieldNames, property.getModifiedDateTime(),uniqueFields);
        return "saved";
    }

    private void findAndModify(MongoTemplate mongoTemplate, SprinklrProperty property, String collectionName, String uniqueFieldName, LocalDateTime modifiedDate,String uniqueField) 
    {
        Query query = new Query();
        Criteria criteria = Criteria.where(uniqueFieldName).is(uniqueField);
        query.addCriteria(criteria);
        Update update = property.createUpdateFromPropertyOninsert(modifiedDate);
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(true), property.getClass(), collectionName);
        
        query = new Query();
        criteria= new Criteria();
        criteria= criteria.and(uniqueFieldName).is(uniqueField);
        criteria= criteria.and("modifiedDate").lt(modifiedDate);
        query.addCriteria(criteria);
        update = property.createUpdateFromProperty();
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(false), property.getClass(), collectionName);
    }

    private void findAndModify(MongoTemplate mongoTemplate, SprinklrProperty property, String collectionName, List<String> uniqueFieldNames, LocalDateTime modifiedDate,List<String> uniqueFields) 
    {
        Query query = new Query();
        Criteria criteria = new Criteria();
        for(int i=0;i<uniqueFieldNames.size();i++)
        {
            criteria = criteria.and(uniqueFieldNames.get(i)).is(uniqueFields.get(i));
        }
        query.addCriteria(criteria);
        Update update = property.createUpdateFromPropertyOninsert(modifiedDate);
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(true), property.getClass(), collectionName);

        query = new Query();
        criteria = new Criteria();
        for(int i=0;i<uniqueFieldNames.size();i++)
        {
            criteria = criteria.and(uniqueFieldNames.get(i)).is(uniqueFields.get(i));
        }
        criteria = criteria.and("modifiedDate").lt(modifiedDate);
        query.addCriteria(criteria);
        update = property.createUpdateFromProperty();
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(false), property.getClass(), collectionName);   
       
    }

    
}
