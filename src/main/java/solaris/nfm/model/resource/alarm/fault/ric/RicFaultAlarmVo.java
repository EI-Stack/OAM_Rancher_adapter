package solaris.nfm.model.resource.alarm.fault.ric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class RicFaultAlarmVo extends RicFaultAlarm
{
	private static final long	serialVersionUID	= 1L;

	private String				errorMessage;
	private String				sop;

	private String				fieldName;
}
