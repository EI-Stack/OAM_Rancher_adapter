package solaris.nfm.model.resource.slice.template;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class NetworkSlice extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				name;
	private String				description;
	@Type(JsonType.class)
	private ArrayNode			serviceProfiles;
}