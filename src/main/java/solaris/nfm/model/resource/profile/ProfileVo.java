package solaris.nfm.model.resource.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class ProfileVo extends Profile
{
	private static final long serialVersionUID = 1L;
}