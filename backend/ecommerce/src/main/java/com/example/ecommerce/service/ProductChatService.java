package com.example.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.ecommerce.model.Category;
import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductRepository;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.service.ChatIntentService.ChatIntent;

@Service
public class ProductChatService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ChatIntentService intentService;

    public ProductChatService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ChatIntentService intentService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.intentService = intentService;
    }

    public List<Product> search(ChatIntent intent) {
        List<Product> products = productRepository.findAll();
        Map<Long, String> categories = categoryNames();
        QuerySpec spec = QuerySpec.from(intent.queryParts(), intent.maxPrice(), intentService);

        return products.stream()
                .filter(product -> matches(product, categories, spec))
                .sorted((left, right) -> Integer.compare(
                        score(right, categories, spec),
                        score(left, categories, spec)))
                .limit(12)
                .toList();
    }

    public List<Product> compareProducts(ChatIntent intent) {
        List<Product> allProducts = productRepository.findAll();
        List<Product> selected = new ArrayList<>();

        for (String part : intent.queryParts()) {
            findBestProduct(part, allProducts).ifPresent(product -> {
                if (selected.stream().noneMatch(existing -> Objects.equals(existing.getId(), product.getId()))) {
                    selected.add(product);
                }
            });
        }

        if (selected.size() < 2) {
            return List.of();
        }

        Long categoryId = selected.get(0).getCategoryId();
        List<Product> sameCategory = selected.stream()
                .filter(product -> Objects.equals(product.getCategoryId(), categoryId))
                .limit(2)
                .toList();

        return sameCategory.size() == 2 ? sameCategory : List.of();
    }

    public Map<Long, String> categoryNames() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private java.util.Optional<Product> findBestProduct(String query, List<Product> products) {
        QuerySpec spec = QuerySpec.from(List.of(query), null, intentService);
        return products.stream()
                .map(product -> Map.entry(product, score(product, categoryNames(), spec)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private boolean matches(Product product, Map<Long, String> categories, QuerySpec spec) {
        if (spec.maxPrice() != null && product.getPrice() != null && product.getPrice().compareTo(spec.maxPrice()) > 0) {
            return false;
        }

        if (!spec.categoryNames().isEmpty()) {
            String category = intentService.normalizeVietnamese(categories.getOrDefault(product.getCategoryId(), ""));
            return spec.categoryNames().contains(category);
        }

        if (spec.terms().isEmpty()) {
            return true;
        }

        return score(product, categories, spec) > 0;
    }

    private int score(Product product, Map<Long, String> categories, QuerySpec spec) {
        String name = intentService.normalizeVietnamese(nullToEmpty(product.getName()));
        String brand = intentService.normalizeVietnamese(nullToEmpty(product.getBrand()));
        String description = intentService.normalizeVietnamese(nullToEmpty(product.getDescriptionShort()));
        String category = intentService.normalizeVietnamese(categories.getOrDefault(product.getCategoryId(), ""));

        int score = 0;
        for (String term : spec.terms()) {
            if (term.length() < 2) {
                continue;
            }
            if (name.equals(term)) {
                score += 100;
            } else if (name.contains(term)) {
                score += 40;
            }
            if (brand.equals(term)) {
                score += 70;
            } else if (brand.contains(term)) {
                score += 30;
            }
            if (category.equals(term)) {
                score += 60;
            }
            if (description.contains(term)) {
                score += 5;
            }
        }
        return score;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record QuerySpec(Set<String> terms, Set<String> categoryNames, BigDecimal maxPrice) {
        static QuerySpec from(List<String> queryParts, BigDecimal maxPrice, ChatIntentService intentService) {
            LinkedHashSet<String> terms = new LinkedHashSet<>();
            for (String part : queryParts) {
                String normalized = intentService.normalizeVietnamese(part);
                if (!normalized.isBlank()) {
                    terms.add(normalized);
                    for (String token : normalized.split("\\s+")) {
                        if (token.length() >= 2) {
                            terms.add(token);
                        }
                    }
                }
            }

            LinkedHashSet<String> categories = new LinkedHashSet<>();
            String joined = String.join(" ", terms);
            if (joined.contains("dien thoai") || joined.contains("smartphone") || joined.contains("phone")) {
                categories.add("phone");
            }
            if (joined.contains("lap") || joined.contains("laptop") || joined.contains("may tinh xach tay") || joined.contains("notebook")) {
                categories.add("laptop");
            }
            if (joined.contains("man hinh") || joined.contains("monitor") || joined.contains("screen")) {
                categories.add("screen");
            }
            if (joined.contains("tai nghe") || joined.contains("headphone") || joined.contains("earphone")) {
                categories.add("headphone");
            }
            if (joined.contains("phu kien") || joined.contains("accessory") || joined.contains("accessories")) {
                categories.add("accessories");
            }

            if (!categories.isEmpty()) {
                terms.removeIf(term -> categories.stream().anyMatch(category -> category.contains(term) || term.contains(category)));
            }

            return new QuerySpec(terms, categories, maxPrice);
        }
    }
}
