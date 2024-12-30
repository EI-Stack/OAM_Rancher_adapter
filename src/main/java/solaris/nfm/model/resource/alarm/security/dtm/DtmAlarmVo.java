package solaris.nfm.model.resource.alarm.security.dtm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class DtmAlarmVo extends DtmAlarm
{
	private static final long serialVersionUID = 1L;
}
