package solaris.nfm.model.resource.alarm.security;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;
import solaris.nfm.model.resource.alarm.fault.comment.Comment;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = false)
public class SecurityAlarmBase extends IdentityEntityBase
{
	private static final long		serialVersionUID	= 1L;

	private String					alarmId;
	private ZonedDateTime			alarmChangedTime;
	private ZonedDateTime			alarmRaisedTime;
	private String					probableCause;
	private String					source;
	@Type(JsonType.class)
	private ObjectNode				additionalInformation;  // TS28623 AttributeNameValuePairSet
	private String					proposedRepairActions;

	// ---[ Comment ]--------------------------------------------------------------------------------------------------
	@Type(JsonType.class)
	private Map<String, Comment>	comments			= new LinkedHashMap<>();

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
}