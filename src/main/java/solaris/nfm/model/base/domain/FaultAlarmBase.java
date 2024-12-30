package solaris.nfm.model.base.domain;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.resource.alarm.fault.comment.Comment;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = false)
public class FaultAlarmBase extends IdentityEntityBase
{
	private static final long		serialVersionUID	= 1L;

	@Enumerated(EnumType.STRING)
	private NetworkType				networkType;

	private String					alarmId;                // ->name;
	@Enumerated(EnumType.STRING)
	private PerceivedSeverity		perceivedSeverity;
	private ZonedDateTime			alarmChangedTime;
	private ZonedDateTime			alarmRaisedTime;
	@Enumerated(EnumType.STRING)
	private AlarmType				alarmType;
	private String					probableCause;          // ->faultErrorCode
	private String					additionalText;         // ->source
	@Type(JsonType.class)
	private ObjectNode				additionalInformation;  // TS28623 AttributeNameValuePairSet
	private String					proposedRepairActions;

	// ---[ Comment ]--------------------------------------------------------------------------------------------------
	@Type(JsonType.class)
	private Map<String, Comment>	comments			= new LinkedHashMap<>();

	// ---[ Acknowledge ]----------------------------------------------------------------------------------------------
	@Enumerated(EnumType.STRING)
	private AckState				ackState;
	private ZonedDateTime			ackTime;
	private Long					ackUserId;
	private String					ackUserName;
	// private String ackSystemId; // Optional

	// ---[ Clear ]----------------------------------------------------------------------------------------------------
	private Long					clearUserId;
	private String					clearUserName;
	private ZonedDateTime			alarmClearedTime;
	// private String clearSystemId; // Optional

	// ---[ Duplicate ]------------------------------------------------------------------------------------------------
	private Integer					duplicateCount;
	private ZonedDateTime			duplicateTime;

	// ---[ Error Message Mapping ]------------------------------------------------------------------------------------
	private String					errorCode;

	public enum NetworkType
	{
		fgc,
		mec,
		ric,
		physical;
	}

	public enum AlarmType
	{
		COMMUNICATIONS_ALARM,
		QUALITY_OF_SERVICE_ALARM,
		PROCESSING_ERROR_ALARM,
		EQUIPMENT_ALARM,
		ENVIRONMENTAL_ALARM,
		INTEGRITY_VIOLATION,
		OPERATIONAL_VIOLATION,
		PHYSICAL_VIOLATION,
		SECURITY_SERVICE_OR_MECHANISM_VIOLATION,
		TIME_DOMAIN_VIOLATION;
	}

	public enum PerceivedSeverity
	{
		INDETERMINATE,
		CRITICAL,
		MAJOR,
		MINOR,
		WARNING,
		CLEARED;
	}

	public enum AckState
	{
		ACKNOWLEDGED,
		UNACKNOWLEDGED;
	}
}