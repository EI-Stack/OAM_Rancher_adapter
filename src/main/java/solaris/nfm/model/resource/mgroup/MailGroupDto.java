package solaris.nfm.model.resource.mgroup;

import java.util.Set;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailGroupDto
{
	@NotBlank(groups = Create.class)
	@Null(groups = Update.class)
	@Length(min = 1, max = 16, groups = {Create.class, Update.class})
	private String		name;

	@NotNull(groups = Create.class)
	@Size(min = 1, max = 50, groups = {Create.class, Update.class})
	private Set<String>	mailAddresses;

	public interface Create
	{}

	public interface Update
	{}
}
