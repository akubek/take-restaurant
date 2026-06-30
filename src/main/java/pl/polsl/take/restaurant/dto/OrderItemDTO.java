package pl.polsl.take.restaurant.dto;

import lombok.Getter;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;

@Getter
public class OrderItemDTO {

    private Long id;
    private Long dishId;
    private String dishName;
    private Integer price;
    private Integer quantity;
    private Integer seatNumber;
    private String notes;
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