package solaris.nfm.model.resource.profile;

import java.time.ZonedDateTime;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class Profile extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				name;
	@Enumerated(EnumType.STRING)
	private ProfileType			type;
	private String				description;
	@Type(JsonType.class)
	private ObjectNode			json;
	private ZonedDateTime		changeTime;

	public enum ProfileType
	{
		UE_Subscription;
	}
}
