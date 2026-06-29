package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Admin reports. ONLY accessible by ADMIN role - the previous version mistakenly
 * allowed "USER", which is a non-existent role and conceptually wrong anyway
 * (customers should not be able to see revenue).
 */
@Tag(name = "Admin Reports", description = "Báo cáo, thống kê doanh thu (chỉ ADMIN)")
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Báo cáo tổng quan")
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy báo cáo tổng quan thành công",
                reportService.getSummary(from, to)));
    }

    @Operation(summary = "Thống kê doanh thu theo ngày")
    @GetMapping("/revenue/daily")
    public ResponseEntity<?> getDailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thống kê doanh thu theo ngày thành công",
                reportService.getDailyRevenue(from, to)));
    }

    @Operation(summary = "Thống kê thanh toán theo phương thức")
    @GetMapping("/payments")
    public ResponseEntity<?> getPaymentStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thống kê thanh toán thành công",
                reportService.getPaymentStatistics(from, to)));
    }

    @Operation(summary = "Top món bán chạy")
    @GetMapping("/best-selling-foods")
    public ResponseEntity<?> getBestSellingFoods(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách món bán chạy thành công",
                reportService.getBestSellingFoods(from, to, limit)));
    }
}
