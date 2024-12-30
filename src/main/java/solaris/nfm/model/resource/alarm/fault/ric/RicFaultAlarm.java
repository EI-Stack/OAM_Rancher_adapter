package solaris.nfm.model.resource.alarm.fault.ric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.FaultAlarmBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class RicFaultAlarm extends FaultAlarmBase
{
	private static final long	serialVersionUID	= 1L;

	private String				fieldId;
	private String				nci;
}