package pl.polsl.take.restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload for creating an immediate order or a future reservation")
public class CreateOrderDTO {
    @Schema(description = "Optional for walk-in orders; required for future reservations")
    private Long customerId;
    
    @Positive
    private Integer tableNumber;

    @NotEmpty
    @Valid
    @JsonProperty("items")
    private List<CreateOrderItemDTO> items;

    @Schema(description = "When provided, the request is treated as a reservation", example = "2026-07-10T18:30:00")
    @Future
    private LocalDateTime orderDateTime;
}
