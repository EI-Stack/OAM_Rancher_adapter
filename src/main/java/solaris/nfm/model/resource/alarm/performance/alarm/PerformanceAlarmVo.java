package solaris.nfm.model.resource.alarm.performance.alarm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
{"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class PerformanceAlarmVo extends PerformanceAlarm
{
	private static final long serialVersionUID = 1L;
}
