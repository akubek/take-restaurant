package pl.polsl.take.restaurant.controller;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.DishPopularityDTO;
import pl.polsl.take.restaurant.service.StatsService;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Reporting endpoints for revenue and dish popularity")
public class StatsController {

    private final StatsService service;

    @GetMapping("/revenue/today")
    @Operation(summary = "Get today's revenue", description = "Returns total paid revenue for the current day")
    public Long todayRevenue() {
        return service.getTodayRevenue();
    }

    @GetMapping("/revenue/week")
    @Operation(summary = "Get weekly revenue", description = "Returns total paid revenue for the current week")
    public Long weekRevenue() {
        return service.getWeekRevenue();
    }

    @GetMapping("/revenue/month")
    @Operation(summary = "Get monthly revenue", description = "Returns total paid revenue for the current month")
    public Long monthRevenue() {
        return service.getMonthRevenue();
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue for time range", description = "Returns total paid revenue between two date-time values")
    public Long revenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.getRevenueBetween(from, to);
    }

    @GetMapping("/popularity")
    @Operation(summary = "Get dish popularity", description = "Returns sold dish quantities between two date-time values")
    public List<DishPopularityDTO> dishPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.getDishPopularity(from, to);
    }
}