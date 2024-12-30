package solaris.nfm.model.resource.alarm.security.dtm;

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

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class DtmAlarm extends SecurityAlarmBase
{
	private static final long	serialVersionUID	= 1L;

	@Enumerated(EnumType.STRING)
	private CvssSeverity		perceivedSeverity;      // CVSS Severity Levels
	@Enumerated(EnumType.STRING)
	private DetectionType		detectionType;
	@Enumerated(EnumType.STRING)
	private DetectionInterface	detectionInterface;
	@Type(JsonType.class)
	private JsonNode			endPoint;

	public enum CvssSeverity
	{
		Critical,
		High,
		Medium,
		Low,
		Any;
	}

	public enum DetectionType
	{
		DoS,
		BiddingDownAttacks,
		Sniffing,
		IpDepletion,
		Spoofing,
		Other,
		NetworkDos,
		EndpointDos,
		MITM,
		MITM_ARP,
		Any,
		Expolits,
		Hijack,
		MecAttack,
		Tamper;
	}

	public enum DetectionInterface
	{
		N1N2,
		N3,
		Any;
	}
}