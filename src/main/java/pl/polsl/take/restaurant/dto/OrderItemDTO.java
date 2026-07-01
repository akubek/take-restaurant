package pl.polsl.take.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;

@Getter
@Schema(description = "Order item response model")
public class OrderItemDTO {

    private Long id;
    private Long dishId;
    private String dishName;
    @Schema(description = "Dish price snapshot from order time (in cents/grosz)", example = "3200")
    private Integer price;
    private Integer quantity;
    private Integer seatNumber;
    private String notes;
    @Schema(example = "NEW")
    private OrderItemStatus status;
    private Boolean isCancelled;

    public OrderItemDTO(OrderItem item) {
        this.id = item.getId();
        this.dishId = item.getDish().getId();
        this.dishName = item.getDish().getName();
        this.price = item.getDishPriceAtOrderTime();
        this.quantity = item.getQuantity();
        this.seatNumber = item.getSeatNumber();
        this.notes = item.getNotes();
        this.status = item.getStatus();
        this.isCancelled = item.getIsCancelled();
    }
}