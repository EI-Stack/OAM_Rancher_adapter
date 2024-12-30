package solaris.nfm.model.base.domain;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class AppFileBase extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;
	private String				name;
	private String				description;
	private String				version;
	private Integer				size;
	private String				label;
}
