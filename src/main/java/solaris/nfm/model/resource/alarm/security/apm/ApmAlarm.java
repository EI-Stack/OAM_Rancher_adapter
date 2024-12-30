package solaris.nfm.model.resource.alarm.security.apm;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.resource.alarm.security.SecurityAlarmBase;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.CvssSeverity;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class ApmAlarm extends SecurityAlarmBase
{
	private static final long	serialVersionUID	= 1L;

	@Enumerated(EnumType.STRING)
	private CvssSeverity		perceivedSeverity;      // CVSS Severity Levels
	@Type(JsonType.class)
	private JsonNode			endPoint;
}
