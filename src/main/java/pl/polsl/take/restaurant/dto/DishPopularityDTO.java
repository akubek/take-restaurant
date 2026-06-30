package pl.polsl.take.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DishPopularityDTO {
    private Long dishId;
    private String dishName;
    private Long totalSold;
}