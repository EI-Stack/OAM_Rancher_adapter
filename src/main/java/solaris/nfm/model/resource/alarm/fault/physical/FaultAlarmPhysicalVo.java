package solaris.nfm.model.resource.alarm.fault.physical;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class FaultAlarmPhysicalVo extends FaultAlarmPhysical
{
	private static final long	serialVersionUID	= 1L;

	private String				mappedAlarmDescription;
	private String				mappedRecommendedOperation;
}
