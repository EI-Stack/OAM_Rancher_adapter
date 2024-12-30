package solaris.nfm.model.resource.mgroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(
{"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class MailGroupVo extends MailGroup
{
	private static final long serialVersionUID = 1L;
}