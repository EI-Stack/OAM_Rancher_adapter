package solaris.nfm.model.resource.appgroup;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import solaris.nfm.model.resource.appgroup.AppGroup.PodPhase;

@Data
public class AppGroupVo
{
	private String		siteName;
	private String		uuid;
	private String		name;
	private Integer		priority;
	private Boolean		preempted;
	private JsonNode	paasRequest;
	private String		description;
	private String		status;
	private PodPhase	podPhase;

	private List<App>	apps	= new ArrayList<>();

	@Data
	public static class App
	{
		private String			name;
		private JsonNode		service;
		private JsonNode		resource;

		private ZonedDateTime	creationTime;
		private ZonedDateTime	startTime;
		private String			scheduelTime;
		private Integer			replica;

		private String			status;
	}
}
