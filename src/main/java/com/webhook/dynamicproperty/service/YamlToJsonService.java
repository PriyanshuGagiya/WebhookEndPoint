package com.webhook.dynamicproperty.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;

@Service
public class YamlToJsonService {

    public JsonNode convertYamlToJson(String yamlContent) throws Exception {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yamlContent, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.convertValue(obj, JsonNode.class);
    }
}
