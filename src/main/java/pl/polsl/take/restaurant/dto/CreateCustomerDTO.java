package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerDTO {
	@NotBlank
	private String firstName;
	
	@NotBlank
	private String lastName;

	private String phoneNumber;

	@Email
	private String email;
}
