package com.webhook.dynamicproperty.service;

import com.webhook.dynamicproperty.config.MongoConfig;
import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;

@Service
public class PropertyServiceImplements implements PropertyService {

    private final MongoConfig mongoConfig;

    public PropertyServiceImplements(MongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }

    @Override
    public String saveProperty(DynamicPropertyDetails dynamicPropertyDetails, String databaseName, String collectionName, String uniqueFieldName) {
        return save(dynamicPropertyDetails, databaseName, collectionName, uniqueFieldName);
    }

    @Override
    public String saveProperty(ServerConfigDetails serverConfigDetails, String databaseName, String collectionName, String uniqueFieldName) {
        return save(serverConfigDetails, databaseName, collectionName, uniqueFieldName);
    }

    @Override
    public String saveProperty(SprPropertyDetails sprPropertyDetails, String databaseName, String collectionName, String uniqueFieldName) {
        return save(sprPropertyDetails, databaseName, collectionName, uniqueFieldName);
    }

    private String save(Object property, String databaseName, String collectionName, String uniqueFieldName) {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase(databaseName);
        upsertProperty(mongoTemplate, property, collectionName, uniqueFieldName, getModifiedDate(property));
        return "saved";
    }

    private <T> void upsertProperty(MongoTemplate mongoTemplate, T property, String collectionName, String uniqueFieldName, String modifiedDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where(uniqueFieldName).is(getUniqueFieldValue(property, uniqueFieldName)));

        Update update = new Update();
        for (Field field : property.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(property);
                if (value != null) {
                    update.set(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field value", e);
            }
        }

        if (mongoTemplate.findOne(query, property.getClass(), collectionName) == null) {
            update.set("createdDate", modifiedDate);
        }

        mongoTemplate.upsert(query, update, collectionName);
    }

    private <T> Object getUniqueFieldValue(T property, String uniqueFieldName) {
        try {
            Field field = property.getClass().getDeclaredField(uniqueFieldName);
            field.setAccessible(true);
            return field.get(property);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error getting unique field value", e);
        }
    }

    private <T> String getModifiedDate(T property) {
        try {
            Field field = property.getClass().getDeclaredField("modifiedDate");
            field.setAccessible(true);
            return (String) field.get(property);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error getting modified date", e);
        }
    }
}
 // package com.webhook.dynamicproperty.service;
    // import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
    // import com.webhook.dynamicproperty.model.ServerConfigDetails;
    // import com.webhook.dynamicproperty.model.SprPropertyDetails;

    // import org.springframework.beans.factory.annotation.Value;
    // import org.springframework.data.mongodb.core.MongoTemplate;
    // import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
    // import org.springframework.data.mongodb.core.query.Criteria;
    // import org.springframework.data.mongodb.core.query.Query;
    // import org.springframework.data.mongodb.core.query.Update;
    // import org.springframework.stereotype.Service;
    // import java.lang.reflect.Field;

    // @Service
    // public class PropertyServiceImplements implements PropertyService {

    //     @Value("${custom.mongodb.url}")
    //     private String mongoUrl;

        
    //     @Override
    //     public String saveProperty(DynamicPropertyDetails dynamicPropertyDetails, String databaseName, String collectionName, String uniqueFieldName) {
    //         MongoTemplate customMongoTemplate = getMongoTemplateForDatabase(databaseName);
    //         upsertProperty(customMongoTemplate, dynamicPropertyDetails, collectionName, uniqueFieldName,dynamicPropertyDetails.getModifiedDate());
    //         return "saved";
    //     }
        
    //     @Override
    //     public String saveProperty(ServerConfigDetails serverConfigDetails, String databaseName, String collectionName,String uniqueFieldName) {
    //         MongoTemplate customMongoTemplate = getMongoTemplateForDatabase(databaseName);
    //         upsertProperty(customMongoTemplate, serverConfigDetails, collectionName, uniqueFieldName,serverConfigDetails.getModifiedDate());
    //         return "saved";
    //     }
    
    //     @Override
    //     public String saveProperty(SprPropertyDetails sprPropertyDetails, String databaseName, String collectionName,String uniqueFieldName) {
    //         MongoTemplate customMongoTemplate = getMongoTemplateForDatabase(databaseName);
    //         upsertProperty(customMongoTemplate, sprPropertyDetails, collectionName, uniqueFieldName,sprPropertyDetails.getModifiedDate());
    //         return "saved";
    //     }

    //     private MongoTemplate getMongoTemplateForDatabase(String databaseName) {
    //         return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoUrl + databaseName));
    //     }
    
    //     private <T> void upsertProperty(MongoTemplate mongoTemplate, T property, String collectionName, String uniqueFieldName,String modifiedDate) {
    //         Query query = new Query();
    //         query.addCriteria(Criteria.where(uniqueFieldName).is(getUniqueFieldValue(property, uniqueFieldName)));

    //         Update update = new Update();
    //         for (Field field : property.getClass().getDeclaredFields()) {
    //             field.setAccessible(true);
    //             try {
    //                 Object value = field.get(property);
    //                 if (value != null) {
    //                     update.set(field.getName(), value);
    //                 }
    //             } catch (IllegalAccessException e) {
    //                 throw new RuntimeException("Error accessing field value", e);
    //             }
    //         }
        
    //         if (mongoTemplate.findOne(query, property.getClass(), collectionName) == null) {
    //             update.set("createdDate", modifiedDate);
    //         }

    //         mongoTemplate.upsert(query, update, collectionName);
    //     }

    //     private <T> Object getUniqueFieldValue(T property, String uniqueFieldName) {
    //         try {
    //             Field field = property.getClass().getDeclaredField(uniqueFieldName);
    //             field.setAccessible(true);
    //             return field.get(property);
    //         } catch (NoSuchFieldException | IllegalAccessException e) {
    //             throw new RuntimeException("Error getting unique field value", e);
    //         }
    //     }
    // }