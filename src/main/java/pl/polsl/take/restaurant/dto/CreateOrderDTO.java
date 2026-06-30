package pl.polsl.take.restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderDTO {
    private Long customerId; //null if customer is not registered
    
    @Positive
    private Integer tableNumber;

    @NotEmpty
    @Valid
    private List<CreateOrderItemDTO> items;

    @Future
    private LocalDateTime orderDateTime; // optional, for reservations
}
