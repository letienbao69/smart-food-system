package com.food.smart_food_system;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SmartFoodSystemApplication {

    // Đồng bộ múi giờ JVM về giờ Việt Nam (UTC+7) để LocalDateTime.now()
    // và CURRENT_TIMESTAMP của MySQL khớp nhau, tránh lệch giờ trên giao diện.
    @PostConstruct
    void initTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(SmartFoodSystemApplication.class, args);
    }
}
