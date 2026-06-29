package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Reponse.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Nhận file ảnh từ client (avatar người dùng, ảnh món ăn), lưu vào thư mục
 * upload cục bộ và trả về URL công khai dạng /uploads/{tên-file}.
 * Người dùng đăng nhập đều có thể upload (avatar); món ăn do admin upload.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn ảnh"));
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chỉ chấp nhận file ảnh"));
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ảnh phải nhỏ hơn 10MB"));
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "img" : file.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/" + filename;
            return ResponseEntity.ok(ApiResponse.success("Tải ảnh thành công", Map.of("url", url)));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Không lưu được ảnh: " + e.getMessage()));
        }
    }
}
