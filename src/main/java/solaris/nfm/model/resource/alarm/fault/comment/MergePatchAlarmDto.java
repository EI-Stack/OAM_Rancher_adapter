package solaris.nfm.model.resource.alarm.fault.comment;

import lombok.Data;
import solaris.nfm.model.base.domain.FaultAlarmBase.AckState;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;

@Data
public class MergePatchAlarmDto
{
	private AckState			ackState;
	private PerceivedSeverity	perceivedSeverity;
}
