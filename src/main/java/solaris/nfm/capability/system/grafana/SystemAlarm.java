package solaris.nfm.capability.system.grafana;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief", "$$_hibernate_interceptor", "hibernateLazyInitializer"})
public class SystemAlarm extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;
	private Integer				faultId;
	private String				faultSource;
	// private Severity faultSeverity;
	private String				faultText;
	private Boolean				cleared;
	private LocalDateTime		eventTime;
	private Integer				faultCount;
	private LocalDateTime		startTime;
	private Boolean				acknowledged;
	private String				acknowledgeComment;
	private Long				acknowledgeUserId;
	private String				acknowledgeUserName;
}