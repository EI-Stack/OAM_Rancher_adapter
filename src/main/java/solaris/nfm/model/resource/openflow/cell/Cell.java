package solaris.nfm.model.resource.openflow.cell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class Cell extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;
	@NotNull
	private String				mac;
	private Long				tnsliceId; // 是關聯到 vlan.id 不是 vlan.VlanId
}