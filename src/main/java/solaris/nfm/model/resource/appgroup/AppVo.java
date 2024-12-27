package solaris.nfm.model.resource.appgroup;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import solaris.nfm.model.resource.appgroup.AppGroup.AppStatus;
import solaris.nfm.model.resource.appgroup.AppGroup.PodPhase;

@Data
public class AppVo
{
	private String		appGroupUuid;
	private String		appName;
	private String		appUuid;
	private String		scheduleTime;
	private AppStatus	status;
	private PodPhase	podPhase;

	private String		cpu;
	private String		memory;
	private String		gpu;
	private JsonNode	storage;
	private JsonNode	service;
}
