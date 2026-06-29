package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.Entity.ChatbotLogEntity;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Repository.ChatbotLogRepository;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.UserRepository;
import com.food.smart_food_system.DTO.chatbot.ChatResponse;
import com.food.smart_food_system.DTO.chatbot.ChatSuggestedFood;
import com.food.smart_food_system.Service.AiClientService;
import com.food.smart_food_system.Service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Chatbot service that augments the bare Gemini call with Smart Food context:
 *   - a system-style instruction telling the model its role
 *   - the user's health profile (BMI, allergies, goals) when available
 *   - a compact menu summary so the bot can recommend real dishes from our DB
 *
 * This turns the chatbot from a generic LLM into a domain assistant.
 */
@Service
@RequiredArgsConstructor
public class ChatbotServiceimpl implements ChatbotService {

    private final ChatbotLogRepository chatbotLogRepository;
    private final AiClientService aiClientService;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    private static final String SYSTEM_PROMPT = """
        Bạn là "Trợ lý dinh dưỡng Smart Food" — chuyên gia tư vấn món ăn theo sức khỏe,
        thân thiện và hiểu biết. Bạn đang trò chuyện trực tiếp với khách trên website nhà hàng.

        NGUYÊN TẮC TRẢ LỜI:
        - Trả lời ĐẦY ĐỦ, có cấu trúc và đi vào trọng tâm câu hỏi. KHÔNG được trả lời cộc lốc.
        - Khi khách hỏi liệt kê / so sánh / gợi ý nhiều món, hãy LIỆT KÊ THẲNG bằng gạch đầu dòng,
          mỗi món một dòng theo định dạng:
            "- **Tên món** — … kcal · đạm …g · giá …đ — (lý do gợi ý 1 câu)"
          Số lượng món tuỳ theo câu hỏi: hỏi "vài món" => 3-5 món; hỏi "tất cả các món < X kcal"
          hoặc "liệt kê các món chay" => liệt kê HẾT các món phù hợp đang có trong menu (đừng cắt bớt).
        - Khi khách hỏi gợi ý theo CÂN NẶNG / mục tiêu (giảm cân, tăng cơ, duy trì), hãy:
            1) Nêu ngắn gọn nhu cầu calo/đạm phù hợp dựa trên hồ sơ (nếu có) hoặc ước lượng nhanh.
            2) Sau đó liệt kê 4-6 món phù hợp từ menu, kèm kcal/đạm.
        - Khi khách hỏi theo TỪ KHOÁ (vd "món gà", "món chay", "ít calo", "nhiều đạm"),
          quét MENU HIỆN TẠI và liệt kê đầy đủ các món khớp; nếu có nhiều, sắp xếp theo độ phù hợp.
        - Luôn dùng HỒ SƠ NGƯỜI DÙNG (nếu có) để cá nhân hoá: giảm cân -> ưu tiên ít kcal/ít béo;
          tăng cơ -> ưu tiên giàu đạm; tiểu đường -> ít đường; cao huyết áp -> ít muối; ăn chay -> món chay.
          NẾU khách có DỊ ỨNG / KIÊNG KỴ, TUYỆT ĐỐI không gợi ý món chứa thành phần đó.
        - CHỈ giới thiệu món có trong MENU HIỆN TẠI. Nếu khách hỏi món không có, nói thật và gợi ý
          món tương tự đang có.
        - Có thể giải thích nhanh các khái niệm dinh dưỡng (BMI, BMR, TDEE, kcal, đạm, carb, béo)
          khi được hỏi — giải thích đủ ý chứ không dài dòng.
        - Lịch sự từ chối các câu hỏi ngoài lĩnh vực ẩm thực / dinh dưỡng / nhà hàng.
        - Dùng tiếng Việt tự nhiên. Chỉ dùng "- " cho gạch đầu dòng và "**chữ in đậm**" cho điểm nhấn;
          không dùng bảng / heading / markdown phức tạp.
        """;

    @Override
    public ChatResponse chat(Long userId, String userMessage) {
        String prompt = buildPrompt(userId, userMessage);
        String aiResponse = aiClientService.ask(prompt);

        // Defensive: if AI returned the user's original message (e.g. on error),
        // give a clearer fallback so the FE doesn't echo nonsense.
        if (aiResponse == null || aiResponse.isBlank() || aiResponse.trim().equals(userMessage.trim())) {
            aiResponse = "Xin lỗi, hệ thống AI đang bận. Bạn có thể thử lại sau ít phút " +
                         "hoặc duyệt menu trực tiếp tại trang Thực đơn nhé!";
        }

        ChatbotLogEntity log = ChatbotLogEntity.builder()
                .userId(userId)
                .message(userMessage)
                .response(aiResponse)
                .build();

        ChatbotLogEntity saved = chatbotLogRepository.save(log);

        return ChatResponse.builder()
                .logId(saved.getId())
                .message(userMessage)
                .answer(aiResponse)
                .suggestedFoods(extractMentionedFoods(aiResponse))
                .build();
    }

    /**
     * Dò trong câu trả lời của AI xem có nhắc tới món nào trong thực đơn không,
     * trả về danh sách món đó (kèm ảnh) để giao diện hiển thị card bấm được.
     * So khớp không phân biệt hoa thường và bỏ dấu tiếng Việt.
     */
    private java.util.List<ChatSuggestedFood> extractMentionedFoods(String answer) {
        if (answer == null || answer.isBlank()) return java.util.List.of();
        String haystack = normalizeVi(answer);

        java.util.LinkedHashMap<Long, ChatSuggestedFood> found = new java.util.LinkedHashMap<>();
        for (FoodEntity f : foodRepository.findAll()) {
            if (f.getName() == null) continue;
            if (f.getStatus() != null && f.getStatus().equalsIgnoreCase("HIDDEN")) continue;
            String needle = normalizeVi(f.getName());
            // bỏ qua tên quá ngắn để tránh khớp nhầm
            if (needle.length() < 4) continue;
            if (haystack.contains(needle) && !found.containsKey(f.getId())) {
                found.put(f.getId(), ChatSuggestedFood.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .imageUrl(f.getImageUrl())
                        .price(f.getPrice())
                        .categoryName(f.getCategory() != null ? f.getCategory().getName() : null)
                        .calories(f.getCalories())
                        .build());
            }
            if (found.size() >= 6) break;
        }
        return new java.util.ArrayList<>(found.values());
    }

    /** Chuẩn hoá tiếng Việt: bỏ dấu + chữ thường để so khớp lỏng. */
    private static String normalizeVi(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd');
        return n.toLowerCase().trim();
    }

    /**
     * Builds the full prompt to send to Gemini. Includes system instruction,
     * user's health profile (if known), and a compact menu snapshot.
     */
    private String buildPrompt(Long userId, String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");

        // Health profile context
        if (userId != null) {
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserEntity u = userOpt.get();
                String profile = buildProfileContext(u);
                if (!profile.isBlank()) {
                    sb.append("HỒ SƠ NGƯỜI DÙNG:\n").append(profile).append("\n\n");
                }
            }
        }

        // Menu context — ưu tiên các món liên quan đến câu hỏi để không bị cắt mất
        sb.append("MENU HIỆN TẠI (chỉ giới thiệu món có trong danh sách này):\n");
        List<FoodEntity> foods = foodRepository.findAll().stream()
                .filter(f -> f.getStatus() == null || "AVAILABLE".equalsIgnoreCase(f.getStatus()))
                .collect(Collectors.toList());
        String kw = userMessage == null ? "" : userMessage.toLowerCase();
        // Sắp xếp: món có tên/tags trùng từ khóa câu hỏi lên đầu, sau đó tới các món còn lại
        foods.sort((a, b) -> Integer.compare(relevance(b, kw), relevance(a, kw)));
        String menuSnippet = foods.stream()
                .limit(120)
                .map(this::formatFood)
                .collect(Collectors.joining("\n"));
        sb.append(menuSnippet).append("\n\n");

        // Lịch sử hội thoại gần đây để bot nhớ ngữ cảnh
        if (userId != null) {
            List<ChatbotLogEntity> recent = chatbotLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (recent != null && !recent.isEmpty()) {
                // lấy tối đa 4 lượt gần nhất, đảo lại theo thứ tự thời gian tăng dần
                List<ChatbotLogEntity> lastFew = recent.stream().limit(4).collect(Collectors.toList());
                java.util.Collections.reverse(lastFew);
                sb.append("LỊCH SỬ TRÒ CHUYỆN GẦN ĐÂY (để bạn nhớ ngữ cảnh):\n");
                for (ChatbotLogEntity l : lastFew) {
                    if (l.getMessage() != null) sb.append("Khách: ").append(l.getMessage()).append("\n");
                    if (l.getResponse() != null) sb.append("Trợ lý: ").append(l.getResponse()).append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("CÂU HỎI MỚI CỦA KHÁCH:\n").append(userMessage);
        sb.append("\n\nHãy trả lời theo đúng nguyên tắc ở đầu prompt: ĐẦY ĐỦ, có cấu trúc, "
                + "liệt kê thẳng các món phù hợp từ MENU HIỆN TẠI, và cá nhân hoá theo HỒ SƠ NGƯỜI DÙNG.");

        return sb.toString();
    }

    // Điểm liên quan đơn giản: tên/tags món khớp với từ trong câu hỏi
    private int relevance(FoodEntity f, String kw) {
        if (kw == null || kw.isBlank()) return 0;
        int score = 0;
        String name = f.getName() == null ? "" : f.getName().toLowerCase();
        String tags = f.getTags() == null ? "" : f.getTags().toLowerCase();
        for (String token : kw.split("\\s+")) {
            if (token.length() < 2) continue;
            if (name.contains(token)) score += 3;
            if (tags.contains(token)) score += 2;
        }
        // ưu tiên nhẹ món có calo (dữ liệu đầy đủ hơn)
        if (f.getCalories() != null && f.getCalories() > 0) score += 1;
        return score;
    }

    private String buildProfileContext(UserEntity u) {
        StringBuilder sb = new StringBuilder();
        if (u.getHeightCm() != null && u.getWeightKg() != null) {
            double h = u.getHeightCm().doubleValue() / 100.0;
            double w = u.getWeightKg().doubleValue();
            double bmi = w / (h * h);
            sb.append(String.format("- Chiều cao: %.0fcm, Cân nặng: %.1fkg, BMI: %.1f%n",
                    u.getHeightCm().doubleValue(), w, bmi));

            // Ước lượng nhanh BMR/TDEE/nhu cầu calo mục tiêu để bot tư vấn theo nhu cầu
            int age = 25;
            if (u.getDateOfBirth() != null) {
                age = java.time.Period.between(u.getDateOfBirth(), java.time.LocalDate.now()).getYears();
                if (age <= 0) age = 25;
            }
            boolean isMale = u.getGender() != null
                    && (u.getGender().equalsIgnoreCase("MALE") || u.getGender().equalsIgnoreCase("M") || u.getGender().equalsIgnoreCase("nam"));
            double bmr = isMale
                    ? 10 * w + 6.25 * u.getHeightCm().doubleValue() - 5 * age + 5
                    : 10 * w + 6.25 * u.getHeightCm().doubleValue() - 5 * age - 161;
            double factor = activityFactor(u.getActivityLevel());
            double tdee = bmr * factor;
            int target = (int) Math.round(tdee);
            String goal = u.getGoal();
            if ("LOSE_WEIGHT".equalsIgnoreCase(goal)) target -= 500;
            else if ("GAIN_MUSCLE".equalsIgnoreCase(goal) || "GAIN_WEIGHT".equalsIgnoreCase(goal)) target += 300;
            sb.append(String.format("- Tuổi: %d, mức vận động: %s%n", age,
                    u.getActivityLevel() == null ? "trung bình" : u.getActivityLevel()));
            sb.append(String.format("- BMR ≈ %.0f kcal/ngày, TDEE ≈ %.0f kcal/ngày, calo mục tiêu ≈ %d kcal/ngày%n",
                    bmr, tdee, target));
            sb.append(String.format("- Gợi ý mỗi bữa: %d–%d kcal%n",
                    (int) Math.round(target * 0.25), (int) Math.round(target * 0.40)));
        }
        if (u.getGoal() != null && !u.getGoal().isBlank()) {
            sb.append("- Mục tiêu: ").append(u.getGoal()).append("\n");
        }
        if (u.getHealthCondition() != null && !u.getHealthCondition().isBlank()) {
            sb.append("- Tình trạng sức khỏe / dị ứng: ").append(u.getHealthCondition()).append("\n");
        }
        if (u.getDietPreference() != null && !u.getDietPreference().isBlank()) {
            sb.append("- Chế độ ăn: ").append(u.getDietPreference()).append("\n");
        }
        return sb.toString();
    }

    private static double activityFactor(String level) {
        if (level == null) return 1.375;
        return switch (level.toUpperCase()) {
            case "SEDENTARY" -> 1.2;
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            case "VERY_ACTIVE" -> 1.9;
            default -> 1.375;
        };
    }

    private String formatFood(FoodEntity f) {
        StringBuilder sb = new StringBuilder("- ").append(f.getName());
        sb.append(" |");
        if (f.getPrice() != null) sb.append(" giá=").append(f.getPrice().intValue()).append("đ");
        if (f.getCalories() != null && f.getCalories() > 0) sb.append(" · kcal=").append(f.getCalories());
        if (f.getProteinG() != null) sb.append(" · đạm=").append(f.getProteinG()).append("g");
        if (f.getFatG() != null)     sb.append(" · béo=").append(f.getFatG()).append("g");
        if (f.getCarbsG() != null)   sb.append(" · carb=").append(f.getCarbsG()).append("g");
        if (f.getCategory() != null) sb.append(" · danh mục=").append(f.getCategory().getName());
        if (f.getTags() != null && !f.getTags().isBlank()) {
            sb.append(" · tags=").append(f.getTags());
        }
        return sb.toString();
    }
}
