package pl.polsl.take.restaurant.model.DTOs;

import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;

@Getter
@Setter
public class CreateOrderItemDTO {
	private Long id;
	private Long orderId;
	private Long dishId;
	private Integer price;
	private Integer quantity;
	private Integer seatNumber;
	private String notes;
	private OrderItemStatus status;
}
