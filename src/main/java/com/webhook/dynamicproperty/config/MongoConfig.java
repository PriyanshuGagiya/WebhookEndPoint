package com.webhook.dynamicproperty.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Value("${custom.mongodb.url}")
    private String mongoUrl;

    public MongoTemplate getMongoTemplateForDatabase() {
        ConnectionString connectionString = new ConnectionString(mongoUrl);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return new MongoTemplate(MongoClients.create(mongoClientSettings), "prod1"); // "prod1" is your database name
    }

    public String getMongoUrl() {
        return mongoUrl;
    }

}
