package solaris.nfm.model.base.domain;

import javax.persistence.MappedSuperclass;

import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class FileBase extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;
	private String				name;
	private String				description;
	private String				version;
	private Long				size;
}