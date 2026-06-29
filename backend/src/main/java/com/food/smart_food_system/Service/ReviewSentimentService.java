package com.food.smart_food_system.Service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewSentimentService {

    private static final List<String> POSITIVE_WORDS = List.of(
            "ngon", "tuyệt", "tốt", "hài lòng", "nhanh", "sạch",
            "thơm", "ổn", "xuất sắc", "rất thích", "ưng", "chất lượng"
    );

    private static final List<String> NEGATIVE_WORDS = List.of(
            "dở", "tệ", "chậm", "lạnh", "nguội", "không ngon",
            "thất vọng", "bẩn", "mặn", "nhạt", "hỏng", "kém"
    );

    public String analyze(String comment, Integer rating) {
        if (rating != null) {
            if (rating >= 4) {
                return "POSITIVE";
            }

            if (rating <= 2) {
                return "NEGATIVE";
            }
        }

        if (comment == null || comment.isBlank()) {
            return "NEUTRAL";
        }

        String text = comment.toLowerCase();

        int positiveScore = 0;
        int negativeScore = 0;

        for (String word : POSITIVE_WORDS) {
            if (text.contains(word)) {
                positiveScore++;
            }
        }

        for (String word : NEGATIVE_WORDS) {
            if (text.contains(word)) {
                negativeScore++;
            }
        }

        if (positiveScore > negativeScore) {
            return "POSITIVE";
        }

        if (negativeScore > positiveScore) {
            return "NEGATIVE";
        }

        return "NEUTRAL";
    }
}