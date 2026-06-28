package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.OrderStatus;

@Getter
@Setter
public class UpdateOrderStatusDTO {
    @NotNull
    private OrderStatus status;
}