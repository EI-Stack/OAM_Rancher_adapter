package solaris.nfm.model.resource.slice.template;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetworkSliceDto
{
	@NotBlank(groups = Create.class)
	@Null(groups = Update.class)
	@Length(min = 1, max = 32, groups = {Create.class, Update.class})
	private String		name;

	private String		description;

	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	private JsonNode	profile;

	public interface Create
	{}

	public interface Update
	{}
}
