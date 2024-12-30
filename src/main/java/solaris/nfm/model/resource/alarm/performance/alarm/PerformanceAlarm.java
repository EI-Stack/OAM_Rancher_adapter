package solaris.nfm.model.resource.alarm.performance.alarm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;
import solaris.nfm.model.base.domain.IdentityEntityBase;
import solaris.nfm.model.resource.alarm.performance.rule.PerformanceRule.Comparison;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class PerformanceAlarm extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	@Enumerated(EnumType.STRING)
	private NetworkType			networkType;
	private String				name;
	@Enumerated(EnumType.STRING)
	private PerceivedSeverity	severity;
	private String				description;
	private LocalDateTime		time;
	@Enumerated(EnumType.STRING)
	private Comparison			comparison;
	private Double				threshold;
	private Double				value;
	@Type(JsonType.class)
	private JsonNode			detail;
}
