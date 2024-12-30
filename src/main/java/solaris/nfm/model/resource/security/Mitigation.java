package solaris.nfm.model.resource.security;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.CvssSeverity;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.DetectionInterface;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.DetectionType;

/**
 * 資安緩解措施
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class Mitigation extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private String				name;
	private String				description;
	@Enumerated(EnumType.STRING)
	private DetectionType		detectionType;
	@Enumerated(EnumType.STRING)
	private DetectionInterface	detectionInterface;
	@Enumerated(EnumType.STRING)
	private CvssSeverity		perceivedSeverity;
	@Type(JsonType.class)
	private ArrayNode			procedures;
	@Type(JsonType.class)
	private ArrayNode			parameters;
}
