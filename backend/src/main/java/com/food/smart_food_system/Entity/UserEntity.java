package com.food.smart_food_system.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "address", length = 500)
    private String address;

    private String status = "ACTIVE";

    // ===== Health profile (used by BMI-based recommendation) =====

    /** MALE / FEMALE / OTHER */
    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Height in centimeters. */
    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    /** Weight in kilograms. */
    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    /**
     * Free-text health condition notes the user wants the system to consider, e.g.
     * "tiểu đường tuýp 2", "cao huyết áp", "dị ứng hải sản".
     */
    @Column(name = "health_condition", length = 500)
    private String healthCondition;

    /** NORMAL / VEGETARIAN / VEGAN / DIABETIC / LOW_SODIUM / LOW_FAT / KETO / GLUTEN_FREE */
    @Column(name = "diet_preference", length = 30)
    private String dietPreference;

    /** SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE */
    @Column(name = "activity_level", length = 20)
    private String activityLevel;

    /** LOSE_WEIGHT / MAINTAIN / GAIN_WEIGHT / GAIN_MUSCLE */
    @Column(name = "goal", length = 20)
    private String goal;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    // ===== getters / setters =====

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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

    public Set<RoleEntity> getRoles() { return roles; }
    public void setRoles(Set<RoleEntity> roles) { this.roles = roles; }
}
