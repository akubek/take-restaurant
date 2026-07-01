package pl.polsl.take.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;

@Getter
@Setter
@Schema(description = "Payload for changing order item status")
public class UpdateOrderItemStatusDTO {
    @NotNull
    @Schema(example = "DELIVERED")
    private OrderItemStatus status;
}