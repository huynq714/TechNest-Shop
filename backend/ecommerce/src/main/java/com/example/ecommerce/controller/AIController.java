package com.example.ecommerce.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.service.AIChatService;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173")
public class AIController {
    private final AIChatService aiChatService;

    public AIController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String message = body != null ? body.get("message") : null;
        String context = body != null ? body.get("context") : null;
        return Map.of("reply", aiChatService.chat(message, context));
    }
}
