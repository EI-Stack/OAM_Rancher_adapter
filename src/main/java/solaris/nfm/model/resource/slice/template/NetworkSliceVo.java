package solaris.nfm.model.resource.slice.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class NetworkSliceVo extends NetworkSlice
{
	private static final long serialVersionUID = 1L;
}