package solaris.nfm.model.resource.openflow.tnslice;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import solaris.nfm.controller.dto.SwitchNodeDto;

@Data
public class TnSliceDto
{
	@NotNull(groups = Create.class)
	@Min(1)
	private Integer				vlanId;
	@NotNull(groups = Create.class)
	private Integer				pcp;
	@NotNull(groups = Create.class)
	@Valid
	private SwitchNodeDto		headNode;
	@NotNull(groups = Create.class)
	@Valid
	private SwitchNodeDto		tailNode;
	private List<SwitchNodeDto>	middleNodes	= new ArrayList<>();

	@NotNull(groups = Create.class)
	@Min(0)
	private Long				uplinkMaxBitrate;
	@NotNull(groups = Create.class)
	@Min(0)
	private Long				downlinkMaxBitrate;
	@NotNull(groups = Create.class)
	@Min(0)
	private Long				uplinkMinBitrate;
	@NotNull(groups = Create.class)
	@Min(0)
	private Long				downlinkMinBitrate;
	@NotNull(groups = Create.class)
	private String				destMac;

	public interface Create
	{}

	public interface Update
	{}
}
