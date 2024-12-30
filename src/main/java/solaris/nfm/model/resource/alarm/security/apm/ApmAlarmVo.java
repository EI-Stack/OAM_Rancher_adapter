package solaris.nfm.model.resource.alarm.security.apm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class ApmAlarmVo extends ApmAlarm
{
	private static final long serialVersionUID = 1L;
}
