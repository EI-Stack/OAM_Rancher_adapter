package solaris.nfm.model.resource.profile;

import java.time.ZonedDateTime;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDto
{
	@NotBlank(groups = Create.class)
	@Null(groups = Update.class)
	@Length(min = 1, max = 32, groups = {Create.class, Update.class})
	private String				name;

	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	private Profile.ProfileType	type;

	private String				description;

	@NotNull(groups = Create.class)
	// @Null(groups = Update.class)
	private ObjectNode			json;

	@Null(groups = {Create.class, Update.class})
	private ZonedDateTime		changeTime;

	public interface Create
	{}

	public interface Update
	{}
}
