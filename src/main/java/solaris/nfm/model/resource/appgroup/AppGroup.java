package solaris.nfm.model.resource.appgroup;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class), @TypeDef(name = "json-node", typeClass = JsonNodeBinaryType.class)})
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class AppGroup extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				name;
	private String				uuid;
	@Type(type = "json-node")
	@Column(columnDefinition = "jsonb")
	private JsonNode			paasRequest;
	private String				description;

	@Type(type = "json-node")
	@Column(columnDefinition = "jsonb")
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