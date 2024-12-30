package solaris.nfm.model.resource.alarm.fault.fgc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class FaultAlarmVo extends FaultAlarm
{
	private static final long	serialVersionUID	= 1L;

	private String				mappedAlarmDescription;
	private String				mappedRecommendedOperation;
}
