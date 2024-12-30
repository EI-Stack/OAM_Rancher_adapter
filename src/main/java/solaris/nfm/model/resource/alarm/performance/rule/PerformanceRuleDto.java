package solaris.nfm.model.resource.alarm.performance.rule;

import java.util.Set;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import solaris.nfm.controller.dto.ControllerBaseDto;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;
import solaris.nfm.model.resource.alarm.performance.rule.PerformanceRule.Comparison;

@Getter
@Setter
public class PerformanceRuleDto extends ControllerBaseDto
{
	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	private NetworkType			networkType;

	@NotBlank(groups = Create.class)
	@Null(groups = Update.class)
	@Length(min = 8, max = 128, groups = {Create.class, Update.class})
	private String				name;

	@NotNull(groups = Create.class)
	private Comparison			comparison;

	@NotNull(groups = Create.class)
	private Double				threshold;

	@NotNull(groups = Create.class)
	@Range(min = 30, max = 300, groups = {Create.class, Update.class})
	private Integer				period;

	@NotNull(groups = Create.class)
	private PerceivedSeverity	severity;

	@NotNull(groups = Create.class)
	private Boolean				disabled;

	@Length(min = 1, max = 1024, groups = {Create.class, Update.class})
	private String				description;

	@Length(min = 1, max = 1024, groups = {Create.class, Update.class})
	private String				sop;

	@NotNull(groups = Create.class)
	private Boolean				mailDisabled;

	private Set<String>			mailAddresses;

	public interface Create
	{}

	public interface Update
	{}
}