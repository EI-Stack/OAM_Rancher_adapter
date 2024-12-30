package solaris.nfm.model.resource.slice.instance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class NetworkSliceInstanceVo extends NetworkSliceInstance
{
	private static final long serialVersionUID = 1L;
}