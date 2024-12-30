package solaris.nfm.model.resource.openflow.tnslice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class TnSliceVo extends TnSlice
{
	private static final long serialVersionUID = 1L;
}