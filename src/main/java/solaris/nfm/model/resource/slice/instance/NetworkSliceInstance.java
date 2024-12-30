package solaris.nfm.model.resource.slice.instance;

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
public class NetworkSliceInstance extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				description;
	private Long				networkSliceId;
	@Type(ListArrayType.class)
	private List<String>		ricSliceList;
}
