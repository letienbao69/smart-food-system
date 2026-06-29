package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Entity.ContactMessageEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Repository.ContactMessageRepository;
import com.food.smart_food_system.Repository.UserRepository;
import com.food.smart_food_system.Service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Liên hệ / phản ánh:
 * - Khách (kể cả chưa đăng nhập) gửi phản ánh qua POST /api/contacts (public).
 * - Admin xem danh sách, đánh dấu đã xử lý, xóa.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactMessageRepository repo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ContactController(ContactMessageRepository repo, UserRepository userRepository,
                             NotificationService notificationService) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ApiResponse<ContactMessageEntity> create(@RequestBody Map<String, String> body, Authentication auth) {
        String name = trim(body.get("name"));
        String message = trim(body.get("message"));
        if (name == null || name.isBlank()) throw new BusinessException("Vui lòng nhập họ tên");
        if (message == null || message.isBlank()) throw new BusinessException("Vui lòng nhập nội dung phản ánh");

        ContactMessageEntity c = new ContactMessageEntity();
        c.setName(name);
        c.setEmail(trim(body.get("email")));
        c.setPhone(trim(body.get("phone")));
        c.setSubject(trim(body.get("subject")));
        c.setMessage(message);
        c.setStatus("NEW");
        if (auth != null && auth.getName() != null) {
            userRepository.findByEmail(auth.getName()).ifPresent(u -> c.setUserId(u.getId()));
        }
        return ApiResponse.success("Đã gửi phản ánh, cảm ơn bạn!", repo.save(c));
    }

    @GetMapping
    public ApiResponse<List<ContactMessageEntity>> list() {
        return ApiResponse.success("OK", repo.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ContactMessageEntity> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ContactMessageEntity c = repo.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phản ánh"));
        String oldStatus = c.getStatus();
        String status = trim(body.get("status"));
        String reply = trim(body.get("reply"));
        if (status != null) c.setStatus(status);
        ContactMessageEntity saved = repo.save(c);

        // Khi chuyển sang ĐÃ XỬ LÝ và phản ánh gắn với một tài khoản -> thông báo cho khách
        boolean nowResolved = status != null && status.equalsIgnoreCase("RESOLVED")
                && (oldStatus == null || !oldStatus.equalsIgnoreCase("RESOLVED"));
        if (nowResolved && c.getUserId() != null) {
            userRepository.findById(c.getUserId()).ifPresent(u ->
                    notificationService.notifyContactResolved(u, c.getSubject(), reply));
        }
        return ApiResponse.success("Đã cập nhật", saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ApiResponse.success("Đã xóa", null);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
}
