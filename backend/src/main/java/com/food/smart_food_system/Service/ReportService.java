package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.BestSellingFoodDTO;
import com.food.smart_food_system.DTO.DailyRevenueDTO;
import com.food.smart_food_system.DTO.PaymentStatisticDTO;
import com.food.smart_food_system.DTO.ReportSummaryDTO;
import com.food.smart_food_system.Repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;

    public ReportService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            FoodRepository foodRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
    }

    public ReportSummaryDTO getSummary(LocalDate from, LocalDate to) {
        LocalDateTime startDate = toStartDate(from);
        LocalDateTime endDate = toEndDate(to);

        ReportSummaryDTO dto = new ReportSummaryDTO();
        dto.setTotalOrders(orderRepository.countByCreatedAtBetween(startDate, endDate));
        dto.setTotalRevenue(safeDecimal(orderRepository.sumRevenue(startDate, endDate)));
        dto.setPaidRevenue(safeDecimal(orderRepository.sumRevenueByPaymentStatus("PAID", startDate, endDate)));
        dto.setUnpaidRevenue(safeDecimal(orderRepository.sumRevenueByPaymentStatus("UNPAID", startDate, endDate)));
        dto.setPendingOrders(orderRepository.countByOrderStatusAndCreatedAtBetween("PENDING", startDate, endDate));
        dto.setCompletedOrders(orderRepository.countByOrderStatusAndCreatedAtBetween("COMPLETED", startDate, endDate));
        dto.setCancelledOrders(orderRepository.countByOrderStatusAndCreatedAtBetween("CANCELLED", startDate, endDate));
        dto.setTotalUsers(userRepository.countByRoleName("CUSTOMER"));
        dto.setTotalFoods(foodRepository.count());

        return dto;
    }

    public List<DailyRevenueDTO> getDailyRevenue(LocalDate from, LocalDate to) {
        return orderRepository.getDailyRevenueRaw(toStartDate(from), toEndDate(to))
                .stream()
                .map(row -> new DailyRevenueDTO(toLocalDate(row[0]), toBigDecimal(row[1]), toLong(row[2])))
                .toList();
    }

    public List<PaymentStatisticDTO> getPaymentStatistics(LocalDate from, LocalDate to) {
        return paymentRepository.getPaymentStatistics(toStartDate(from), toEndDate(to));
    }

    public List<BestSellingFoodDTO> getBestSellingFoods(LocalDate from, LocalDate to, int limit) {
        return orderItemRepository.getBestSellingFoods(toStartDate(from), toEndDate(to), PageRequest.of(0, limit));
    }

    private LocalDateTime toStartDate(LocalDate date) { return (date != null ? date : LocalDate.now().minusDays(30)).atStartOfDay(); }
    private LocalDateTime toEndDate(LocalDate date) { return (date != null ? date : LocalDate.now()).atTime(LocalTime.MAX); }
    private BigDecimal safeDecimal(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof java.sql.Date sd) return sd.toLocalDate();
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return LocalDate.parse(v.toString());
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(v.toString());
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }
}
