package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.health.HealthAnalysisDTO;
import com.food.smart_food_system.DTO.health.HealthFoodDTO;
import com.food.smart_food_system.DTO.health.HealthProfileResponseDTO;
import com.food.smart_food_system.DTO.health.HealthRecommendationResponseDTO;
import com.food.smart_food_system.DTO.health.UpdateHealthProfileRequest;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BMI + TDEE based food recommendation service.
 *
 * Pipeline:
 *   1. Build {@link HealthAnalysisDTO} from the user's profile (BMI, BMR, TDEE,
 *      target meal calories, preferred / avoid tag sets).
 *   2. For each available food, compute a 0..100 match score based on:
 *        - distance from the target calorie window for one meal
 *        - matching the user's preferred tags
 *        - penalty for tags the user should avoid
 *        - hard filters from dietPreference / health conditions
 *        - small bonus from food rating and stock availability
 *   3. Sort and return top N.
 *
 * Optionally calls {@link AiClientService} (Gemini) to produce a natural-language
 * advice line. If the Gemini key is not configured, advice is generated locally
 * from rules so the endpoint always returns something useful.
 */
@Service
public class HealthRecommendationService {

    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final AiClientService aiClientService;

    public HealthRecommendationService(UserRepository userRepository,
                                       FoodRepository foodRepository,
                                       AiClientService aiClientService) {
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.aiClientService = aiClientService;
    }

    // =========================================================================
    // Profile CRUD
    // =========================================================================

    @Transactional(readOnly = true)
    public HealthProfileResponseDTO getProfile(String email) {
        UserEntity user = findUser(email);
        return toProfileDto(user);
    }

    @Transactional
    public HealthProfileResponseDTO updateProfile(String email, UpdateHealthProfileRequest req) {
        UserEntity user = findUser(email);
        if (req.getGender() != null)           user.setGender(req.getGender().toUpperCase());
        if (req.getDateOfBirth() != null)      user.setDateOfBirth(req.getDateOfBirth());
        if (req.getHeightCm() != null)         user.setHeightCm(req.getHeightCm());
        if (req.getWeightKg() != null)         user.setWeightKg(req.getWeightKg());
        if (req.getHealthCondition() != null)  user.setHealthCondition(req.getHealthCondition());
        if (req.getDietPreference() != null)   user.setDietPreference(req.getDietPreference().toUpperCase());
        if (req.getActivityLevel() != null)    user.setActivityLevel(req.getActivityLevel().toUpperCase());
        if (req.getGoal() != null)             user.setGoal(req.getGoal().toUpperCase());
        userRepository.save(user);
        return toProfileDto(user);
    }

    // =========================================================================
    // Analysis (BMI + TDEE)
    // =========================================================================

    @Transactional(readOnly = true)
    public HealthAnalysisDTO analyze(String email) {
        UserEntity user = findUser(email);
        return analyzeUser(user);
    }

    private HealthAnalysisDTO analyzeUser(UserEntity user) {
        HealthAnalysisDTO out = new HealthAnalysisDTO();

        BigDecimal heightCm = user.getHeightCm();
        BigDecimal weightKg = user.getWeightKg();
        LocalDate dob = user.getDateOfBirth();

        boolean hasBasic = heightCm != null && weightKg != null
                && heightCm.doubleValue() > 0 && weightKg.doubleValue() > 0;
        if (!hasBasic) {
            out.setReliable(false);
            out.setSummary("Bạn chưa cập nhật chiều cao / cân nặng. Hãy hoàn thiện hồ sơ sức khỏe để nhận gợi ý chính xác hơn.");
            out.setAvoidTags(List.of());
            out.setPreferTags(List.of());
            out.setAllergyKeywords(List.of());
            return out;
        }

        // BMI = kg / m^2
        double h = heightCm.doubleValue() / 100.0;
        double w = weightKg.doubleValue();
        double bmiRaw = w / (h * h);
        BigDecimal bmi = BigDecimal.valueOf(bmiRaw).setScale(2, RoundingMode.HALF_UP);
        out.setBmi(bmi);

        String bmiCategory;
        String bmiLabel;
        if (bmiRaw < 18.5) {
            bmiCategory = "UNDERWEIGHT";
            bmiLabel = "Thiếu cân";
        } else if (bmiRaw < 25) {
            bmiCategory = "NORMAL";
            bmiLabel = "Bình thường";
        } else if (bmiRaw < 30) {
            bmiCategory = "OVERWEIGHT";
            bmiLabel = "Thừa cân";
        } else {
            bmiCategory = "OBESE";
            bmiLabel = "Béo phì";
        }
        out.setBmiCategory(bmiCategory);
        out.setBmiCategoryLabel(bmiLabel);

        // Age - default to 25 if DOB missing
        int age = 25;
        if (dob != null) {
            age = Math.max(10, Math.min(100, Period.between(dob, LocalDate.now()).getYears()));
        }

        // Mifflin-St Jeor BMR
        boolean male = "MALE".equalsIgnoreCase(user.getGender());
        double bmr = male
                ? 10 * w + 6.25 * heightCm.doubleValue() - 5 * age + 5
                : 10 * w + 6.25 * heightCm.doubleValue() - 5 * age - 161;
        out.setBmr((int) Math.round(bmr));

        // Activity factor -> TDEE
        double activityFactor = activityFactor(user.getActivityLevel());
        double tdee = bmr * activityFactor;
        out.setTdee((int) Math.round(tdee));

        // Goal -> target daily calories
        String goal = user.getGoal() == null ? "MAINTAIN" : user.getGoal().toUpperCase();
        double target = tdee;
        switch (goal) {
            case "LOSE_WEIGHT" -> target = tdee - 500;
            case "GAIN_WEIGHT", "GAIN_MUSCLE" -> target = tdee + 300;
            default -> target = tdee;
        }
        // Override for BMI extremes
        if ("OBESE".equals(bmiCategory) && target > tdee - 300) {
            target = tdee - 500;
        }
        if ("UNDERWEIGHT".equals(bmiCategory) && target < tdee + 200) {
            target = tdee + 300;
        }
        out.setTargetDailyCalories((int) Math.round(target));

        // Assume 3 main meals, target window is ~30%..40% of daily target per meal
        int mealMin = (int) Math.round(target * 0.25);
        int mealMax = (int) Math.round(target * 0.40);
        out.setTargetMealCaloriesMin(mealMin);
        out.setTargetMealCaloriesMax(mealMax);

        // Tag preferences based on diet, goal, health conditions
        Set<String> prefer = new HashSet<>();
        Set<String> avoid = new HashSet<>();

        String diet = user.getDietPreference() == null ? "" : user.getDietPreference().toUpperCase();
        switch (diet) {
            case "VEGETARIAN" -> {
                prefer.add("VEGETARIAN");
                avoid.add("CONTAINS_SEAFOOD");
            }
            case "VEGAN" -> {
                prefer.add("VEGAN");
                avoid.add("CONTAINS_SEAFOOD");
                avoid.add("CONTAINS_DAIRY");
            }
            case "DIABETIC" -> {
                prefer.add("LOW_SUGAR");
                prefer.add("HIGH_FIBER");
                prefer.add("DIABETIC_FRIENDLY");
                avoid.add("HIGH_SUGAR");
            }
            case "LOW_SODIUM" -> {
                prefer.add("LOW_SODIUM");
                avoid.add("HIGH_SODIUM");
            }
            case "LOW_FAT" -> {
                prefer.add("LOW_FAT");
                avoid.add("HIGH_FAT");
            }
            case "KETO" -> {
                prefer.add("KETO");
                prefer.add("HIGH_FAT");
                avoid.add("HIGH_SUGAR");
            }
            case "GLUTEN_FREE" -> prefer.add("GLUTEN_FREE");
            default -> {
                // NORMAL or unknown - no diet-driven tags
            }
        }

        // Goal-driven preferences
        switch (goal) {
            case "LOSE_WEIGHT" -> {
                prefer.add("LOW_FAT");
                prefer.add("LOW_SUGAR");
                avoid.add("HIGH_SUGAR");
            }
            case "GAIN_MUSCLE" -> prefer.add("HIGH_PROTEIN");
            case "GAIN_WEIGHT" -> prefer.add("HIGH_PROTEIN");
            default -> { /* MAINTAIN - no extras */ }
        }

        // Health-condition free text scan (best-effort Vietnamese keywords)
        String condition = user.getHealthCondition();
        if (condition != null) {
            String c = condition.toLowerCase();
            if (c.contains("tiểu đường") || c.contains("tieu duong") || c.contains("diabet")) {
                prefer.add("LOW_SUGAR");
                avoid.add("HIGH_SUGAR");
            }
            if (c.contains("huyết áp") || c.contains("huyet ap") || c.contains("hyperten")) {
                prefer.add("LOW_SODIUM");
                avoid.add("HIGH_SODIUM");
            }
            if (c.contains("tim") || c.contains("cholesterol") || c.contains("mỡ máu")) {
                prefer.add("LOW_FAT");
                avoid.add("HIGH_FAT");
            }
            if (c.contains("hải sản") || c.contains("hai san")) {
                avoid.add("CONTAINS_SEAFOOD");
            }
            if (c.contains("đậu phộng") || c.contains("dau phong") || c.contains("hạt") || c.contains("nut")) {
                avoid.add("CONTAINS_NUTS");
            }
            if (c.contains("sữa") || c.contains("lactose") || c.contains("dairy")) {
                avoid.add("CONTAINS_DAIRY");
            }
        }

        // Trích từ khoá dị ứng cụ thể từ ô "tình trạng sức khoẻ / dị ứng"
        // Ví dụ: "Dị ứng thịt gà, không ăn được tôm" -> ["gà", "tôm"]
        // Các từ này sẽ được dùng để lọc món theo tên / mô tả / nguyên liệu.
        out.setAllergyKeywords(extractAllergyKeywords(condition));

        out.setPreferTags(new ArrayList<>(prefer));
        out.setAvoidTags(new ArrayList<>(avoid));
        out.setReliable(true);

        String summary = String.format(
                "BMI %s (%s). Mục tiêu khoảng %d kcal/ngày, mỗi bữa từ %d–%d kcal.",
                bmi.toPlainString(),
                bmiLabel.toLowerCase(),
                out.getTargetDailyCalories(),
                out.getTargetMealCaloriesMin(),
                out.getTargetMealCaloriesMax());
        out.setSummary(summary);

        return out;
    }

    // =========================================================================
    // Recommendation
    // =========================================================================

    /**
     * Build a health-based recommendation for the current user.
     *
     * @param email     authenticated user's email
     * @param limit     max number of foods to return (clamped to 1..30)
     * @param useAi     if true, also ask Gemini for a free-form advice paragraph
     */
    @Transactional(readOnly = true)
    public HealthRecommendationResponseDTO recommend(String email, int limit, boolean useAi) {
        UserEntity user = findUser(email);
        HealthAnalysisDTO analysis = analyzeUser(user);

        int n = Math.max(1, Math.min(30, limit));

        List<FoodEntity> allFoods = foodRepository.findAll();
        List<HealthFoodDTO> scored = allFoods.stream()
                .filter(this::isAvailable)
                .filter(food -> passesHardFilters(food, analysis))
                .map(food -> scoreFood(food, analysis))
                .sorted(Comparator.comparingDouble(HealthFoodDTO::getMatchScore).reversed())
                .limit(n)
                .collect(Collectors.toList());

        String aiAdvice = "";
        if (useAi && analysis.isReliable()) {
            aiAdvice = buildAiAdvice(user, analysis, scored);
        }

        return new HealthRecommendationResponseDTO(user.getId(), analysis, scored, aiAdvice);
    }

    private boolean passesHardFilters(FoodEntity food, HealthAnalysisDTO analysis) {
        // 1) Loại theo tag dị ứng/chế độ ăn (CONTAINS_SEAFOOD, CONTAINS_NUTS, CONTAINS_DAIRY).
        Set<String> foodTags = parseTags(food.getTags());
        Set<String> hardAvoid = Set.of(
                "CONTAINS_SEAFOOD", "CONTAINS_NUTS", "CONTAINS_DAIRY"
        );
        if (analysis.getAvoidTags() != null) {
            for (String avoid : analysis.getAvoidTags()) {
                if (hardAvoid.contains(avoid) && foodTags.contains(avoid)) {
                    return false;
                }
            }
        }

        // 2) Loại theo từ khoá dị ứng tự do (do người dùng nhập).
        //    Quét trong tên, mô tả và nguyên liệu (đã chuẩn hoá về chữ thường, không dấu).
        List<String> kws = analysis.getAllergyKeywords();
        if (kws != null && !kws.isEmpty()) {
            String haystack = normalizeForMatch(
                    safe(food.getName()) + " " + safe(food.getDescription()) + " " + safe(food.getIngredients())
            );
            for (String kw : kws) {
                String needle = normalizeForMatch(kw);
                if (!needle.isBlank() && haystack.contains(needle)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /** Chuẩn hoá tiếng Việt: bỏ dấu + chữ thường để so khớp lỏng. */
    private static String normalizeForMatch(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd');
        return n.toLowerCase().trim();
    }

    /**
     * Trích các từ khoá dị ứng/kiêng kỵ từ câu khai báo tự do của khách hàng.
     * Hỗ trợ vài cụm dẫn dắt phổ biến: "dị ứng X", "không ăn được Y", "kiêng Z".
     * Mỗi từ trả về cũng kèm dạng không dấu để khớp linh hoạt.
     */
    private static List<String> extractAllergyKeywords(String text) {
        if (text == null || text.isBlank()) return List.of();
        String src = text.toLowerCase().replace('\n', ' ').replace(';', ',');

        // Danh sách cụm dẫn dắt: nếu xuất hiện, lấy phần phía sau đến dấu chấm/phẩy tiếp theo.
        String[] cues = {
                "dị ứng", "di ung",
                "không ăn được", "khong an duoc",
                "không ăn", "khong an",
                "kiêng", "kieng",
                "tránh", "tranh",
                "no eat", "allergic to", "allergy"
        };

        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (String cue : cues) {
            int i = 0;
            while ((i = src.indexOf(cue, i)) != -1) {
                int start = i + cue.length();
                int end = start;
                while (end < src.length() && src.charAt(end) != ',' && src.charAt(end) != '.') end++;
                String chunk = src.substring(start, end).trim();
                // tách các mục bằng " và " / " hoặc " / " with "
                for (String part : chunk.split("\\s+(và|hoac|hoặc|or|and|with)\\s+|,| / ")) {
                    String p = part.trim();
                    // bỏ vài giới từ thừa đầu câu
                    p = p.replaceAll("^(với|với cả|cả|the|a |an )\\s+", "").trim();
                    if (p.length() >= 2 && p.length() <= 40) {
                        out.add(p);
                    }
                }
                i = end;
            }
        }
        return new java.util.ArrayList<>(out);
    }

    private HealthFoodDTO scoreFood(FoodEntity food, HealthAnalysisDTO analysis) {
        HealthFoodDTO dto = new HealthFoodDTO();
        dto.setFoodId(food.getId());
        dto.setName(food.getName());
        dto.setDescription(food.getDescription());
        dto.setImageUrl(food.getImageUrl());
        dto.setPrice(food.getPrice());
        if (food.getCategory() != null) dto.setCategoryName(food.getCategory().getName());
        dto.setRatingAvg(food.getRatingAvg());
        dto.setCalories(food.getCalories());
        dto.setProteinG(food.getProteinG());
        dto.setFatG(food.getFatG());
        dto.setCarbsG(food.getCarbsG());

        Set<String> tags = parseTags(food.getTags());
        Set<String> prefer = analysis.getPreferTags() == null ? Set.of()
                : new HashSet<>(analysis.getPreferTags());
        Set<String> avoid = analysis.getAvoidTags() == null ? Set.of()
                : new HashSet<>(analysis.getAvoidTags());

        double score = 50.0; // baseline

        // (1) Calorie fit: full bonus when inside the target meal window, scaled penalty otherwise.
        if (analysis.isReliable() && food.getCalories() != null && food.getCalories() > 0
                && analysis.getTargetMealCaloriesMin() != null
                && analysis.getTargetMealCaloriesMax() != null) {
            int cal = food.getCalories();
            int min = analysis.getTargetMealCaloriesMin();
            int max = analysis.getTargetMealCaloriesMax();
            if (cal >= min && cal <= max) {
                score += 25;
            } else {
                int gap = cal < min ? (min - cal) : (cal - max);
                // every 100 kcal away costs 6 points, capped at 25
                double penalty = Math.min(25, (gap / 100.0) * 6.0);
                score += 25 - penalty;
            }
        }

        // (2) Tag matching
        List<String> matched = new ArrayList<>();
        for (String t : tags) {
            if (prefer.contains(t)) {
                score += 6;
                matched.add(t);
            }
            if (avoid.contains(t)) {
                score -= 8;
            }
        }
        dto.setMatchedTags(matched);

        // (3) Rating bonus (0..5 stars -> 0..5 points)
        if (food.getRatingAvg() != null) {
            score += Math.min(5.0, food.getRatingAvg().doubleValue());
        }

        // (4) Stock bonus
        if (food.getStock() != null && food.getStock() > 0) {
            score += 1;
        }

        // Clamp to 0..100
        score = Math.max(0, Math.min(100, score));
        dto.setMatchScore(Math.round(score * 10.0) / 10.0);
        dto.setReason(buildReason(food, analysis, matched));

        return dto;
    }

    private String buildReason(FoodEntity food, HealthAnalysisDTO analysis, List<String> matched) {
        List<String> bits = new ArrayList<>();
        if (food.getCalories() != null && analysis.getTargetMealCaloriesMin() != null
                && analysis.getTargetMealCaloriesMax() != null
                && food.getCalories() >= analysis.getTargetMealCaloriesMin()
                && food.getCalories() <= analysis.getTargetMealCaloriesMax()) {
            bits.add("calo phù hợp khẩu phần (" + food.getCalories() + " kcal)");
        }
        if (!matched.isEmpty()) {
            bits.add("đáp ứng tiêu chí " + String.join(", ", matched.stream().map(this::tagLabel).toList()));
        }
        if (food.getRatingAvg() != null && food.getRatingAvg().doubleValue() >= 4.0) {
            bits.add("được đánh giá tốt (" + food.getRatingAvg() + "★)");
        }
        if (bits.isEmpty()) {
            return "Phù hợp tổng thể với hồ sơ sức khỏe của bạn.";
        }
        return "Gợi ý vì " + String.join("; ", bits) + ".";
    }

    private String tagLabel(String tag) {
        return switch (tag) {
            case "HIGH_PROTEIN" -> "giàu đạm";
            case "LOW_FAT" -> "ít béo";
            case "LOW_SUGAR" -> "ít đường";
            case "LOW_SODIUM" -> "ít muối";
            case "HIGH_FIBER" -> "nhiều chất xơ";
            case "VEGETARIAN" -> "chay";
            case "VEGAN" -> "thuần chay";
            case "KETO" -> "keto";
            case "GLUTEN_FREE" -> "không gluten";
            case "DIABETIC_FRIENDLY" -> "phù hợp tiểu đường";
            default -> tag.toLowerCase().replace('_', ' ');
        };
    }

    private String buildAiAdvice(UserEntity user, HealthAnalysisDTO analysis, List<HealthFoodDTO> foods) {
        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < Math.min(5, foods.size()); i++) {
            HealthFoodDTO f = foods.get(i);
            menu.append(String.format("- %s (%d kcal)%n",
                    f.getName(),
                    f.getCalories() == null ? 0 : f.getCalories()));
        }

        String prompt = String.format(
                """
                Bạn là chuyên gia dinh dưỡng. Hãy viết một đoạn tư vấn ngắn (3-5 câu) bằng tiếng Việt,
                lịch sự, dành cho người dùng dưới đây. Không cần lặp lại số liệu, chỉ đưa lời khuyên
                thực tế và động viên.

                Thông tin người dùng:
                - BMI: %s (%s)
                - Mục tiêu calo / ngày: %d kcal
                - Tình trạng sức khỏe: %s
                - Chế độ ăn: %s
                - Mục tiêu: %s

                Hệ thống đã chọn các món sau cho bữa kế tiếp:
                %s

                Hãy giải thích vì sao các món này phù hợp và đưa thêm 1-2 lời khuyên kèm theo
                (ví dụ: nên uống nhiều nước, nên đi bộ sau ăn, hạn chế đồ chiên...).
                """,
                analysis.getBmi(),
                analysis.getBmiCategoryLabel(),
                analysis.getTargetDailyCalories(),
                nvl(user.getHealthCondition(), "không có"),
                nvl(user.getDietPreference(), "bình thường"),
                nvl(user.getGoal(), "duy trì"),
                menu.toString().trim()
        );

        try {
            String answer = aiClientService.ask(prompt);
            if (answer == null || answer.isBlank() || answer.equals(prompt)) {
                return localAdvice(analysis);
            }
            return answer.trim();
        } catch (Exception e) {
            return localAdvice(analysis);
        }
    }

    private String localAdvice(HealthAnalysisDTO analysis) {
        return switch (analysis.getBmiCategory() == null ? "" : analysis.getBmiCategory()) {
            case "UNDERWEIGHT" -> "Bạn đang thiếu cân. Ưu tiên các món giàu đạm, đủ tinh bột tốt và bổ sung bữa phụ. Đừng quên uống đủ nước và ngủ đủ giấc.";
            case "OVERWEIGHT" -> "Bạn đang thừa cân. Ưu tiên món ít đường, ít béo, nhiều rau xanh. Cố gắng đi bộ ít nhất 30 phút mỗi ngày.";
            case "OBESE" -> "Chỉ số BMI ở mức béo phì. Giảm khẩu phần tinh bột và đồ chiên rán, ưu tiên các món hấp/luộc. Cân nhắc gặp bác sĩ dinh dưỡng để có kế hoạch chi tiết.";
            case "NORMAL" -> "Bạn đang ở vùng cân nặng lý tưởng. Duy trì khẩu phần cân bằng, ăn đa dạng và vận động thường xuyên là đủ.";
            default -> "Hãy cập nhật hồ sơ sức khỏe đầy đủ để nhận tư vấn cá nhân hóa.";
        };
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private double activityFactor(String activityLevel) {
        if (activityLevel == null) return 1.375;
        return switch (activityLevel.toUpperCase()) {
            case "SEDENTARY" -> 1.2;
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            case "VERY_ACTIVE" -> 1.9;
            default -> 1.375;
        };
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return Set.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private boolean isAvailable(FoodEntity food) {
        if (food == null) return false;
        String s = food.getStatus();
        boolean active = s == null || s.equalsIgnoreCase("AVAILABLE") || s.equalsIgnoreCase("ACTIVE");
        boolean stocked = food.getStock() == null || food.getStock() > 0;
        return active && stocked;
    }

    private UserEntity findUser(String email) {
        Optional<UserEntity> opt = userRepository.findByEmail(email);
        return opt.orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
    }

    private HealthProfileResponseDTO toProfileDto(UserEntity user) {
        HealthProfileResponseDTO dto = new HealthProfileResponseDTO();
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setDateOfBirth(user.getDateOfBirth());
        if (user.getDateOfBirth() != null) {
            dto.setAge(Period.between(user.getDateOfBirth(), LocalDate.now()).getYears());
        }
        dto.setHeightCm(user.getHeightCm());
        dto.setWeightKg(user.getWeightKg());
        dto.setHealthCondition(user.getHealthCondition());
        dto.setDietPreference(user.getDietPreference());
        dto.setActivityLevel(user.getActivityLevel());
        dto.setGoal(user.getGoal());
        dto.setProfileComplete(user.getHeightCm() != null
                && user.getWeightKg() != null
                && user.getDateOfBirth() != null
                && user.getGender() != null);
        return dto;
    }

    private String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
