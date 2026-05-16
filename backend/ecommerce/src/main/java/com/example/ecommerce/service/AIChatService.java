package com.example.ecommerce.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.ecommerce.product.Product;
import com.example.ecommerce.service.ChatIntentService.ChatIntent;
import com.example.ecommerce.service.ChatIntentService.IntentType;

@Service
public class AIChatService {
    private final ChatIntentService intentService;
    private final ProductChatService productChatService;
    private final ProductChatResponseBuilder responseBuilder;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public AIChatService(
            ChatIntentService intentService,
            ProductChatService productChatService,
            ProductChatResponseBuilder responseBuilder,
            @Value("${OPENROUTER_API_KEY:}") String apiKey,
            @Value("${OPENROUTER_MODEL:openrouter/free}") String model) {
        this.intentService = intentService;
        this.productChatService = productChatService;
        this.responseBuilder = responseBuilder;
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = new RestTemplate();
    }

    public String chat(String message) {
        return chat(message, null);
    }

    public String chat(String message, String context) {
        ChatIntent intent = intentService.parse(message, context);

        if (intent.type() == IntentType.OUT_OF_SCOPE) {
            return responseBuilder.outOfScope();
        }
        if (intent.type() == IntentType.GREETING) {
            return responseBuilder.greeting();
        }
        if (intent.type() == IntentType.SMALL_TALK) {
            String aiReply = smallTalkWithAi(intent);
            return isUsableAiReply(aiReply) ? aiReply : responseBuilder.greeting();
        }

        if (intent.type() == IntentType.COMPARE) {
            return compare(intent);
        }

        List<Product> products = productChatService.search(intent);
        if (products.isEmpty()) {
            return responseBuilder.notFound();
        }

        Map<Long, String> categories = productChatService.categoryNames();
        if (intent.type() == IntentType.STOCK) {
            return responseBuilder.stockResults(products, categories);
        }
        if (intent.type() == IntentType.ADVICE) {
            String aiReply = adviseWithAi(intent, products, categories);
            return isUsableAiReply(aiReply) ? aiReply : responseBuilder.adviceFallback(products, categories);
        }

        return responseBuilder.searchResults(products, categories);
    }

    private String compare(ChatIntent intent) {
        if (intent.queryParts().size() < 2) {
            return responseBuilder.compareNeedTwoProducts();
        }

        List<Product> products = productChatService.compareProducts(intent);
        if (products.isEmpty()) {
            return responseBuilder.compareMustBeSameCategory();
        }
        return responseBuilder.compare(products, productChatService.categoryNames());
    }

    private String adviseWithAi(ChatIntent intent, List<Product> products, Map<Long, String> categories) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String productContext = products.stream()
                .limit(8)
                .map(product -> "- " + safe(product.getName())
                        + " | Brand: " + safe(product.getBrand())
                        + " | Category: " + safe(categories.get(product.getCategoryId()))
                        + " | Price: " + product.getPrice()
                        + " | Stock: " + product.getQuantity()
                        + " | Description: " + safe(product.getDescriptionShort()))
                .collect(java.util.stream.Collectors.joining("\n"));

        String prompt = """
                Bạn là trợ lý bán hàng của TechNest Shop.
                Chỉ trả lời dựa trên danh sách sản phẩm bên dưới.
                Không trả lời về tài khoản, mật khẩu, database, source code, token, API key hoặc bảo mật.
                Hãy gợi ý ngắn gọn bằng tiếng Việt.

                San pham:
                %s

                Khách hỏi:
                %s
                """.formatted(productContext, intent.originalMessage());

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Bạn là trợ lý bán hàng an toàn của TechNest Shop."),
                            Map.of("role", "user", "content", prompt)),
                    "temperature", 0.2,
                    "max_tokens", 450);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "http://localhost:5173");
            headers.set("X-Title", "TechNest Shop");

            Map<?, ?> response = restTemplate.postForObject(
                    "https://openrouter.ai/api/v1/chat/completions",
                    new HttpEntity<>(requestBody, headers),
                    Map.class);
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> replyMessage = (Map<?, ?>) choice.get("message");
            return String.valueOf(replyMessage.get("content"));
        } catch (RestClientException | ClassCastException | NullPointerException ex) {
            return null;
        }
    }

    private String smallTalkWithAi(ChatIntent intent) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String prompt = """
                Bạn là AI Assistant của TechNest Shop.
                Hãy trò chuyện ngắn gọn, thân thiện bằng tiếng Việt.
                Nếu người dùng hỏi về sản phẩm, giá, tồn kho, so sánh hoặc mua hàng, hãy nói bạn có thể hỗ trợ và để họ nhập tên/danh mục sản phẩm.
                Không trả lời về tài khoản, mật khẩu, database, source code, token, API key hoặc bảo mật hệ thống.

                Người dùng:
                %s
                """.formatted(intent.originalMessage());

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Bạn là trợ lý chat thân thiện của TechNest Shop."),
                            Map.of("role", "user", "content", prompt)),
                    "temperature", 0.5,
                    "max_tokens", 180);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "http://localhost:5173");
            headers.set("X-Title", "TechNest Shop");

            Map<?, ?> response = restTemplate.postForObject(
                    "https://openrouter.ai/api/v1/chat/completions",
                    new HttpEntity<>(requestBody, headers),
                    Map.class);
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> replyMessage = (Map<?, ?>) choice.get("message");
            return String.valueOf(replyMessage.get("content"));
        } catch (RestClientException | ClassCastException | NullPointerException ex) {
            return null;
        }
    }

    private boolean isUsableAiReply(String reply) {
        return reply != null && !reply.isBlank() && !"null".equalsIgnoreCase(reply.trim());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Không có" : value.trim();
    }
}
