package com.example.ecommerce.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.ecommerce.product.Product;

@Component
public class ProductChatResponseBuilder {
    public String greeting() {
        return "Xin chào. Mình có thể hỗ trợ bạn tìm sản phẩm, xem giá, tồn kho và so sánh sản phẩm tại TechNest.";
    }

    public String outOfScope() {
        return "Tôi chỉ hỗ trợ thông tin sản phẩm, giá, tồn kho, so sánh và tư vấn mua hàng tại TechNest Shop.";
    }

    public String notFound() {
        return "Tôi chưa tìm thấy sản phẩm phù hợp trong dữ liệu hiện có. Bạn có thể hỏi bằng tên sản phẩm, thương hiệu, danh mục hoặc khoảng giá.";
    }

    public String searchResults(List<Product> products, Map<Long, String> categories) {
        return productList("Hiện tại TechNest có:", products, categories);
    }

    public String stockResults(List<Product> products, Map<Long, String> categories) {
        return productList("Thông tin sản phẩm trong kho TechNest:", products, categories);
    }

    public String compareNeedTwoProducts() {
        return "Bạn muốn so sánh 2 sản phẩm nào? Hãy nhập cụ thể tên 2 sản phẩm cùng loại, ví dụ: so sánh iPhone 17 Pro và Xiaomi 14 Pro.";
    }

    public String compareMustBeSameCategory() {
        return "Mình chỉ so sánh 2 sản phẩm cùng loại. Bạn hãy chọn 2 điện thoại, 2 laptop, 2 màn hình, hoặc 2 sản phẩm trong cùng danh mục.";
    }

    public String compare(List<Product> products, Map<Long, String> categories) {
        if (products.size() < 2) {
            return compareNeedTwoProducts();
        }

        Product first = products.get(0);
        Product second = products.get(1);

        return """
                Mình so sánh 2 sản phẩm này theo dữ liệu TechNest:

                1. %s
                - Giá: %s
                - Thương hiệu: %s
                - Danh mục: %s
                - Tồn kho: %s
                - Điểm nổi bật:
                %s

                2. %s
                - Giá: %s
                - Thương hiệu: %s
                - Danh mục: %s
                - Tồn kho: %s
                - Điểm nổi bật:
                %s

                Kết luận nhanh:
                - Rẻ hơn: %s
                - Còn hàng nhiều hơn: %s
                """.formatted(
                safe(first.getName()),
                formatPrice(first.getPrice()),
                safe(first.getBrand()),
                safe(categories.get(first.getCategoryId())),
                stockText(first),
                formatHighlights(first),
                safe(second.getName()),
                formatPrice(second.getPrice()),
                safe(second.getBrand()),
                safe(categories.get(second.getCategoryId())),
                stockText(second),
                formatHighlights(second),
                cheaperProduct(first, second),
                betterStockProduct(first, second));
    }

    public String adviceFallback(List<Product> products, Map<Long, String> categories) {
        return productList("Mình gợi ý các sản phẩm phù hợp trong database:", products, categories);
    }

    private String productList(String title, List<Product> products, Map<Long, String> categories) {
        String lines = products.stream()
                .limit(8)
                .map(product -> "- " + safe(product.getName())
                        + " | " + safe(product.getBrand())
                        + " | " + safe(categories.get(product.getCategoryId()))
                        + " | " + formatPrice(product.getPrice())
                        + " | " + stockText(product))
                .collect(java.util.stream.Collectors.joining("\n"));
        return title + "\n" + lines;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "Chưa có giá";
        }
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(price);
    }

    private String stockText(Product product) {
        return product.getQuantity() > 0 ? "còn " + product.getQuantity() + " sản phẩm" : "hết hàng";
    }

    private String formatHighlights(Product product) {
        String description = safe(product.getDescriptionShort());
        String[] lines = description.split("\\R+");
        return java.util.Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(5)
                .map(line -> "  - " + line)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String cheaperProduct(Product first, Product second) {
        if (first.getPrice() == null && second.getPrice() == null) {
            return "Chưa có dữ liệu giá";
        }
        if (first.getPrice() == null) {
            return safe(second.getName());
        }
        if (second.getPrice() == null) {
            return safe(first.getName());
        }
        int result = first.getPrice().compareTo(second.getPrice());
        if (result == 0) {
            return "Hai sản phẩm bằng giá";
        }
        return result < 0 ? safe(first.getName()) : safe(second.getName());
    }

    private String betterStockProduct(Product first, Product second) {
        if (first.getQuantity() == second.getQuantity()) {
            return "Hai sản phẩm có tồn kho bằng nhau";
        }
        return first.getQuantity() > second.getQuantity() ? safe(first.getName()) : safe(second.getName());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Không có" : value.trim();
    }
}
