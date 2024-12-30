package solaris.nfm.model.resource.mecapppackage;

import java.time.LocalDateTime;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class MecAppPackage extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	@Column(length = 64, nullable = false)
	private String				name;
	@Column(length = 1024)
	private String				description;
	@Column(length = 102400)
	private String				icon;
	@Type(JsonType.class)
	private JsonNode			files;
	@Column(columnDefinition = "TIMESTAMP", nullable = false)
	private LocalDateTime		creationTime;
}