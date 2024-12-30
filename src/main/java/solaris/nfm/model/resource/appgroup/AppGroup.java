package solaris.nfm.model.resource.appgroup;

import jakarta.persistence.Entity;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

import io.hypersistence.utils.hibernate.type.json.JsonType;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class AppGroup extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				name;
	private String				uuid;
	@Type(JsonType.class)
	private JsonNode			paasRequest;
	private String				description;

	@Type(JsonType.class)
	private ArrayNode			appInfos;  // 裡面存放 storage 與 service 資料

	public enum AppStatus
	{
		Queuing,
		Running,
		Error,
		Stop,
		Deleted;
	}

	public enum PodPhase
	{
		Pending,
		Running,
		Succeeded,
		Failed,
		Unknown;
	}
}