package solaris.nfm.model.resource.alarm.performance.rule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class PerformanceRule extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	@Enumerated(EnumType.STRING)
	private NetworkType			networkType;
	private String				name;
	@Enumerated(EnumType.STRING)
	private Comparison			comparison;
	private Double				threshold;
	private Integer				period;
	private LocalDateTime		triggerTime;
	@Enumerated(EnumType.STRING)
	private PerceivedSeverity	severity;
	private Boolean				disabled;
	private String				description;
	private String				sop;
	private Boolean				mailDisabled;
	@Type(ListArrayType.class)
	private List<String>		mailAddresses		= new ArrayList<>();

	public enum Comparison
	{
		greater,
		less;
	}
}