package com.webhook.dynamicproperty.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MongoConfig {

    @Value("${custom.mongodb.url}")
    private String mongoUrl;

    private final ConcurrentHashMap<String, MongoTemplate> mongoTemplatesCache = new ConcurrentHashMap<>();

    public MongoTemplate getMongoTemplateForDatabase(String databaseName) {
        return mongoTemplatesCache.computeIfAbsent(databaseName, dbName ->
            new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoUrl + dbName)));
    }
}
