package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thông báo thành công",
                notificationService.getAdminNotifications()
        ));
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnread() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thông báo chưa đọc thành công",
                notificationService.getUnreadAdminNotifications()
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> countUnread() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy số lượng thông báo chưa đọc thành công",
                Map.of("count", notificationService.countUnreadAdminNotifications())
        ));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đánh dấu thông báo đã đọc thành công",
                notificationService.markAsRead(id)
        ));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        notificationService.markAllAdminAsRead();

        return ResponseEntity.ok(ApiResponse.success(
                "Đánh dấu tất cả thông báo đã đọc thành công",
                null
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        notificationService.deleteAdminNotification(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Xóa thông báo thành công",
                null
        ));
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllNotifications() {
        notificationService.deleteAllAdminNotifications();

        return ResponseEntity.ok(ApiResponse.success(
                "Xóa tất cả thông báo thành công",
                null
        ));
    }
}