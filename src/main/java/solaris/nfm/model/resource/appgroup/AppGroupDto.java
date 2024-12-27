package solaris.nfm.model.resource.appgroup;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;
import solaris.nfm.controller.dto.ControllerBaseDto;

@Getter
@Setter
public class AppGroupDto extends ControllerBaseDto
{
	private String		siteName;
	private String		name;

	private Integer		priority;
	private Boolean		preempted;

	private JsonNode	paasRequest;
	private String		description;

	// 第一版不支援延遲執行
	// private ArrayNode appScheduleTimes;

	public interface Create
	{}

	public interface Update
	{}
}
