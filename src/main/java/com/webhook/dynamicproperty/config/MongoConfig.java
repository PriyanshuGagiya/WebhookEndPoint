package com.webhook.dynamicproperty.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;


@Configuration
public class MongoConfig {

    @Value("${custom.mongodb.url}")
    private String mongoUrl;

    public MongoTemplate getMongoTemplateForDatabase()
    {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoUrl));
    }
}
