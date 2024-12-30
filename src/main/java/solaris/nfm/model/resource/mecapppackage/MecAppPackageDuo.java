package solaris.nfm.model.resource.mecapppackage;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import solaris.nfm.controller.dto.ControllerBaseDto;

@Getter
@Setter
public class MecAppPackageDuo extends ControllerBaseDto
{
	private String		name;
	@Size(max = 1024, message = "The string length of argument (description) must be less than 1024.")
	private String		description;
	@Size(max = 102400, message = "The string length of argument (icon) must be less than 102400.")
	private String		icon;
	private JsonNode	files;
}
