package solaris.nfm.model.resource.mgroup;

import java.util.List;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class MailGroup extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				name;
	@Type(ListArrayType.class)
	private List<String>		mailAddresses;
}
