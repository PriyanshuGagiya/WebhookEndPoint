
package com.webhook.dynamicproperty.service;
import com.webhook.dynamicproperty.config.MongoConfig;
import com.webhook.dynamicproperty.model.DynamicPropertyDetails;
import com.webhook.dynamicproperty.model.ServerConfigDetails;
import com.webhook.dynamicproperty.model.SprPropertyDetails;
import com.webhook.dynamicproperty.model.PartnerLevelConfigBeanDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

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

    public String saveProperty(PartnerLevelConfigBeanDetails partnerLevelConfigBeanDetails, String databaseName, String collectionName, List<String> uniqueFieldNames) {
        return save(partnerLevelConfigBeanDetails, databaseName, collectionName, uniqueFieldNames);
    }

    private String save(Object property, String databaseName, String collectionName, String uniqueFieldName) {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase(databaseName);
        upsertProperty(mongoTemplate, property, collectionName, uniqueFieldName, getModifiedDate(property));
        return "saved";
    }

    private String save(Object property, String databaseName, String collectionName, List<String> uniqueFieldNames) {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForDatabase(databaseName);
        upsertProperty(mongoTemplate, property, collectionName, uniqueFieldNames, getModifiedDate(property));
        return "saved";
    }

    private <T> void upsertProperty(MongoTemplate mongoTemplate, T property, String collectionName, String uniqueFieldName, String modifiedDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where(uniqueFieldName).is(getUniqueFieldValue(property, uniqueFieldName)));

        T existingProperty = mongoTemplate.findOne(query, (Class<T>) property.getClass(), collectionName);

        Update update = createUpdateFromProperty(property);
        if (existingProperty == null) {
            // Set createdDate only when inserting a new document
            update.set("createdDate", modifiedDate);
        } else if (isModifiedDateGreater(modifiedDate, getModifiedDate(existingProperty))) {
            // Update modifiedDate only if the new one is greater
            update.set("modifiedDate", modifiedDate);
        }

        mongoTemplate.upsert(query, update, collectionName);
    }

    private <T> void upsertProperty(MongoTemplate mongoTemplate, T property, String collectionName, List<String> uniqueFieldNames, String modifiedDate) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        for (String uniqueFieldName : uniqueFieldNames) {
            criteria = criteria.and(uniqueFieldName).is(getUniqueFieldValue(property, uniqueFieldName));
        }
        query.addCriteria(criteria);

        T existingProperty = mongoTemplate.findOne(query, (Class<T>) property.getClass(), collectionName);

        Update update = createUpdateFromProperty(property);
        if (existingProperty == null) 
        {
            update.set("createdDate", modifiedDate);
        } else if (isModifiedDateGreater(modifiedDate, getModifiedDate(existingProperty))) 
        {
            update.set("modifiedDate", modifiedDate);
        }

        mongoTemplate.upsert(query, update, collectionName);
    }

    private <T> Update createUpdateFromProperty(T property) {
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
        return update;
    }

    private <T> Object getUniqueFieldValue(T property, String uniqueFieldName) {
        try {
            if (uniqueFieldName.startsWith("config.")) {
                String nestedFieldName = uniqueFieldName.substring("config.".length());
                return getNestedFieldValue(property, nestedFieldName);
            } else {
                Field field = property.getClass().getDeclaredField(uniqueFieldName);
                field.setAccessible(true);
                return field.get(property);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error getting unique field value", e);
        }
    }

    private <T> Object getNestedFieldValue(T property, String fieldName) {
        try {
            Field configField = property.getClass().getDeclaredField("config");
            configField.setAccessible(true);
            Map<String, Object> configMap = (Map<String, Object>) configField.get(property);
            return configMap.get(fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error getting nested field value", e);
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

    private boolean isModifiedDateGreater(String newModifiedDate, String existingModifiedDate) {
       return newModifiedDate.compareTo(existingModifiedDate) > 0;
        
    }
}
