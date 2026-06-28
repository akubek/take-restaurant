package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;

@Getter
@Setter
public class UpdateOrderItemStatusDTO {
    @NotNull
    private OrderItemStatus status;
}