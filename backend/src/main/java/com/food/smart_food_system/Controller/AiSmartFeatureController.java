package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.OrderEntity;
import com.food.smart_food_system.Entity.OrderItemEntity;
import com.food.smart_food_system.Entity.PaymentEntity;
import com.food.smart_food_system.Entity.ReviewEntity;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.OrderItemRepository;
import com.food.smart_food_system.Repository.OrderRepository;
import com.food.smart_food_system.Repository.PaymentRepository;
import com.food.smart_food_system.Repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FILE FULL CODE TÍCH HỢP AI CHO SMART FOOD SYSTEM
 * -------------------------------------------------
 * Copy file này vào:
 * src/main/java/com/food/smart_food_system/Controller/AiSmartFeatureController.java
 *
 * API chính cho FE React:
 * 1. GET  /api/ai/recommendations/{userId}
 * 2. POST /api/ai/chat
 * 3. GET  /api/ai/smart-search?keyword=...
 * 4. POST /api/ai/payment/qr
 * 5. POST /api/ai/food-description
 * 6. POST /api/ai/review/sentiment
 * 7. POST /api/ai/food-image/verify
 * 8. GET  /api/ai/manager/demand-forecast
 * 9. GET  /api/ai/manager/review-insights
 *
 * Lưu ý SecurityConfig:
 * Nếu bị 401/403 khi FE gọi /api/ai/** thì thêm dòng này vào SecurityConfig:
 * .requestMatchers("/api/ai/**").permitAll()
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiSmartFeatureController {

    private final FoodRepository foodRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${payment.bank-code:MB}")
    private String bankCode;

    @Value("${payment.account-no:0123456789}")
    private String accountNo;

    @Value("${payment.account-name:LE TRAN TIEN BAO}")
    private String accountName;

    public AiSmartFeatureController(
            FoodRepository foodRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ReviewRepository reviewRepository,
            PaymentRepository paymentRepository
    ) {
        this.foodRepository = foodRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
    }

    // =========================================================
    // 1. AI GỢI Ý MÓN ĂN CHO KHÁCH HÀNG
    // Quy trình:
    // - Lấy lịch sử đặt hàng theo User ID.
    // - Tính điểm món liên quan theo category, món thường mua chung, độ phổ biến, rating.
    // - Trả về danh sách ID món ăn + dữ liệu món để FE hiển thị.
    // =========================================================

    @GetMapping("/recommendations/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResult<RecommendationResponse>> recommendFoods(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        limit = normalizeLimit(limit, 4, 30);

        List<FoodEntity> allFoods = foodRepository.findAll();
        if (allFoods.isEmpty()) {
            return ResponseEntity.ok(ApiResult.success("Chưa có món ăn trong hệ thống.",
                    new RecommendationResponse(userId, "EMPTY_MENU", List.of(), List.of())));
        }

        Map<Long, FoodEntity> foodMap = allFoods.stream()
                .filter(f -> f.getId() != null)
                .collect(Collectors.toMap(FoodEntity::getId, Function.identity(), (a, b) -> a));

        List<OrderEntity> userOrders = orderRepository.findByUserIdOrderByIdDesc(userId);

        // User chưa có lịch sử: fallback sang món phổ biến/rating cao.
        if (userOrders.isEmpty()) {
            List<FoodScore> popular = buildPopularScores(allFoods, List.of());
            List<FoodDTO> foods = popular.stream()
                    .limit(limit)
                    .map(score -> toFoodDTO(score.food, score.score, "POPULAR_FALLBACK"))
                    .toList();

            return ResponseEntity.ok(ApiResult.success(
                    "User chưa có lịch sử đặt hàng, hệ thống gợi ý món phổ biến.",
                    new RecommendationResponse(userId, "POPULAR_FALLBACK", extractIds(foods), foods)
            ));
        }

        Set<Long> userOrderedFoodIds = new HashSet<>();
        Map<Long, Integer> userFoodFrequency = new HashMap<>();
        Map<Long, Integer> categoryFrequency = new HashMap<>();

        for (OrderEntity order : userOrders) {
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItemEntity item : items) {
                FoodEntity food = item.getFood();
                if (food == null || food.getId() == null) continue;

                Long foodId = food.getId();
                userOrderedFoodIds.add(foodId);
                userFoodFrequency.merge(foodId, safeQuantity(item.getQuantity()), Integer::sum);

                Long categoryId = getCategoryId(food);
                if (categoryId != null) {
                    categoryFrequency.merge(categoryId, safeQuantity(item.getQuantity()), Integer::sum);
                }
            }
        }

        // Nếu order có nhưng không đọc được item thì vẫn fallback món phổ biến.
        if (userOrderedFoodIds.isEmpty()) {
            List<FoodScore> popular = buildPopularScores(allFoods, List.of());
            List<FoodDTO> foods = popular.stream()
                    .limit(limit)
                    .map(score -> toFoodDTO(score.food, score.score, "POPULAR_FALLBACK"))
                    .toList();

            return ResponseEntity.ok(ApiResult.success(
                    "Không tìm thấy chi tiết món trong lịch sử order, hệ thống gợi ý món phổ biến.",
                    new RecommendationResponse(userId, "POPULAR_FALLBACK", extractIds(foods), foods)
            ));
        }

        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, String> reasonMap = new HashMap<>();

        // Content-based: món cùng category với món user đã mua.
        for (FoodEntity food : allFoods) {
            if (food.getId() == null) continue;
            if (userOrderedFoodIds.contains(food.getId())) continue;
            if (!isAvailable(food)) continue;

            Long categoryId = getCategoryId(food);
            if (categoryId != null && categoryFrequency.containsKey(categoryId)) {
                double categoryScore = categoryFrequency.get(categoryId) * 3.0;
                scoreMap.merge(food.getId(), categoryScore, Double::sum);
                reasonMap.put(food.getId(), "CONTENT_BASED_SAME_CATEGORY");
            }
        }

        // Collaborative filtering đơn giản: món nào hay được mua cùng với món user đã mua.
        List<OrderEntity> allOrders = orderRepository.findAll();
        for (OrderEntity order : allOrders) {
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
            Set<Long> orderFoodIds = items.stream()
                    .map(OrderItemEntity::getFood)
                    .filter(Objects::nonNull)
                    .map(FoodEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            boolean hasSimilarTaste = orderFoodIds.stream().anyMatch(userOrderedFoodIds::contains);
            if (!hasSimilarTaste) continue;

            for (Long foodId : orderFoodIds) {
                if (userOrderedFoodIds.contains(foodId)) continue;
                FoodEntity food = foodMap.get(foodId);
                if (food == null || !isAvailable(food)) continue;

                scoreMap.merge(foodId, 5.0, Double::sum);
                reasonMap.put(foodId, "COLLABORATIVE_FILTERING_BOUGHT_TOGETHER");
            }
        }

        // Cộng điểm rating, stock, độ phổ biến để kết quả ổn định hơn.
        Map<Long, Integer> globalPopularity = countGlobalFoodPopularity(allOrders);
        for (FoodEntity food : allFoods) {
            if (food.getId() == null || userOrderedFoodIds.contains(food.getId()) || !isAvailable(food)) continue;

            double bonus = 0;
            bonus += safeRating(food).doubleValue();
            bonus += Math.min(globalPopularity.getOrDefault(food.getId(), 0), 20) * 0.4;
            bonus += food.getStock() != null && food.getStock() > 0 ? 1.0 : 0.0;
            scoreMap.merge(food.getId(), bonus, Double::sum);
            reasonMap.putIfAbsent(food.getId(), "POPULAR_AND_RATING_BONUS");
        }

        List<FoodDTO> result = scoreMap.entrySet().stream()
                .map(entry -> new FoodScore(foodMap.get(entry.getKey()), entry.getValue(), reasonMap.get(entry.getKey())))
                .filter(score -> score.food != null)
                .sorted(Comparator.comparingDouble((FoodScore s) -> s.score).reversed())
                .limit(limit)
                .map(score -> toFoodDTO(score.food, score.score, score.reason))
                .collect(Collectors.toList());

        // Nếu thiếu món, bổ sung món phổ biến.
        if (result.size() < limit) {
            Set<Long> existed = result.stream().map(FoodDTO::getId).collect(Collectors.toSet());
            List<FoodDTO> backup = buildPopularScores(allFoods, userOrderedFoodIds).stream()
                    .filter(score -> !existed.contains(score.food.getId()))
                    .limit(limit - result.size())
                    .map(score -> toFoodDTO(score.food, score.score, "POPULAR_BACKUP"))
                    .toList();
            result.addAll(backup);
        }

        return ResponseEntity.ok(ApiResult.success(
                "Gợi ý món ăn thành công.",
                new RecommendationResponse(userId, "HYBRID_RECOMMENDATION", extractIds(result), result)
        ));
    }

    // =========================================================
    // 2. AI CHATBOT TƯ VẤN MÓN + QR THANH TOÁN
    // - Hỏi menu, món chay, món cay, món rẻ, trạng thái đơn.
    // - Nếu user hỏi thanh toán/QR/chuyển khoản thì trả QR ngay.
    // - Nếu có GEMINI_API_KEY thì gọi Gemini, không có key thì fallback logic nội bộ.
    // =========================================================

    @PostMapping("/chat")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResult<ChatBotResponse>> chat(@RequestBody ChatBotRequest request) {
        String message = normalizeText(request.getMessage());
        Long userId = request.getUserId();

        if (!StringUtils.hasText(message)) {
            return ResponseEntity.ok(ApiResult.error("Vui lòng nhập nội dung cần hỏi chatbot."));
        }

        if (isPaymentIntent(message)) {
            BigDecimal amount = request.getAmount() != null ? request.getAmount() : findLatestOrderAmount(userId);
            String content = buildTransferContent(userId, request.getOrderId());
            PaymentQrDTO qr = buildPaymentQr(amount, content);
            String answer = "Dạ được ạ. Đây là mã QR thanh toán cho đơn hàng của anh. Anh chuyển khoản đúng số tiền và nội dung để quán xác nhận nhanh nhé.";
            return ResponseEntity.ok(ApiResult.success("Chatbot đã tạo QR thanh toán.", new ChatBotResponse(answer, qr, List.of())));
        }

        if (message.contains("trạng thái") || message.contains("don hang") || message.contains("đơn hàng") || message.contains("order")) {
            String answer = answerLatestOrderStatus(userId);
            return ResponseEntity.ok(ApiResult.success("Chatbot trả lời trạng thái đơn hàng.", new ChatBotResponse(answer, null, List.of())));
        }

        List<FoodEntity> foods = foodRepository.findAll();
        List<FoodDTO> relatedFoods = findFoodsByIntent(message, foods, 6);

        String fallbackAnswer = buildLocalChatAnswer(message, relatedFoods);
        String answer = callGeminiForChat(message, foods, fallbackAnswer);

        return ResponseEntity.ok(ApiResult.success(
                "Chatbot trả lời thành công.",
                new ChatBotResponse(answer, null, relatedFoods)
        ));
    }

    // =========================================================
    // 3. TÌM KIẾM THÔNG MINH / SEMANTIC SEARCH BẢN NỘI BỘ
    // - Không cần vector DB vẫn chạy được.
    // - Hiểu các ý như: mát, mùa hè, cay, chay, rẻ, nhiều đạm...
    // =========================================================

    @GetMapping("/smart-search")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResult<List<FoodDTO>>> smartSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "12") int limit
    ) {
        limit = normalizeLimit(limit, 4, 50);
        String query = normalizeText(keyword);
        if (!StringUtils.hasText(query)) {
            return ResponseEntity.ok(ApiResult.success("Từ khóa rỗng.", List.of()));
        }

        List<FoodEntity> foods = foodRepository.findAll();
        List<FoodDTO> result = findFoodsByIntent(query, foods, limit);

        return ResponseEntity.ok(ApiResult.success("Tìm kiếm thông minh thành công.", result));
    }

    // =========================================================
    // 4. THANH TOÁN QR
    // - Tự render URL QR VietQR để FE hiển thị.
    // - Có thể dùng độc lập hoặc gọi qua chatbot.
    // =========================================================

    @PostMapping("/payment/qr")
    public ResponseEntity<ApiResult<PaymentQrDTO>> createPaymentQr(@RequestBody PaymentQrRequest request) {
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        String content = StringUtils.hasText(request.getOrderCode()) ? request.getOrderCode() : "SMART FOOD PAYMENT";

        PaymentQrDTO qr = buildPaymentQr(amount, content);
        return ResponseEntity.ok(ApiResult.success("Tạo QR thanh toán thành công.", qr));
    }

    /**
     * Nếu muốn lưu payment pending vào DB theo orderId.
     * API này không tự xác nhận thanh toán thật, chỉ tạo bản ghi chờ thanh toán.
     */
    @PostMapping("/payment/qr/order/{orderId}")
    @Transactional
    public ResponseEntity<ApiResult<PaymentQrDTO>> createPaymentQrForOrder(@PathVariable Long orderId) {
        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResult.error("Không tìm thấy đơn hàng ID = " + orderId));
        }

        OrderEntity order = orderOpt.get();
        BigDecimal amount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        String content = StringUtils.hasText(order.getOrderCode()) ? order.getOrderCode() : "ORDER " + order.getId();

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setProvider("VIETQR");
        payment.setStatus("PENDING");
        payment.setTransactionCode("QR-" + System.currentTimeMillis());
        paymentRepository.save(payment);

        PaymentQrDTO qr = buildPaymentQr(amount, content);
        return ResponseEntity.ok(ApiResult.success("Tạo QR thanh toán cho đơn hàng thành công.", qr));
    }

    // =========================================================
    // 5. AI CONTENT GENERATION - TỰ TẠO MÔ TẢ MÓN ĂN
    // =========================================================

    @PostMapping("/food-description")
    public ResponseEntity<ApiResult<FoodDescriptionResponse>> generateFoodDescription(@RequestBody FoodDescriptionRequest request) {
        if (!StringUtils.hasText(request.getFoodName())) {
            return ResponseEntity.ok(ApiResult.error("Vui lòng nhập tên món ăn."));
        }

        String prompt = "Viết mô tả món ăn bằng tiếng Việt, hấp dẫn, chuẩn SEO, 3-4 câu. "
                + "Tên món: " + request.getFoodName() + ". "
                + "Nguyên liệu: " + nullToEmpty(request.getIngredients()) + ". "
                + "Văn phong phù hợp website đặt đồ ăn.";

        String fallback = "Món " + request.getFoodName().trim()
                + " được chế biến từ nguyên liệu tươi ngon, hương vị hài hòa và dễ thưởng thức. "
                + "Đây là lựa chọn phù hợp cho những bữa ăn nhanh, tiện lợi nhưng vẫn đảm bảo chất lượng. "
                + "Món ăn mang lại trải nghiệm thơm ngon, hấp dẫn và phù hợp với nhiều khẩu vị.";

        String description = callGeminiText(prompt, fallback);
        return ResponseEntity.ok(ApiResult.success("Tạo mô tả món ăn thành công.", new FoodDescriptionResponse(description)));
    }

    // =========================================================
    // 6. SENTIMENT ANALYSIS - PHÂN TÍCH TÂM TRẠNG ĐÁNH GIÁ
    // =========================================================

    @PostMapping("/review/sentiment")
    public ResponseEntity<ApiResult<SentimentResponse>> analyzeSentiment(@RequestBody SentimentRequest request) {
        String comment = normalizeText(request.getComment());
        if (!StringUtils.hasText(comment)) {
            return ResponseEntity.ok(ApiResult.error("Vui lòng nhập nội dung đánh giá."));
        }

        SentimentResponse result = analyzeSentimentLocal(comment);
        return ResponseEntity.ok(ApiResult.success("Phân tích đánh giá thành công.", result));
    }

    @GetMapping("/manager/review-insights")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResult<ReviewInsightResponse>> reviewInsights() {
        List<ReviewEntity> reviews = reviewRepository.findAll();
        int positive = 0;
        int negative = 0;
        int neutral = 0;
        Map<String, Integer> complaintTags = new LinkedHashMap<>();

        for (ReviewEntity review : reviews) {
            SentimentResponse sentiment = analyzeSentimentLocal(review.getComment());
            if ("POSITIVE".equals(sentiment.getLabel())) positive++;
            else if ("NEGATIVE".equals(sentiment.getLabel())) negative++;
            else neutral++;

            for (String tag : sentiment.getTags()) {
                complaintTags.merge(tag, 1, Integer::sum);
            }
        }

        ReviewInsightResponse response = new ReviewInsightResponse(reviews.size(), positive, negative, neutral, complaintTags);
        return ResponseEntity.ok(ApiResult.success("Thống kê cảm xúc khách hàng thành công.", response));
    }

    // =========================================================
    // 7. FOOD IMAGE RECOGNITION - XÁC THỰC ẢNH ĐÁNH GIÁ
    // Bản này không dùng model ảnh thật, nhưng có endpoint hoàn chỉnh cho đồ án.
    // Nếu muốn thật hơn thì nối Google Vision / Gemini Vision sau.
    // =========================================================

    @PostMapping(value = "/food-image/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResult<ImageVerifyResponse>> verifyFoodImage(
            @RequestParam Long foodId,
            @RequestParam("image") MultipartFile image
    ) {
        Optional<FoodEntity> foodOpt = foodRepository.findById(foodId);
        if (foodOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResult.error("Không tìm thấy món ăn ID = " + foodId));
        }

        if (image == null || image.isEmpty()) {
            return ResponseEntity.ok(ApiResult.error("Vui lòng upload ảnh món ăn."));
        }

        String contentType = image.getContentType() == null ? "" : image.getContentType().toLowerCase();
        boolean isImage = contentType.startsWith("image/");
        boolean validSize = image.getSize() <= 10 * 1024 * 1024;

        boolean verified = isImage && validSize;
        String message = verified
                ? "Ảnh hợp lệ để gửi đánh giá. Bước nhận diện AI thật có thể tích hợp Gemini Vision/Google Vision sau."
                : "Ảnh không hợp lệ. Vui lòng upload file ảnh dưới 10MB.";

        ImageVerifyResponse response = new ImageVerifyResponse(
                foodId,
                foodOpt.get().getName(),
                image.getOriginalFilename(),
                contentType,
                image.getSize(),
                verified,
                verified ? 0.75 : 0.0,
                message
        );

        return ResponseEntity.ok(ApiResult.success("Kiểm tra ảnh đánh giá thành công.", response));
    }

    // =========================================================
    // 8. DEMAND FORECASTING - DỰ BÁO DOANH THU/NHU CẦU
    // Bản nội bộ: trung bình 7 ngày gần nhất + hệ số cuối tuần.
    // =========================================================

    @GetMapping("/manager/demand-forecast")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResult<DemandForecastResponse>> demandForecast(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now().plusDays(1);
        List<OrderEntity> orders = orderRepository.findAll();

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(7);

        List<OrderEntity> recentOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(from))
                .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(today))
                .toList();

        long totalOrders = recentOrders.size();
        BigDecimal totalRevenue = recentOrders.stream()
                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgOrders = totalOrders / 7.0;
        BigDecimal avgRevenue = totalRevenue.divide(BigDecimal.valueOf(7), 2, java.math.RoundingMode.HALF_UP);

        double weekendFactor = isWeekend(targetDate) ? 1.25 : 1.0;
        int predictedOrders = Math.max(1, (int) Math.round(avgOrders * weekendFactor));
        BigDecimal predictedRevenue = avgRevenue.multiply(BigDecimal.valueOf(weekendFactor));

        Map<Long, Integer> foodCount = countFoodPopularityByOrders(recentOrders);
        List<FoodDemandDTO> topFoods = foodCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(8)
                .map(entry -> {
                    FoodEntity food = foodRepository.findById(entry.getKey()).orElse(null);
                    String name = food != null ? food.getName() : "Food ID " + entry.getKey();
                    int predictedQty = Math.max(1, (int) Math.round((entry.getValue() / 7.0) * weekendFactor));
                    return new FoodDemandDTO(entry.getKey(), name, predictedQty);
                })
                .toList();

        DemandForecastResponse response = new DemandForecastResponse(
                targetDate,
                predictedOrders,
                predictedRevenue,
                isWeekend(targetDate) ? "Ngày cuối tuần, nhu cầu dự kiến tăng." : "Ngày thường, nhu cầu dự kiến ổn định.",
                topFoods
        );

        return ResponseEntity.ok(ApiResult.success("Dự báo nhu cầu thành công.", response));
    }

    // =========================================================
    // PRIVATE LOGIC
    // =========================================================

    private List<FoodScore> buildPopularScores(List<FoodEntity> allFoods, Collection<Long> excludeIds) {
        Set<Long> exclude = new HashSet<>(excludeIds == null ? List.of() : excludeIds);
        Map<Long, Integer> popularity = countGlobalFoodPopularity(orderRepository.findAll());

        return allFoods.stream()
                .filter(food -> food.getId() != null)
                .filter(food -> !exclude.contains(food.getId()))
                .filter(this::isAvailable)
                .map(food -> {
                    double score = 0;
                    score += Math.min(popularity.getOrDefault(food.getId(), 0), 30) * 2.0;
                    score += safeRating(food).doubleValue() * 2.0;
                    score += food.getStock() != null && food.getStock() > 0 ? 1.0 : 0.0;
                    return new FoodScore(food, score, "POPULAR");
                })
                .sorted(Comparator.comparingDouble((FoodScore s) -> s.score).reversed())
                .toList();
    }

    private Map<Long, Integer> countGlobalFoodPopularity(List<OrderEntity> orders) {
        return countFoodPopularityByOrders(orders);
    }

    private Map<Long, Integer> countFoodPopularityByOrders(List<OrderEntity> orders) {
        Map<Long, Integer> result = new HashMap<>();
        for (OrderEntity order : orders) {
            if (order.getId() == null) continue;
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItemEntity item : items) {
                if (item.getFood() == null || item.getFood().getId() == null) continue;
                result.merge(item.getFood().getId(), safeQuantity(item.getQuantity()), Integer::sum);
            }
        }
        return result;
    }

    private List<FoodDTO> findFoodsByIntent(String query, List<FoodEntity> foods, int limit) {
        String q = normalizeText(query);
        Map<Long, Double> scoreMap = new HashMap<>();

        for (FoodEntity food : foods) {
            if (food.getId() == null || !isAvailable(food)) continue;

            String name = normalizeText(food.getName());
            String desc = normalizeText(food.getDescription());
            String category = food.getCategory() != null ? normalizeText(food.getCategory().getName()) : "";
            String full = name + " " + desc + " " + category;

            double score = 0;
            for (String token : q.split("\\s+")) {
                if (token.length() < 2) continue;
                if (name.contains(token)) score += 5;
                if (desc.contains(token)) score += 2;
                if (category.contains(token)) score += 3;
            }

            if (q.contains("mát") || q.contains("mat") || q.contains("mùa hè") || q.contains("giai khat") || q.contains("giải khát")) {
                if (containsAny(full, "salad", "rau", "sinh to", "sinh tố", "nuoc", "nước", "tra", "trà", "hoa qua", "trai cay", "trái cây")) score += 8;
            }
            if (q.contains("cay") || q.contains("spicy")) {
                if (containsAny(full, "cay", "ớt", "ot", "sa tế", "sate", "kim chi")) score += 8;
            }
            if (q.contains("chay") || q.contains("vegetarian")) {
                if (containsAny(full, "chay", "rau", "đậu", "dau", "nấm", "nam")) score += 8;
            }
            if (q.contains("rẻ") || q.contains("re") || q.contains("giá rẻ") || q.contains("cheap")) {
                if (food.getPrice() != null && food.getPrice().compareTo(BigDecimal.valueOf(50000)) <= 0) score += 8;
            }
            if (q.contains("đạm") || q.contains("protein") || q.contains("thịt") || q.contains("thit")) {
                if (containsAny(full, "gà", "ga", "bò", "bo", "heo", "thịt", "thit", "cá", "ca", "trứng", "trung")) score += 8;
            }

            score += safeRating(food).doubleValue() * 0.8;
            if (score > 0) scoreMap.put(food.getId(), score);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> foods.stream().filter(f -> Objects.equals(f.getId(), entry.getKey())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(food -> toFoodDTO(food, scoreMap.get(food.getId()), "SMART_SEARCH"))
                .toList();
    }

    private String buildLocalChatAnswer(String message, List<FoodDTO> relatedFoods) {
        if (relatedFoods == null || relatedFoods.isEmpty()) {
            return "Hiện tại em chưa tìm thấy món thật sự phù hợp trong menu. Anh có thể hỏi rõ hơn như: món cay, món chay, món giá rẻ hoặc món mát cho mùa hè nhé.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Dạ, em gợi ý cho anh một vài món phù hợp trong menu:\n");
        for (int i = 0; i < Math.min(5, relatedFoods.size()); i++) {
            FoodDTO food = relatedFoods.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(food.getName())
                    .append(" - ")
                    .append(food.getPrice())
                    .append("đ")
                    .append("\n");
        }
        sb.append("Anh chọn món nào thì em có thể hỗ trợ tạo QR thanh toán luôn ạ.");
        return sb.toString();
    }

    private String callGeminiForChat(String message, List<FoodEntity> foods, String fallback) {
        String menuContext = foods.stream()
                .filter(this::isAvailable)
                .limit(30)
                .map(food -> "- " + food.getName() + " | giá: " + food.getPrice() + " | mô tả: " + nullToEmpty(food.getDescription()))
                .collect(Collectors.joining("\n"));

        String prompt = "Bạn là chatbot tư vấn đặt món cho website Smart Food. "
                + "Chỉ tư vấn dựa trên menu dưới đây, trả lời tiếng Việt, thân thiện, ngắn gọn.\n\n"
                + "MENU:\n" + menuContext + "\n\n"
                + "Câu hỏi khách hàng: " + message;

        return callGeminiText(prompt, fallback);
    }

    @SuppressWarnings("unchecked")
    private String callGeminiText(String prompt, String fallback) {
        if (!StringUtils.hasText(geminiApiKey)) {
            return fallback;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel
                    + ":generateContent?key="
                    + geminiApiKey;

            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) return fallback;

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) return fallback;

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
            if (contentMap == null) return fallback;

            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
            if (parts == null || parts.isEmpty()) return fallback;

            Object text = parts.get(0).get("text");
            return text != null && StringUtils.hasText(text.toString()) ? text.toString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String answerLatestOrderStatus(Long userId) {
        if (userId == null) {
            return "Anh vui lòng cung cấp User ID để em kiểm tra trạng thái đơn hàng nhé.";
        }

        List<OrderEntity> orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        if (orders.isEmpty()) {
            return "Hiện tại em chưa tìm thấy đơn hàng nào của anh trong hệ thống.";
        }

        OrderEntity latest = orders.get(0);
        return "Đơn hàng gần nhất của anh là " + latest.getOrderCode()
                + ", trạng thái đơn: " + latest.getOrderStatus()
                + ", trạng thái thanh toán: " + latest.getPaymentStatus()
                + ", tổng tiền: " + latest.getFinalAmount() + "đ.";
    }

    private BigDecimal findLatestOrderAmount(Long userId) {
        if (userId == null) return BigDecimal.ZERO;
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        if (orders.isEmpty()) return BigDecimal.ZERO;
        OrderEntity latest = orders.get(0);
        return latest.getFinalAmount() != null ? latest.getFinalAmount() : BigDecimal.ZERO;
    }

    private PaymentQrDTO buildPaymentQr(BigDecimal amount, String transferContent) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        String encodedAccountName = encode(accountName);
        String encodedContent = encode(transferContent);

        String qrImageUrl = "https://img.vietqr.io/image/"
                + bankCode + "-" + accountNo + "-compact2.png"
                + "?amount=" + safeAmount.toBigInteger()
                + "&addInfo=" + encodedContent
                + "&accountName=" + encodedAccountName;

        return new PaymentQrDTO(bankCode, accountNo, accountName, safeAmount, transferContent, qrImageUrl);
    }

    private String buildTransferContent(Long userId, Long orderId) {
        if (orderId != null) return "SMART FOOD ORDER " + orderId;
        if (userId != null) return "SMART FOOD USER " + userId;
        return "SMART FOOD PAYMENT";
    }

    private SentimentResponse analyzeSentimentLocal(String comment) {
        String text = normalizeText(comment);
        int positive = 0;
        int negative = 0;
        List<String> tags = new ArrayList<>();

        if (containsAny(text, "ngon", "tuyệt", "tot", "tốt", "hai long", "hài lòng", "nhanh", "sạch", "thom", "thơm", "de an", "dễ ăn")) {
            positive += 3;
            tags.add("Khen ngợi hương vị/dịch vụ");
        }
        if (containsAny(text, "dở", "te", "tệ", "nguội", "lâu", "chậm", "bẩn", "mặn", "nhạt", "khó ăn", "that vong", "thất vọng")) {
            negative += 3;
        }
        if (containsAny(text, "lâu", "chậm", "tre", "trễ")) tags.add("Phàn nàn phục vụ chậm");
        if (containsAny(text, "mặn", "nhạt", "cay quá", "nguội", "khó ăn", "dở")) tags.add("Phàn nàn về chất lượng món ăn");
        if (containsAny(text, "đắt", "dat", "giá cao", "mac", "mắc")) tags.add("Phàn nàn về giá");

        String label;
        double score;
        if (positive > negative) {
            label = "POSITIVE";
            score = Math.min(0.95, 0.55 + positive * 0.1);
        } else if (negative > positive) {
            label = "NEGATIVE";
            score = Math.min(0.95, 0.55 + negative * 0.1);
        } else {
            label = "NEUTRAL";
            score = 0.5;
        }

        if (tags.isEmpty()) tags.add("Đánh giá chung");
        return new SentimentResponse(label, score, tags);
    }

    private FoodDTO toFoodDTO(FoodEntity food, Double score, String reason) {
        Long categoryId = getCategoryId(food);
        String categoryName = food.getCategory() != null ? food.getCategory().getName() : null;
        return new FoodDTO(
                food.getId(),
                food.getName(),
                food.getDescription(),
                food.getPrice(),
                food.getImageUrl(),
                food.getStatus(),
                food.getStock(),
                food.getRatingAvg(),
                categoryId,
                categoryName,
                score == null ? 0.0 : score,
                reason
        );
    }

    private Long getCategoryId(FoodEntity food) {
        if (food == null || food.getCategory() == null) return null;
        return food.getCategory().getId();
    }

    private BigDecimal safeRating(FoodEntity food) {
        return food.getRatingAvg() != null ? food.getRatingAvg() : BigDecimal.ZERO;
    }

    private int safeQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : 1;
    }

    private boolean isAvailable(FoodEntity food) {
        if (food == null) return false;
        String status = food.getStatus();
        return status == null || status.equalsIgnoreCase("AVAILABLE") || status.equalsIgnoreCase("ACTIVE");
    }

    private boolean isPaymentIntent(String text) {
        return containsAny(text, "thanh toán", "thanh toan", "qr", "chuyển khoản", "chuyen khoan", "trả tiền", "tra tien", "payment", "pay");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(normalizeText(keyword))) return true;
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private int normalizeLimit(int limit, int min, int max) {
        if (limit < min) return min;
        return Math.min(limit, max);
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private List<Long> extractIds(List<FoodDTO> foods) {
        return foods.stream().map(FoodDTO::getId).toList();
    }

    // =========================================================
    // DTO NỘI BỘ - ĐỂ ANH CHỈ CẦN 1 FILE, KHÔNG PHẢI TÁCH DTO
    // =========================================================

    private static class FoodScore {
        private final FoodEntity food;
        private final double score;
        private final String reason;

        private FoodScore(FoodEntity food, double score) {
            this(food, score, null);
        }

        private FoodScore(FoodEntity food, double score, String reason) {
            this.food = food;
            this.score = score;
            this.reason = reason;
        }
    }

    public static class ApiResult<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResult() {
        }

        public ApiResult(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResult<T> success(String message, T data) {
            return new ApiResult<>(true, message, data);
        }

        public static <T> ApiResult<T> error(String message) {
            return new ApiResult<>(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    public static class RecommendationResponse {
        private Long userId;
        private String algorithm;
        private List<Long> recommendedFoodIds;
        private List<FoodDTO> foods;

        public RecommendationResponse() {
        }

        public RecommendationResponse(Long userId, String algorithm, List<Long> recommendedFoodIds, List<FoodDTO> foods) {
            this.userId = userId;
            this.algorithm = algorithm;
            this.recommendedFoodIds = recommendedFoodIds;
            this.foods = foods;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public List<Long> getRecommendedFoodIds() { return recommendedFoodIds; }
        public void setRecommendedFoodIds(List<Long> recommendedFoodIds) { this.recommendedFoodIds = recommendedFoodIds; }
        public List<FoodDTO> getFoods() { return foods; }
        public void setFoods(List<FoodDTO> foods) { this.foods = foods; }
    }

    public static class FoodDTO {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private String status;
        private Integer stock;
        private BigDecimal ratingAvg;
        private Long categoryId;
        private String categoryName;
        private Double aiScore;
        private String reason;

        public FoodDTO() {
        }

        public FoodDTO(Long id, String name, String description, BigDecimal price, String imageUrl,
                       String status, Integer stock, BigDecimal ratingAvg, Long categoryId, String categoryName,
                       Double aiScore, String reason) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageUrl = imageUrl;
            this.status = status;
            this.stock = stock;
            this.ratingAvg = ratingAvg;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.aiScore = aiScore;
            this.reason = reason;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public BigDecimal getRatingAvg() { return ratingAvg; }
        public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public Double getAiScore() { return aiScore; }
        public void setAiScore(Double aiScore) { this.aiScore = aiScore; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ChatBotRequest {
        private Long userId;
        private String message;
        private BigDecimal amount;
        private Long orderId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }

    public static class ChatBotResponse {
        private String answer;
        private PaymentQrDTO paymentQr;
        private List<FoodDTO> suggestedFoods;

        public ChatBotResponse() {
        }

        public ChatBotResponse(String answer, PaymentQrDTO paymentQr, List<FoodDTO> suggestedFoods) {
            this.answer = answer;
            this.paymentQr = paymentQr;
            this.suggestedFoods = suggestedFoods;
        }

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public PaymentQrDTO getPaymentQr() { return paymentQr; }
        public void setPaymentQr(PaymentQrDTO paymentQr) { this.paymentQr = paymentQr; }
        public List<FoodDTO> getSuggestedFoods() { return suggestedFoods; }
        public void setSuggestedFoods(List<FoodDTO> suggestedFoods) { this.suggestedFoods = suggestedFoods; }
    }

    public static class PaymentQrRequest {
        private BigDecimal amount;
        private String orderCode;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    }

    public static class PaymentQrDTO {
        private String bankCode;
        private String accountNo;
        private String accountName;
        private BigDecimal amount;
        private String transferContent;
        private String qrImageUrl;

        public PaymentQrDTO() {
        }

        public PaymentQrDTO(String bankCode, String accountNo, String accountName, BigDecimal amount, String transferContent, String qrImageUrl) {
            this.bankCode = bankCode;
            this.accountNo = accountNo;
            this.accountName = accountName;
            this.amount = amount;
            this.transferContent = transferContent;
            this.qrImageUrl = qrImageUrl;
        }

        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getTransferContent() { return transferContent; }
        public void setTransferContent(String transferContent) { this.transferContent = transferContent; }
        public String getQrImageUrl() { return qrImageUrl; }
        public void setQrImageUrl(String qrImageUrl) { this.qrImageUrl = qrImageUrl; }
    }

    public static class FoodDescriptionRequest {
        private String foodName;
        private String ingredients;

        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public String getIngredients() { return ingredients; }
        public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    }

    public static class FoodDescriptionResponse {
        private String description;

        public FoodDescriptionResponse() {
        }

        public FoodDescriptionResponse(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class SentimentRequest {
        private String comment;

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class SentimentResponse {
        private String label;
        private Double confidence;
        private List<String> tags;

        public SentimentResponse() {
        }

        public SentimentResponse(String label, Double confidence, List<String> tags) {
            this.label = label;
            this.confidence = confidence;
            this.tags = tags;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class ImageVerifyResponse {
        private Long foodId;
        private String foodName;
        private String fileName;
        private String contentType;
        private Long size;
        private Boolean verified;
        private Double confidence;
        private String message;

        public ImageVerifyResponse() {
        }

        public ImageVerifyResponse(Long foodId, String foodName, String fileName, String contentType, Long size,
                                   Boolean verified, Double confidence, String message) {
            this.foodId = foodId;
            this.foodName = foodName;
            this.fileName = fileName;
            this.contentType = contentType;
            this.size = size;
            this.verified = verified;
            this.confidence = confidence;
            this.message = message;
        }

        public Long getFoodId() { return foodId; }
        public void setFoodId(Long foodId) { this.foodId = foodId; }
        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public Boolean getVerified() { return verified; }
        public void setVerified(Boolean verified) { this.verified = verified; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class DemandForecastResponse {
        private LocalDate forecastDate;
        private Integer predictedOrders;
        private BigDecimal predictedRevenue;
        private String note;
        private List<FoodDemandDTO> topFoods;

        public DemandForecastResponse() {
        }

        public DemandForecastResponse(LocalDate forecastDate, Integer predictedOrders, BigDecimal predictedRevenue, String note, List<FoodDemandDTO> topFoods) {
            this.forecastDate = forecastDate;
            this.predictedOrders = predictedOrders;
            this.predictedRevenue = predictedRevenue;
            this.note = note;
            this.topFoods = topFoods;
        }

        public LocalDate getForecastDate() { return forecastDate; }
        public void setForecastDate(LocalDate forecastDate) { this.forecastDate = forecastDate; }
        public Integer getPredictedOrders() { return predictedOrders; }
        public void setPredictedOrders(Integer predictedOrders) { this.predictedOrders = predictedOrders; }
        public BigDecimal getPredictedRevenue() { return predictedRevenue; }
        public void setPredictedRevenue(BigDecimal predictedRevenue) { this.predictedRevenue = predictedRevenue; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public List<FoodDemandDTO> getTopFoods() { return topFoods; }
        public void setTopFoods(List<FoodDemandDTO> topFoods) { this.topFoods = topFoods; }
    }

    public static class FoodDemandDTO {
        private Long foodId;
        private String foodName;
        private Integer predictedQuantity;

        public FoodDemandDTO() {
        }

        public FoodDemandDTO(Long foodId, String foodName, Integer predictedQuantity) {
            this.foodId = foodId;
            this.foodName = foodName;
            this.predictedQuantity = predictedQuantity;
        }

        public Long getFoodId() { return foodId; }
        public void setFoodId(Long foodId) { this.foodId = foodId; }
        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public Integer getPredictedQuantity() { return predictedQuantity; }
        public void setPredictedQuantity(Integer predictedQuantity) { this.predictedQuantity = predictedQuantity; }
    }

    public static class ReviewInsightResponse {
        private Integer totalReviews;
        private Integer positive;
        private Integer negative;
        private Integer neutral;
        private Map<String, Integer> tags;

        public ReviewInsightResponse() {
        }

        public ReviewInsightResponse(Integer totalReviews, Integer positive, Integer negative, Integer neutral, Map<String, Integer> tags) {
            this.totalReviews = totalReviews;
            this.positive = positive;
            this.negative = negative;
            this.neutral = neutral;
            this.tags = tags;
        }

        public Integer getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }
        public Integer getPositive() { return positive; }
        public void setPositive(Integer positive) { this.positive = positive; }
        public Integer getNegative() { return negative; }
        public void setNegative(Integer negative) { this.negative = negative; }
        public Integer getNeutral() { return neutral; }
        public void setNeutral(Integer neutral) { this.neutral = neutral; }
        public Map<String, Integer> getTags() { return tags; }
        public void setTags(Map<String, Integer> tags) { this.tags = tags; }
    }
}
