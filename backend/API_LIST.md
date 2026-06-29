# API Reference

Base URL: `http://localhost:8080`
Định dạng auth: `Authorization: Bearer <JWT>` (lấy từ `/api/auth/login`).
Tất cả response bọc trong:
```json
{ "success": true, "message": "...", "data": <payload> }
```

Swagger UI tương tác đầy đủ: `http://localhost:8080/swagger-ui.html`

---

## 🟢 Auth (public)

| Method | Path | Mô tả |
| --- | --- | --- |
| POST | `/api/auth/register` | Đăng ký người dùng mới (role mặc định CUSTOMER) |
| POST | `/api/auth/login` | Đăng nhập, trả về JWT |
| GET  | `/api/auth/me` | Thông tin user hiện tại (cần JWT) |

---

## ⭐ Health & BMI Recommendation (cần JWT — tính năng AI chính)

| Method | Path | Mô tả |
| --- | --- | --- |
| GET  | `/api/health/profile` | Lấy hồ sơ sức khỏe của user hiện tại |
| PUT  | `/api/health/profile` | Cập nhật chiều cao / cân nặng / bệnh nền / mục tiêu |
| GET  | `/api/health/analysis` | Phân tích BMI / BMR / TDEE / calo mục tiêu |
| GET  | `/api/health/recommendations?limit=10&useAi=true` | Gợi ý món ăn theo BMI + tình trạng sức khỏe |

Tham số `dietPreference` chấp nhận: `NORMAL`, `VEGETARIAN`, `VEGAN`, `DIABETIC`,
`LOW_SODIUM`, `LOW_FAT`, `KETO`, `GLUTEN_FREE`.

Tham số `activityLevel`: `SEDENTARY`, `LIGHT`, `MODERATE`, `ACTIVE`, `VERY_ACTIVE`.

Tham số `goal`: `LOSE_WEIGHT`, `MAINTAIN`, `GAIN_WEIGHT`, `GAIN_MUSCLE`.

---

## Categories

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/api/categories` | public |
| GET | `/api/categories/{id}` | public |
| POST | `/api/categories` | ADMIN |
| PUT | `/api/categories/{id}` | ADMIN |
| DELETE | `/api/categories/{id}` | ADMIN |

---

## Foods

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/api/foods?categoryId=&keyword=` | public |
| GET | `/api/foods/{id}` | public |
| POST | `/api/foods` | ADMIN |
| PUT | `/api/foods/{id}` | ADMIN |
| DELETE | `/api/foods/{id}` | ADMIN |

> Body create/update giờ chấp nhận thêm các trường dinh dưỡng:
> `calories`, `proteinG`, `fatG`, `carbsG`, `tags` (CSV như `HIGH_PROTEIN,LOW_SUGAR`).

---

## Cart (cần JWT)

| Method | Path |
| --- | --- |
| GET | `/api/cart` |
| POST | `/api/cart/items` |
| PUT | `/api/cart/items/{itemId}` |
| DELETE | `/api/cart/items/{itemId}` |
| DELETE | `/api/cart/clear` |

---

## Addresses (cần JWT)

| Method | Path |
| --- | --- |
| GET | `/api/addresses` |
| POST | `/api/addresses` |
| PUT | `/api/addresses/{id}` |
| DELETE | `/api/addresses/{id}` |

---

## Orders

| Method | Path | Quyền |
| --- | --- | --- |
| POST | `/api/orders` | CUSTOMER |
| GET | `/api/orders/my` | CUSTOMER |
| GET | `/api/orders/my/{id}` | CUSTOMER |
| GET | `/api/orders` | ADMIN |
| GET | `/api/orders/{id}` | ADMIN |
| PUT | `/api/orders/{id}/status` | ADMIN |

---

## Vouchers

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/api/vouchers/validate/{code}` | public |
| GET | `/api/vouchers` | ADMIN |
| GET | `/api/vouchers/{id}` | ADMIN |
| POST | `/api/vouchers` | ADMIN |
| PUT | `/api/vouchers/{id}` | ADMIN |
| DELETE | `/api/vouchers/{id}` | ADMIN |

---

## Reviews

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/api/reviews/food/{foodId}` | public |
| POST | `/api/reviews` | CUSTOMER |

---

## Wishlist (cần JWT, không có userId trong URL — lấy từ token)

| Method | Path |
| --- | --- |
| GET | `/api/wishlists/me` |
| POST | `/api/wishlists/food/{foodId}` |
| DELETE | `/api/wishlists/food/{foodId}` |
| GET | `/api/wishlists/food/{foodId}/check` |
| GET | `/api/wishlists/count` |

> ⚠ API cũ `/api/wishlists/user/{userId}/...` đã bị **xoá** vì cho phép user A
> xem/sửa wishlist của user B. FE phải đổi sang URL mới.

---

## Behavior-based Recommendations (cần JWT)

Gợi ý dựa trên lịch sử mua hàng / giỏ hàng – KHÔNG dựa trên BMI.

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/api/recommendations/my?limit=8` | logged-in |
| GET | `/api/recommendations/cart?limit=8` | logged-in |
| GET | `/api/recommendations/user/{userId}?limit=8` | ADMIN |

---

## AI Smart Features (cần JWT)

`/api/ai/**` — các tính năng AI phụ trợ.

| Method | Path | Mô tả |
| --- | --- | --- |
| GET | `/api/ai/recommendations/{userId}?limit=8` | Recommend theo hành vi (legacy) |
| POST | `/api/ai/chat` | Chatbot tư vấn món |
| GET | `/api/ai/smart-search?keyword=...` | Semantic search |
| POST | `/api/ai/payment/qr` | Sinh QR thanh toán VietQR |
| POST | `/api/ai/payment/qr/order/{orderId}` | QR cho đơn hàng cụ thể |
| POST | `/api/ai/food-description` | Sinh mô tả món ăn |
| POST | `/api/ai/review/sentiment` | Phân tích cảm xúc review |
| POST | `/api/ai/food-image/verify` | Xác thực ảnh món (đơn giản) |
| GET | `/api/ai/manager/demand-forecast` | Dự báo nhu cầu (admin) |
| GET | `/api/ai/manager/review-insights` | Thống kê cảm xúc review (admin) |

---

## Chatbot (cần JWT)

| Method | Path |
| --- | --- |
| POST | `/api/chatbot/message` |
| GET | `/api/chatbot/ping` |

---

## Admin Reports (ADMIN)

| Method | Path |
| --- | --- |
| GET | `/api/admin/reports/summary?from=&to=` |
| GET | `/api/admin/reports/revenue/daily?from=&to=` |
| GET | `/api/admin/reports/payments?from=&to=` |
| GET | `/api/admin/reports/best-selling-foods?from=&to=&limit=10` |

---

## Admin User / Role / Employee management (ADMIN)

| Method | Path |
| --- | --- |
| GET | `/api/admin/users` |
| GET | `/api/admin/users/{id}` |
| POST | `/api/admin/users` |
| PUT | `/api/admin/users/{id}` |
| PUT | `/api/admin/users/{id}/status` |
| PUT | `/api/admin/users/{id}/roles` |
| DELETE | `/api/admin/users/{id}` |
| GET | `/api/admin/roles` |
| GET | `/api/employee-positions` |
| POST | `/api/employee-positions` |
| PUT | `/api/employee-positions/{id}` |
| DELETE | `/api/employee-positions/{id}` |
| GET | `/api/employees` |
| POST | `/api/employees` |
| PUT | `/api/employees/{id}` |
| DELETE | `/api/employees/{id}` |

---

## Notifications

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/api/notifications/admin` | ADMIN |
| GET | `/api/notifications/me` | logged-in |
| PUT | `/api/notifications/{id}/read` | logged-in |

---

## Password reset (public)

| Method | Path |
| --- | --- |
| POST | `/api/auth/forgot-password` |
| POST | `/api/auth/verify-reset-token` |
| POST | `/api/auth/reset-password` |

---

## Swagger / OpenAPI

| Method | Path |
| --- | --- |
| GET | `/swagger-ui.html` |
| GET | `/v3/api-docs` |
