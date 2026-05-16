package com.example.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class ChatIntentService {
    public enum IntentType {
        GREETING,
        SMALL_TALK,
        SEARCH,
        STOCK,
        COMPARE,
        ADVICE,
        OUT_OF_SCOPE
    }

    public record ChatIntent(
            IntentType type,
            String originalMessage,
            String normalizedMessage,
            List<String> queryParts,
            BigDecimal maxPrice) {
    }

    private static final List<String> BLOCKED_TERMS = List.of(
            "password", "mat khau", "admin", "staff", "jwt", "token",
            "database", "sql", "source code", "backend", "server", "secret", "api key",
            "apikey", "role", "quyen", "tai khoan", "nguoi dung", "user", "hack",
            "bypass", "dang nhap", "bao mat", "security");

    public ChatIntent parse(String message, String context) {
        String original = normalize(message);
        if (original == null) {
            return new ChatIntent(IntentType.SEARCH, "", "", List.of(), null);
        }

        String normalized = normalizeVietnamese(original);
        if (containsBlockedTopic(normalized)) {
            return new ChatIntent(IntentType.OUT_OF_SCOPE, original, normalized, List.of(), null);
        }
        if (isGreeting(normalized)) {
            return new ChatIntent(IntentType.GREETING, original, normalized, List.of(), null);
        }
        if (isSmallTalk(normalized)) {
            return new ChatIntent(IntentType.SMALL_TALK, original, normalized, List.of(), null);
        }

        String effective = isFollowUpQuestion(normalized) && normalize(context) != null
                ? context + " " + original
                : original;
        String normalizedEffective = normalizeVietnamese(effective);
        BigDecimal maxPrice = extractUpperPrice(normalizedEffective);

        if (isCompareQuestion(normalizedEffective)) {
            return new ChatIntent(
                    IntentType.COMPARE,
                    original,
                    normalizedEffective,
                    extractCompareParts(normalizedEffective),
                    maxPrice);
        }
        if (isAdviceQuestion(normalizedEffective)) {
            return new ChatIntent(
                    IntentType.ADVICE,
                    original,
                    normalizedEffective,
                    List.of(cleanQuery(normalizedEffective)),
                    maxPrice);
        }
        if (isStockQuestion(normalizedEffective)) {
            return new ChatIntent(
                    IntentType.STOCK,
                    original,
                    normalizedEffective,
                    List.of(cleanQuery(normalizedEffective)),
                    maxPrice);
        }

        return new ChatIntent(
                IntentType.SEARCH,
                original,
                normalizedEffective,
                List.of(cleanQuery(normalizedEffective)),
                maxPrice);
    }

    public String normalizeVietnamese(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower
                .replace('á', 'a').replace('à', 'a').replace('ả', 'a').replace('ã', 'a').replace('ạ', 'a')
                .replace('ă', 'a').replace('ắ', 'a').replace('ằ', 'a').replace('ẳ', 'a').replace('ẵ', 'a').replace('ặ', 'a')
                .replace('â', 'a').replace('ấ', 'a').replace('ầ', 'a').replace('ẩ', 'a').replace('ẫ', 'a').replace('ậ', 'a')
                .replace('é', 'e').replace('è', 'e').replace('ẻ', 'e').replace('ẽ', 'e').replace('ẹ', 'e')
                .replace('ê', 'e').replace('ế', 'e').replace('ề', 'e').replace('ể', 'e').replace('ễ', 'e').replace('ệ', 'e')
                .replace('í', 'i').replace('ì', 'i').replace('ỉ', 'i').replace('ĩ', 'i').replace('ị', 'i')
                .replace('ó', 'o').replace('ò', 'o').replace('ỏ', 'o').replace('õ', 'o').replace('ọ', 'o')
                .replace('ô', 'o').replace('ố', 'o').replace('ồ', 'o').replace('ổ', 'o').replace('ỗ', 'o').replace('ộ', 'o')
                .replace('ơ', 'o').replace('ớ', 'o').replace('ờ', 'o').replace('ở', 'o').replace('ỡ', 'o').replace('ợ', 'o')
                .replace('ú', 'u').replace('ù', 'u').replace('ủ', 'u').replace('ũ', 'u').replace('ụ', 'u')
                .replace('ư', 'u').replace('ứ', 'u').replace('ừ', 'u').replace('ử', 'u').replace('ữ', 'u').replace('ự', 'u')
                .replace('ý', 'y').replace('ỳ', 'y').replace('ỷ', 'y').replace('ỹ', 'y').replace('ỵ', 'y')
                .replace('đ', 'd')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String cleanQuery(String normalizedMessage) {
        String cleaned = normalizedMessage
                .replaceAll("\\b(hien tai|shop|technest|toi|minh|em|anh|chi|muon|mua|can|cho toi|co|khong|nao|san pham|tim|kiem|gia|bao nhieu|ton kho|con hang|con ban|het hang|duoi|khong qua|trieu|vnd|d|goi y|de xuat|ban)\\b", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned;
    }

    private boolean containsBlockedTopic(String normalized) {
        return BLOCKED_TERMS.stream().anyMatch(normalized::contains);
    }

    private boolean isGreeting(String normalized) {
        return normalized.equals("xin chao")
                || normalized.equals("chao")
                || normalized.equals("hello")
                || normalized.equals("hi")
                || normalized.equals("hey")
                || normalized.equals("alo");
    }

    private boolean isSmallTalk(String normalized) {
        if (hasProductSignal(normalized)) {
            return false;
        }
        return normalized.contains("ban khoe khong")
                || normalized.contains("khoe khong")
                || normalized.contains("ban la ai")
                || normalized.contains("ten gi")
                || normalized.contains("noi chuyen")
                || normalized.contains("tam su")
                || normalized.contains("cam on")
                || normalized.contains("thank")
                || normalized.contains("bye")
                || normalized.contains("tam biet");
    }

    private boolean hasProductSignal(String normalized) {
        return normalized.contains("san pham")
                || normalized.contains("mua")
                || normalized.contains("gia")
                || normalized.contains("ton kho")
                || normalized.contains("con hang")
                || normalized.contains("con ban")
                || normalized.contains("so sanh")
                || normalized.contains("dien thoai")
                || normalized.contains("phone")
                || normalized.contains("laptop")
                || normalized.contains("lap")
                || normalized.contains("man hinh")
                || normalized.contains("screen")
                || normalized.contains("tai nghe")
                || normalized.contains("headphone")
                || normalized.contains("phu kien")
                || normalized.contains("xiaomi")
                || normalized.contains("iphone")
                || normalized.contains("samsung")
                || normalized.contains("asus")
                || normalized.contains("dell")
                || normalized.contains("apple")
                || normalized.contains("sony");
    }

    private boolean isFollowUpQuestion(String normalized) {
        return normalized.contains("con ")
                || normalized.contains("khac")
                || normalized.contains("them")
                || normalized.contains("so sanh")
                || normalized.contains("duoc khong")
                || normalized.contains("cai nao")
                || normalized.contains("nen mua");
    }

    private boolean isCompareQuestion(String normalized) {
        return normalized.contains("so sanh")
                || normalized.contains("khac nhau")
                || normalized.contains(" vs ");
    }

    private boolean isStockQuestion(String normalized) {
        return normalized.contains("ton kho")
                || normalized.contains("con hang")
                || normalized.contains("con ban")
                || normalized.contains("het hang");
    }

    private boolean isAdviceQuestion(String normalized) {
        return normalized.contains("nen mua")
                || normalized.contains("tu van")
                || normalized.contains("goi y")
                || normalized.contains("de xuat");
    }

    private List<String> extractCompareParts(String normalized) {
        String cleaned = normalized
                .replace("so sanh", " ")
                .replace("khac nhau", " ")
                .replace("duoc khong", " ")
                .replace("giup toi", " ")
                .replace("giup minh", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<String> parts = new ArrayList<>();
        for (String part : cleaned.split("\\s+(?:va|voi|vs)\\s+|,")) {
            String query = cleanQuery(part);
            if (query.length() >= 2) {
                parts.add(query);
            }
        }
        return parts;
    }

    private BigDecimal extractUpperPrice(String normalized) {
        if (!normalized.contains("duoi") && !normalized.contains("<") && !normalized.contains("khong qua")) {
            return null;
        }

        String digits = normalized.replaceAll("[^0-9]", " ").trim().replaceAll("\\s+", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(digits);
            if (normalized.contains("trieu")) {
                return value.multiply(new BigDecimal("1000000"));
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
