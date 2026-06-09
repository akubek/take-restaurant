package pl.polsl.take.restaurant.model.DTOs;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateOrderDTO {
    private Long customerId;
    private Integer tableNumber;
    private List<CreateOrderItemDTO> items;
}
