package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class UserNotificationController {

    private final NotificationService notificationService;

    public UserNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thông báo của tôi thành công",
                notificationService.getMyNotifications(authentication.getName())
        ));
    }

    @GetMapping("/my/unread")
    public ResponseEntity<?> getMyUnreadNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thông báo chưa đọc của tôi thành công",
                notificationService.getUnreadMyNotifications(authentication.getName())
        ));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<?> countMyUnreadNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy số lượng thông báo chưa đọc thành công",
                Map.of("count", notificationService.countUnreadMyNotifications(authentication.getName()))
        ));
    }

    @PutMapping("/my/{id}/read")
    public ResponseEntity<?> markMyNotificationAsRead(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đánh dấu thông báo đã đọc thành công",
                notificationService.markMyNotificationAsRead(authentication.getName(), id)
        ));
    }

    @PutMapping("/my/read-all")
    public ResponseEntity<?> markAllMyNotificationsAsRead(Authentication authentication) {
        notificationService.markAllMyNotificationsAsRead(authentication.getName());

        return ResponseEntity.ok(ApiResponse.success(
                "Đánh dấu tất cả thông báo của tôi đã đọc thành công",
                null
        ));
    }

    @DeleteMapping("/my/{id}")
    public ResponseEntity<?> deleteMyNotification(
            Authentication authentication,
            @PathVariable Long id
    ) {
        notificationService.deleteMyNotification(authentication.getName(), id);

        return ResponseEntity.ok(ApiResponse.success(
                "Xóa thông báo thành công",
                null
        ));
    }

    @DeleteMapping("/my/all")
    public ResponseEntity<?> deleteAllMyNotifications(Authentication authentication) {
        notificationService.deleteAllMyNotifications(authentication.getName());

        return ResponseEntity.ok(ApiResponse.success(
                "Xóa tất cả thông báo thành công",
                null
        ));
    }
}