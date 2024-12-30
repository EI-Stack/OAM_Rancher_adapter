package solaris.nfm.model.resource.appgroup;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class AppGroupsVo
{
	private Long		id;
	private String		siteName;
	private String		name;
	private Integer		priority;
	private Boolean		preempted;
	private JsonNode	paasRequest;
	private String		description;
	private String		status;
}
