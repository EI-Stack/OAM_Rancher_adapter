package solaris.nfm.model.resource.statistic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(
{"id", "entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class StatisticVo extends Statistic
{
	private static final long serialVersionUID = 1L;
}