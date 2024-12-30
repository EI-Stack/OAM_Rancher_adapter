package solaris.nfm.model.resource.appgroup;

import lombok.Data;
import solaris.nfm.model.resource.appgroup.AppGroup.AppStatus;
import solaris.nfm.model.resource.appgroup.AppGroup.PodPhase;

@Data
public class AppListVo
{
	private String		appName;
	private AppStatus	status;
	private PodPhase	podPhase;
	private String		scheduleTime;
	private String		startTime;
	private String		creationTime;
}
