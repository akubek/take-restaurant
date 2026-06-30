package pl.polsl.take.restaurant.controller;

import java.time.LocalDateTime;
import java.util.List;

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
public class StatsController {

    private final StatsService service;

    @GetMapping("/revenue/today")
    public Long todayRevenue() {
        return service.getTodayRevenue();
    }

    @GetMapping("/revenue/week")
    public Long weekRevenue() {
        return service.getWeekRevenue();
    }

    @GetMapping("/revenue/month")
    public Long monthRevenue() {
        return service.getMonthRevenue();
    }

    @GetMapping("/revenue")
    public Long revenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.getRevenueBetween(from, to);
    }

    @GetMapping("/popularity")
    public List<DishPopularityDTO> dishPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.getDishPopularity(from, to);
    }
}