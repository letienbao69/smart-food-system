package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateOrderRequest;
import com.food.smart_food_system.DTO.OrderItemResponseDTO;
import com.food.smart_food_system.DTO.OrderResponseDTO;
import com.food.smart_food_system.DTO.UpdateOrderStatusRequest;
import com.food.smart_food_system.Entity.*;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.*;
import com.food.smart_food_system.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private UserVoucherRepository userVoucherRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public OrderResponseDTO createOrder(Long userId, CreateOrderRequest request) {
        validateCreateOrderRequest(request);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + userId));

        CartEntity cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Người dùng chưa có giỏ hàng"));

        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống");
        }

        AddressEntity address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ với id: " + request.getAddressId()));

        if (address.getUser() == null || !address.getUser().getId().equals(userId)) {
            throw new BusinessException("Địa chỉ giao hàng không thuộc về user này");
        }

        BigDecimal totalAmount = calculateTotalAmount(cartItems);

        VoucherEntity voucher = null;
        UserVoucherEntity userVoucher = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getVoucherId() != null) {
            voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với id: " + request.getVoucherId()));

            validateVoucher(voucher, totalAmount);

            userVoucher = userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId()).orElse(null);
            if (userVoucher != null && Boolean.TRUE.equals(userVoucher.getIsUsed())) {
                throw new BusinessException("Voucher này đã được sử dụng");
            }

            discountAmount = calculateDiscount(voucher, totalAmount);
        }

        validateStock(cartItems);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setAddress(address);
        order.setVoucher(voucher);
        order.setOrderCode(generateOrderCode());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setPaymentMethod(request.getPaymentMethod().trim().toUpperCase());
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setNote(request.getNote());

        OrderEntity savedOrder = orderRepository.save(order);

        for (CartItemEntity cartItem : cartItems) {
            FoodEntity food = cartItem.getFood();

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(savedOrder);
            orderItem.setFood(food);
            orderItem.setFoodName(food.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setSubtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemRepository.save(orderItem);

            food.setStock(food.getStock() - cartItem.getQuantity());
            if (food.getStock() <= 0) {
                food.setStock(Math.max(food.getStock(), 0));
                food.setStatus("OUT_OF_STOCK");
            }
            foodRepository.save(food);
        }

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrder(savedOrder);
        history.setStatus("PENDING");
        history.setNote("Đơn hàng vừa được tạo");
        orderStatusHistoryRepository.save(history);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(savedOrder);
        payment.setProvider(request.getPaymentMethod().trim().toUpperCase());
        payment.setAmount(finalAmount);
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        if (voucher != null) {
            voucher.setQuantity(Math.max(voucher.getQuantity() - 1, 0));
            voucherRepository.save(voucher);

            if (userVoucher != null) {
                userVoucher.setIsUsed(true);
                userVoucher.setUsedAt(LocalDateTime.now());
                userVoucherRepository.save(userVoucher);
            }
        }

        cartItemRepository.deleteByCartId(cart.getId());

        return mapToOrderResponseDTO(savedOrder);
    }

    @Override
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + userId));

        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToOrderResponseDTO)
                .toList();
    }

    @Override
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderResponseDTO)
                .toList();
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        return mapToOrderResponseDTO(order);
    }

    @Override
    public OrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        if (request.getOrderStatus() == null || request.getOrderStatus().isBlank()) {
            throw new BusinessException("Trạng thái đơn hàng không được để trống");
        }

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        String newStatus = request.getOrderStatus().trim().toUpperCase();

        order.setOrderStatus(newStatus);

        if ("COMPLETED".equals(newStatus)) {
            order.setPaymentStatus("PAID");
        }

        if ("CANCELLED".equals(newStatus)) {
            restoreStock(orderId);
        }

        orderRepository.save(order);

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrder(order);
        history.setStatus(newStatus);
        history.setNote(request.getNote());
        orderStatusHistoryRepository.save(history);

        return mapToOrderResponseDTO(order);
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.getAddressId() == null) {
            throw new BusinessException("Địa chỉ giao hàng không được để trống");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new BusinessException("Phương thức thanh toán không được để trống");
        }

        String paymentMethod = request.getPaymentMethod().trim().toUpperCase();
        if (!paymentMethod.equals("COD") && !paymentMethod.equals("VNPAY") && !paymentMethod.equals("MOMO")) {
            throw new BusinessException("Phương thức thanh toán không hợp lệ");
        }
    }

    private BigDecimal calculateTotalAmount(List<CartItemEntity> cartItems) {
        return cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateVoucher(VoucherEntity voucher, BigDecimal totalAmount) {
        if (voucher.getStatus() == null || !"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            throw new BusinessException("Voucher không khả dụng");
        }

        if (voucher.getStartDate() != null && LocalDateTime.now().isBefore(voucher.getStartDate())) {
            throw new BusinessException("Voucher chưa tới thời gian sử dụng");
        }

        if (voucher.getEndDate() != null && LocalDateTime.now().isAfter(voucher.getEndDate())) {
            throw new BusinessException("Voucher đã hết hạn");
        }

        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            throw new BusinessException("Voucher đã hết số lượng");
        }

        if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
        }
    }

    private BigDecimal calculateDiscount(VoucherEntity voucher, BigDecimal totalAmount) {
        BigDecimal discount = BigDecimal.ZERO;
        String discountType = voucher.getDiscountType() == null ? "" : voucher.getDiscountType().trim().toUpperCase();

        if ("PERCENT".equals(discountType) || "%".equals(discountType)) {
            discount = totalAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
                discount = voucher.getMaxDiscount();
            }
        } else if ("VND".equals(discountType)) {
            discount = voucher.getDiscountValue();
        }

        if (discount.compareTo(totalAmount) > 0) {
            discount = totalAmount;
        }

        return discount;
    }

    private void validateStock(List<CartItemEntity> cartItems) {
        for (CartItemEntity item : cartItems) {
            FoodEntity food = item.getFood();
            if (food.getStock() == null || food.getStock() < item.getQuantity()) {
                throw new BusinessException("Món ăn '" + food.getName() + "' không đủ số lượng tồn kho");
            }
        }
    }

    private void restoreStock(Long orderId) {
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItemEntity item : orderItems) {
            FoodEntity food = item.getFood();
            food.setStock(food.getStock() + item.getQuantity());
            if (food.getStock() > 0 && "OUT_OF_STOCK".equalsIgnoreCase(food.getStatus())) {
                food.setStatus("AVAILABLE");
            }
            foodRepository.save(food);
        }
    }

    private String generateOrderCode() {
        return "ORD" + System.currentTimeMillis();
    }

    private OrderResponseDTO mapToOrderResponseDTO(OrderEntity order) {
        List<OrderItemResponseDTO> itemDTOs = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(item -> new OrderItemResponseDTO(
                        item.getFood().getId(),
                        item.getFoodName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getOrderCode(),
                order.getUser() != null ? order.getUser().getId() : null,
                order.getAddress() != null ? order.getAddress().getId() : null,
                order.getVoucher() != null ? order.getVoucher().getId() : null,
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getNote(),
                order.getCreatedAt(),
                itemDTOs
        );
    }
}