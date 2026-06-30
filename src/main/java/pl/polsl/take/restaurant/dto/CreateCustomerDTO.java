package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerDTO {
	private String firstName;

	private String lastName;

	private String phoneNumber;

	@Email
	private String email;
}
