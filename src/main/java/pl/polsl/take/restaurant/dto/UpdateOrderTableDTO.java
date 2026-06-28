package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderTableDTO {
    @NotNull
    @Positive
    private Integer tableNumber;
}