package solaris.nfm.model.resource.alarm.fault.mec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.FaultAlarmBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class MecFaultAlarm extends FaultAlarmBase
{
	private static final long	serialVersionUID	= 1L;

	private String				regionId;
	private String				appId;
	private String				appIp;
}