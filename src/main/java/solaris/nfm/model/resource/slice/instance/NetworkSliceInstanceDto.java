package solaris.nfm.model.resource.slice.instance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetworkSliceInstanceDto
{
	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	private Long	networkSliceId;

	private String	description;

	public interface Create
	{}

	public interface Update
	{}
}
