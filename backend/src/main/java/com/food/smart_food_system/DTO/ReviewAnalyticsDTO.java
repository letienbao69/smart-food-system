package com.food.smart_food_system.DTO;

public class ReviewAnalyticsDTO {

    private long totalReviews;
    private long positive;
    private long negative;
    private long neutral;

    private double positiveRate;
    private double negativeRate;
    private double neutralRate;

    private double averageRating;

    public ReviewAnalyticsDTO() {
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public long getPositive() {
        return positive;
    }

    public void setPositive(long positive) {
        this.positive = positive;
    }

    public long getNegative() {
        return negative;
    }

    public void setNegative(long negative) {
        this.negative = negative;
    }

    public long getNeutral() {
        return neutral;
    }

    public void setNeutral(long neutral) {
        this.neutral = neutral;
    }

    public double getPositiveRate() {
        return positiveRate;
    }

    public void setPositiveRate(double positiveRate) {
        this.positiveRate = positiveRate;
    }

    public double getNegativeRate() {
        return negativeRate;
    }

    public void setNegativeRate(double negativeRate) {
        this.negativeRate = negativeRate;
    }

    public double getNeutralRate() {
        return neutralRate;
    }

    public void setNeutralRate(double neutralRate) {
        this.neutralRate = neutralRate;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
}