package solaris.nfm.model.resource.mecapppackage;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import solaris.nfm.controller.dto.ControllerBaseDto;

@Getter
@Setter
public class MecAppPackageDto extends ControllerBaseDto
{
	@NotBlank(groups = Create.class)
	@Null(groups = Update.class)
	@Length(min = 1, max = 64, groups = {Create.class, Update.class})
	private String		name;

	@Length(min = 1, max = 1024, groups = {Create.class, Update.class})
	private String		description;

	@Length(min = 1, max = 102400, groups = {Create.class, Update.class})
	private String		icon;

	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	private JsonNode	files;

	public interface Create
	{}

	public interface Update
	{}
}
