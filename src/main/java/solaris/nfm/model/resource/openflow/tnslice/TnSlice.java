package solaris.nfm.model.resource.openflow.tnslice;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.controller.dto.SwitchNodeDto;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class TnSlice extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private int					vlanId;
	private int					pcp;

	@Type(JsonType.class)
	private SwitchNodeDto		headNode;
	@Type(JsonType.class)
	private SwitchNodeDto		tailNode;
	@Type(JsonType.class)
	private List<SwitchNodeDto>	middleNodes			= new ArrayList<>();

	private long				uplinkMaxBitrate;
	private long				downlinkMaxBitrate;
	private long				uplinkMinBitrate;
	private long				downlinkMinBitrate;

	private String				destMac;
}
