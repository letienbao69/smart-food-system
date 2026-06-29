package com.food.smart_food_system.DTO.health;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of PUT /api/health/profile.
 * All fields optional - service merges only non-null values onto the user.
 */
public class UpdateHealthProfileRequest {

    /** MALE / FEMALE / OTHER */
    private String gender;

    private LocalDate dateOfBirth;

    @DecimalMin(value = "50.0", message = "Chiều cao phải >= 50cm")
    @DecimalMax(value = "250.0", message = "Chiều cao phải <= 250cm")
    private BigDecimal heightCm;

    @DecimalMin(value = "20.0", message = "Cân nặng phải >= 20kg")
    @DecimalMax(value = "300.0", message = "Cân nặng phải <= 300kg")
    private BigDecimal weightKg;

    private String healthCondition;

    /** NORMAL / VEGETARIAN / VEGAN / DIABETIC / LOW_SODIUM / LOW_FAT / KETO / GLUTEN_FREE */
    private String dietPreference;

    /** SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE */
    private String activityLevel;

    /** LOSE_WEIGHT / MAINTAIN / GAIN_WEIGHT / GAIN_MUSCLE */
    private String goal;

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }

    public String getDietPreference() { return dietPreference; }
    public void setDietPreference(String dietPreference) { this.dietPreference = dietPreference; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
}
