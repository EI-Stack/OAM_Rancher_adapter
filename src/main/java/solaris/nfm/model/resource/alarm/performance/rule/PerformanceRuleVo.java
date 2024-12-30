package solaris.nfm.model.resource.alarm.performance.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
{"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class PerformanceRuleVo extends PerformanceRule
{
	private static final long serialVersionUID = 1L;
}
