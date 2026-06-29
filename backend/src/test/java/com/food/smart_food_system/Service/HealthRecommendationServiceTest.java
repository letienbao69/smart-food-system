package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.health.HealthAnalysisDTO;
import com.food.smart_food_system.DTO.health.HealthRecommendationResponseDTO;
import com.food.smart_food_system.Entity.CategoryEntity;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies the BMI / TDEE math and basic food scoring logic for the AI
 * recommendation feature. Uses plain Mockito - no Spring context needed.
 */
class HealthRecommendationServiceTest {

    private UserRepository userRepository;
    private FoodRepository foodRepository;
    private AiClientService aiClientService;
    private HealthRecommendationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        foodRepository = mock(FoodRepository.class);
        aiClientService = mock(AiClientService.class);
        service = new HealthRecommendationService(userRepository, foodRepository, aiClientService);
    }

    @Test
    @DisplayName("BMI 22.86 -> NORMAL for 70kg / 175cm")
    void bmi_classifies_normal() {
        UserEntity user = buildUser("MALE", 30, 175, 70, "MODERATE", "NORMAL", "MAINTAIN", null);
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        HealthAnalysisDTO analysis = service.analyze("u@x");

        assertEquals("NORMAL", analysis.getBmiCategory());
        assertEquals(new BigDecimal("22.86"), analysis.getBmi());
        assertTrue(analysis.isReliable());
    }

    @Test
    @DisplayName("BMI 31.25 -> OBESE for 90kg / 170cm; target calories cut")
    void bmi_classifies_obese_and_cuts_calories() {
        UserEntity user = buildUser("MALE", 35, 170, 90, "SEDENTARY", "NORMAL", "MAINTAIN", null);
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        HealthAnalysisDTO analysis = service.analyze("u@x");

        assertEquals("OBESE", analysis.getBmiCategory());
        // Even though goal=MAINTAIN, OBESE BMI forces a cut: target < tdee - 300
        assertTrue(analysis.getTargetDailyCalories() < analysis.getTdee() - 300,
                "Target " + analysis.getTargetDailyCalories() + " should be below TDEE " + analysis.getTdee() + " - 300");
    }

    @Test
    @DisplayName("UNDERWEIGHT pushes target calories up by at least 200 kcal over TDEE")
    void bmi_classifies_underweight_and_adds_calories() {
        UserEntity user = buildUser("FEMALE", 22, 165, 45, "LIGHT", "NORMAL", "MAINTAIN", null);
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        HealthAnalysisDTO analysis = service.analyze("u@x");

        assertEquals("UNDERWEIGHT", analysis.getBmiCategory());
        assertTrue(analysis.getTargetDailyCalories() >= analysis.getTdee() + 200);
    }

    @Test
    @DisplayName("Missing height/weight -> reliable=false, no crash")
    void missing_basic_data_does_not_crash() {
        UserEntity user = new UserEntity();
        user.setEmail("u@x");
        user.setFullName("Test");
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        HealthAnalysisDTO analysis = service.analyze("u@x");

        assertFalse(analysis.isReliable());
        assertNotNull(analysis.getSummary());
    }

    @Test
    @DisplayName("Diabetic preference and goal=LOSE_WEIGHT add LOW_SUGAR to prefer tags")
    void diet_preference_drives_prefer_tags() {
        UserEntity user = buildUser("MALE", 40, 170, 85, "SEDENTARY", "DIABETIC", "LOSE_WEIGHT", null);
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        HealthAnalysisDTO analysis = service.analyze("u@x");

        assertTrue(analysis.getPreferTags().contains("LOW_SUGAR"));
        assertTrue(analysis.getAvoidTags().contains("HIGH_SUGAR"));
    }

    @Test
    @DisplayName("Recommender hard-filters out seafood when user is allergic")
    void recommender_filters_allergen() {
        UserEntity user = buildUser("FEMALE", 28, 160, 55, "MODERATE", "NORMAL", "MAINTAIN", "dị ứng hải sản");
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        FoodEntity seafood = food("Cá hồi", 400, "CONTAINS_SEAFOOD,HIGH_PROTEIN");
        FoodEntity vegetarian = food("Đậu hũ", 380, "VEGETARIAN,HIGH_PROTEIN");
        when(foodRepository.findAll()).thenReturn(List.of(seafood, vegetarian));

        HealthRecommendationResponseDTO result = service.recommend("u@x", 10, false);

        assertEquals(1, result.getRecommendations().size());
        assertEquals("Đậu hũ", result.getRecommendations().get(0).getName());
    }

    @Test
    @DisplayName("Recommender ranks calorie-fitting + tag-matching food higher")
    void recommender_ranks_better_match_first() {
        UserEntity user = buildUser("MALE", 30, 175, 75, "MODERATE", "NORMAL", "LOSE_WEIGHT", null);
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));

        // Target for a MODERATE 75kg/175cm male is roughly ~2700 TDEE; LOSE_WEIGHT -> ~2200/day.
        // Per-meal window: ~25-40% of 2200 = roughly 550..880 kcal.
        FoodEntity good = food("Salad gà", 600, "LOW_FAT,LOW_SUGAR,HIGH_PROTEIN");
        FoodEntity bad = food("Bánh ngọt 1500kcal", 1500, "HIGH_SUGAR,HIGH_FAT");
        when(foodRepository.findAll()).thenReturn(List.of(bad, good));

        HealthRecommendationResponseDTO result = service.recommend("u@x", 10, false);

        assertEquals(2, result.getRecommendations().size());
        assertEquals("Salad gà", result.getRecommendations().get(0).getName(),
                "calorie-fitting + tag-matching food should rank first");
        assertTrue(result.getRecommendations().get(0).getMatchScore()
                > result.getRecommendations().get(1).getMatchScore());
    }

    @Test
    @DisplayName("Recommender does not call AI when useAi=false")
    void recommender_skips_ai_when_disabled() {
        UserEntity user = buildUser("MALE", 30, 175, 70, "MODERATE", "NORMAL", "MAINTAIN", null);
        when(userRepository.findByEmail("u@x")).thenReturn(Optional.of(user));
        when(foodRepository.findAll()).thenReturn(List.of(food("Cơm gà", 500, "HIGH_PROTEIN")));

        HealthRecommendationResponseDTO result = service.recommend("u@x", 5, false);

        assertEquals("", result.getAiAdvice());
        verify(aiClientService, never()).ask(any());
    }

    // -------- helpers --------

    private UserEntity buildUser(String gender, int age, double heightCm, double weightKg,
                                 String activity, String diet, String goal, String condition) {
        UserEntity u = new UserEntity();
        u.setEmail("u@x");
        u.setFullName("Test User");
        u.setGender(gender);
        u.setDateOfBirth(LocalDate.now().minusYears(age));
        u.setHeightCm(BigDecimal.valueOf(heightCm));
        u.setWeightKg(BigDecimal.valueOf(weightKg));
        u.setActivityLevel(activity);
        u.setDietPreference(diet);
        u.setGoal(goal);
        u.setHealthCondition(condition);
        return u;
    }

    private FoodEntity food(String name, int calories, String tags) {
        FoodEntity f = new FoodEntity();
        f.setId((long) (name.hashCode() & 0xFFFF));
        f.setName(name);
        f.setPrice(BigDecimal.valueOf(50000));
        f.setStock(10);
        f.setStatus("AVAILABLE");
        f.setCalories(calories);
        f.setTags(tags);
        f.setRatingAvg(BigDecimal.valueOf(4.5));
        CategoryEntity cat = new CategoryEntity();
        cat.setId(1L);
        cat.setName("Đồ ăn");
        f.setCategory(cat);
        return f;
    }
}
