package com.webhook.dynamicproperty.service;

import com.webhook.dynamicproperty.config.MongoConfig;
import com.webhook.dynamicproperty.model.SprinklrProperty;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PropertyServiceImpl implements PropertyService {

    private static final Logger logger = LoggerFactory.getLogger(GithubService.class);
    
    @Autowired
    private  MongoConfig mongoConfig;

    @Override
    public Boolean save(SprinklrProperty property, String collectionName, String uniqueFieldName,String uniqueField) 
    {
        try
        {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase();
            findAndModify(mongoTemplate, property, collectionName, uniqueFieldName, property.getModifiedDateTime(),uniqueField);
        }
        catch(Exception e)
        {
            logger.error("Error while saving property in collection: " + collectionName, e);
            return false;
        }
        
        return true;
    }
    @Override
    public Boolean save(SprinklrProperty property, String collectionName, List<String> uniqueFieldNames,List<String> uniqueFields) {
        try 
        {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase();
            findAndModify(mongoTemplate, property, collectionName, uniqueFieldNames, property.getModifiedDateTime(),uniqueFields);
        } 
        catch (Exception e) 
        {
            logger.error("Error while saving property in collection: " + collectionName, e);
            return false;
        }
        return true;
        
    }

    private void findAndModify(MongoTemplate mongoTemplate, SprinklrProperty property, String collectionName, String uniqueFieldName, LocalDateTime modifiedDateTime,String uniqueField) 
    {
        Query query = new Query();

        Criteria criteria = Criteria.where(uniqueFieldName).is(uniqueField);
        query.addCriteria(criteria);

        Update update = property.createUpdateFromPropertyOninsert(modifiedDateTime);
       
        SprinklrProperty existingProperty=mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(true), property.getClass(), collectionName);

        if(existingProperty!=null)
        {
            query = new Query();
            criteria= new Criteria();
            criteria= criteria.and(uniqueFieldName).is(uniqueField);
            criteria= criteria.and("modifiedDateTime").lt(modifiedDateTime);
            query.addCriteria(criteria);

            update = property.createUpdateFromProperty();

            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(false), property.getClass(), collectionName);
        }
    }

    private void findAndModify(MongoTemplate mongoTemplate, SprinklrProperty property, String collectionName, List<String> uniqueFieldNames, LocalDateTime modifiedDateTime,List<String> uniqueFields) 
    {
        Query query = new Query();
        Criteria criteria = new Criteria();
        for(int i=0;i<uniqueFieldNames.size();i++)
        {
            criteria = criteria.and(uniqueFieldNames.get(i)).is(uniqueFields.get(i));
        }
        query.addCriteria(criteria);

        Update update = property.createUpdateFromPropertyOninsert(modifiedDateTime);

        SprinklrProperty existingProperty=mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(true), property.getClass(), collectionName);

        if(existingProperty!=null)
        {
            query = new Query();
            criteria = new Criteria();
            for(int i=0;i<uniqueFieldNames.size();i++)
            {
                criteria = criteria.and(uniqueFieldNames.get(i)).is(uniqueFields.get(i));
            }
            criteria = criteria.and("modifiedDateTime").lt(modifiedDateTime);
            query.addCriteria(criteria);

            update = property.createUpdateFromProperty();

            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().upsert(false), property.getClass(), collectionName);   
        }
       
    }

    
}
