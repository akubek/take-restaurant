package pl.polsl.take.restaurant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.StatsService;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService service;

    @GetMapping("/revenue/today")
    public int today() {
        return service.todayRevenue();
    }

    @GetMapping("/revenue/week")
    public int week() {
        return service.weekRevenue();
    }
}