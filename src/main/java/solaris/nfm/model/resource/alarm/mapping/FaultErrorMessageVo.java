package solaris.nfm.model.resource.alarm.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(
{"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class FaultErrorMessageVo extends FaultErrorMessage
{
	private static final long serialVersionUID = 1L;
}