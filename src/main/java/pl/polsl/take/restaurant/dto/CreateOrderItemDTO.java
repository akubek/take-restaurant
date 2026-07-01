package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderItemDTO {
	@NotNull
	private Long dishId;

	@NotNull
	@Min(value = 1)
	private Integer quantity;

	@Positive
	private Integer seatNumber;

	private String notes;
}
