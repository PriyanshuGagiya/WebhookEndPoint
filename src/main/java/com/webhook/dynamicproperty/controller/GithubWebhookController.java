package com.webhook.dynamicproperty.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.webhook.dynamicproperty.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class GithubWebhookController {
    @Autowired
    private GithubService githubService;

    @PostMapping("/github")
    public void gitWebhook(@RequestBody JsonNode jsonNode) {
        githubService.processWebhookPayload(jsonNode);
    }
}
