package com.food.smart_food_system.DTO.health;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of BMI + TDEE analysis based on the user's health profile.
 */
public class HealthAnalysisDTO {

    /** BMI rounded to 2 decimals (kg/m^2). */
    private BigDecimal bmi;

    /** UNDERWEIGHT / NORMAL / OVERWEIGHT / OBESE */
    private String bmiCategory;

    /** Short Vietnamese description of the BMI category. */
    private String bmiCategoryLabel;

    /** Basal Metabolic Rate, kcal/day. */
    private Integer bmr;

    /** Total Daily Energy Expenditure, kcal/day. */
    private Integer tdee;

    /** Target daily calories given the user's goal. */
    private Integer targetDailyCalories;

    /** Suggested calorie window for ONE meal. */
    private Integer targetMealCaloriesMin;
    private Integer targetMealCaloriesMax;

    /** Tags this user should avoid (e.g. HIGH_SUGAR if diabetic). */
    private List<String> avoidTags;

    /** Tags this user should prefer (e.g. HIGH_PROTEIN if gaining muscle). */
    private List<String> preferTags;

    /**
     * Từ khoá dị ứng / kiêng kỵ trích từ ô "tình trạng sức khỏe / dị ứng"
     * của khách (ví dụ: ["gà", "tôm"]). Bất kỳ món nào có tên, mô tả hoặc
     * thành phần chứa các từ này sẽ bị loại khỏi danh sách gợi ý.
     */
    private List<String> allergyKeywords;

    /** Friendly summary line, e.g. "BMI 26.1 - thừa cân. Nên giảm 500 kcal/ngày." */
    private String summary;

    /** Whether the analysis is reliable (i.e. profile has height + weight + DOB). */
    private boolean reliable;

    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }

    public String getBmiCategory() { return bmiCategory; }
    public void setBmiCategory(String bmiCategory) { this.bmiCategory = bmiCategory; }

    public String getBmiCategoryLabel() { return bmiCategoryLabel; }
    public void setBmiCategoryLabel(String bmiCategoryLabel) { this.bmiCategoryLabel = bmiCategoryLabel; }

    public Integer getBmr() { return bmr; }
    public void setBmr(Integer bmr) { this.bmr = bmr; }

    public Integer getTdee() { return tdee; }
    public void setTdee(Integer tdee) { this.tdee = tdee; }

    public Integer getTargetDailyCalories() { return targetDailyCalories; }
    public void setTargetDailyCalories(Integer targetDailyCalories) { this.targetDailyCalories = targetDailyCalories; }

    public Integer getTargetMealCaloriesMin() { return targetMealCaloriesMin; }
    public void setTargetMealCaloriesMin(Integer targetMealCaloriesMin) { this.targetMealCaloriesMin = targetMealCaloriesMin; }

    public Integer getTargetMealCaloriesMax() { return targetMealCaloriesMax; }
    public void setTargetMealCaloriesMax(Integer targetMealCaloriesMax) { this.targetMealCaloriesMax = targetMealCaloriesMax; }

    public List<String> getAvoidTags() { return avoidTags; }
    public void setAvoidTags(List<String> avoidTags) { this.avoidTags = avoidTags; }

    public List<String> getPreferTags() { return preferTags; }
    public void setPreferTags(List<String> preferTags) { this.preferTags = preferTags; }

    public List<String> getAllergyKeywords() { return allergyKeywords; }
    public void setAllergyKeywords(List<String> allergyKeywords) { this.allergyKeywords = allergyKeywords; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isReliable() { return reliable; }
    public void setReliable(boolean reliable) { this.reliable = reliable; }
}
