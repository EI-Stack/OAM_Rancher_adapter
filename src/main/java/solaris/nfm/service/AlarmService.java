package solaris.nfm.service;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.message.amqp.AmqpService;
import solaris.nfm.capability.message.amqp.AmqpSubService.AlarmNotificationType;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.capability.message.websocket.WebSocketService;
import solaris.nfm.capability.rest.RestServiceBase;
import solaris.nfm.capability.system.MailService;
import solaris.nfm.model.base.domain.FaultAlarmBase;
import solaris.nfm.model.base.domain.FaultAlarmBase.AlarmType;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;
import solaris.nfm.model.resource.alarm.fault.fgc.FaultAlarm;
import solaris.nfm.model.resource.alarm.fault.fgc.FaultAlarmDao;
import solaris.nfm.model.resource.alarm.fault.fgc.FaultAlarmDmo;
import solaris.nfm.model.resource.alarm.fault.mec.MecFaultAlarm;
import solaris.nfm.model.resource.alarm.fault.mec.MecFaultAlarmDao;
import solaris.nfm.model.resource.alarm.fault.mec.MecFaultAlarmDmo;
import solaris.nfm.model.resource.alarm.fault.physical.FaultAlarmPhysical;
import solaris.nfm.model.resource.alarm.fault.physical.FaultAlarmPhysicalDao;
import solaris.nfm.model.resource.alarm.fault.physical.FaultAlarmPhysicalDmo;
import solaris.nfm.model.resource.alarm.fault.ric.RicFaultAlarm;
import solaris.nfm.model.resource.alarm.fault.ric.RicFaultAlarmDao;
import solaris.nfm.model.resource.alarm.fault.ric.RicFaultAlarmDmo;
import solaris.nfm.model.resource.alarm.mapping.FaultErrorMessage;
import solaris.nfm.model.resource.alarm.mapping.FaultErrorMessageDao;
import solaris.nfm.model.resource.alarm.security.SecurityAlarmBase;
import solaris.nfm.model.resource.alarm.security.apm.ApmAlarm;
import solaris.nfm.model.resource.alarm.security.apm.ApmAlarmDao;
import solaris.nfm.model.resource.alarm.security.apm.ApmAlarmDmo;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.CvssSeverity;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.DetectionInterface;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.DetectionType;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarmDao;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarmDmo;
import solaris.nfm.model.resource.systemparam.SystemParameter;
import solaris.nfm.model.resource.systemparam.SystemParameterDao;
import solaris.nfm.util.DateTimeUtil;
import solaris.nfm.util.PrimitiveTypeUtil;

@Service
@Slf4j
public class AlarmService extends RestServiceBase
{
	@Autowired
	protected ObjectMapper			objectMapper;
	@Autowired
	private AmqpService				amqpService;
	@Autowired
	private WebSocketService		webSocketService;
	@Autowired
	private MailService				mailService;
	@Autowired
	private FaultErrorMessageDao	errorMessageDao;
	@Autowired
	private SystemParameterDao		systemParameterDao;
	@Autowired
	private FaultAlarmDao			fmAlarmDao;
	@Autowired
	private FaultAlarmDmo			fmAlarmDmo;
	@Autowired
	private MecFaultAlarmDao		mecFmAlarmDao;
	@Autowired
	private MecFaultAlarmDmo		mecFmAlarmDmo;
	@Autowired
	private RicFaultAlarmDao		ricFmAlarmDao;
	@Autowired
	private RicFaultAlarmDmo		ricFmAlarmDmo;
	@Autowired
	private FaultAlarmPhysicalDao	fmAlarmPhysicalDao;
	@Autowired
	private FaultAlarmPhysicalDmo	fmAlarmPhysicalDmo;
	@Autowired
	private DtmAlarmDao				dtmAarmDao;
	@Autowired
	private DtmAlarmDmo				dtmAlarmDmo;
	@Autowired
	private ApmAlarmDao				apmAarmDao;
	@Autowired
	private ApmAlarmDmo				apmAlarmDmo;

	/**
	 * 進行 5GC FM Alarm 處理
	 */
	public void handleFgcFm(final JsonNode msgNode) throws Exception
	{
		// ---[ Must ]-------------------------------------------------------------------------------------------------//
		final NetworkType networkType = NetworkType.valueOf(msgNode.path("networkType").asText().toLowerCase());
		final String alarmId = msgNode.path("alarmId").asText();
		final AlarmType alarmType = AlarmType.valueOf(msgNode.path("alarmType").asText().toUpperCase());
		final String probableCause = msgNode.path("probableCause").asText();
		final PerceivedSeverity perceivedSeverity = PerceivedSeverity.valueOf(msgNode.path("perceivedSeverity").asText().toUpperCase());
		final ZonedDateTime alarmTime = DateTimeUtil.castIsoToUtcZonedDateTime(msgNode.path("alarmTime").asText());

		// ---[ Optional ]---------------------------------------------------------------------------------------------//
		final String proposedRepairActions = (msgNode.path("proposedRepairActions").isValueNode()) ? msgNode.path("proposedRepairActions").asText() : null;
		final String additionalText = (msgNode.path("additionalText").isValueNode()) ? msgNode.path("additionalText").asText() : null;        // Source
		final ObjectNode additionalInformation = (msgNode.path("additionalInformation").isContainerNode()) ? (ObjectNode) msgNode.path("additionalInformation") : null;
		final String alarmErrorCode = (msgNode.path("alarmErrorCode").isValueNode()) ? msgNode.path("alarmErrorCode").asText() : null;

		FaultAlarm faultAlarm = this.fmAlarmDao.findTopByNetworkTypeAndAlarmId(networkType, alarmId);

		if (faultAlarm == null)
		{
			// perceivedSeverity = cleared 代表問題已經解決，那就不需要紀錄
			if (perceivedSeverity == PerceivedSeverity.CLEARED) return;

			final FaultAlarm faultAlarmDetach = new FaultAlarm();
			faultAlarmDetach.setNetworkType(networkType);
			faultAlarmDetach.setAlarmId(alarmId);
			faultAlarmDetach.setPerceivedSeverity(perceivedSeverity);
			faultAlarmDetach.setAlarmRaisedTime(alarmTime);
			faultAlarmDetach.setAlarmType(alarmType);
			faultAlarmDetach.setProbableCause(probableCause);
			faultAlarmDetach.setAdditionalText(additionalText);
			faultAlarmDetach.setAdditionalInformation(additionalInformation);
			faultAlarmDetach.setProposedRepairActions(proposedRepairActions);
			faultAlarmDetach.setDuplicateCount(0);
			faultAlarmDetach.setDuplicateTime(null);
			faultAlarmDetach.setErrorCode(alarmErrorCode);
			// 5GC Only
			faultAlarmDetach.setSourceNetworkFunction(additionalText);
			faultAlarm = this.fmAlarmDmo.createOne(faultAlarmDetach);

			handleAlarm(MessageType.FaultAlarm, faultAlarm);

			return;
		}

		// 在這之後，faultAlarm 必定存在

		if (perceivedSeverity == PerceivedSeverity.CLEARED)
		{
			faultAlarm.setPerceivedSeverity(PerceivedSeverity.CLEARED);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setAlarmClearedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.fmAlarmDmo.removeOne(faultAlarm.getId());
		} else if (faultAlarm.getPerceivedSeverity() != perceivedSeverity)
		{
			faultAlarm.setPerceivedSeverity(perceivedSeverity);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.fmAlarmDmo.modifyOne(faultAlarm);
		} else
		{
			faultAlarm.setDuplicateCount(faultAlarm.getDuplicateCount() + 1);
			faultAlarm.setDuplicateTime(alarmTime);
			this.fmAlarmDmo.modifyOne(faultAlarm);
		}

		handleAlarm(MessageType.FaultAlarm, faultAlarm);
	}

	/**
	 * 進行 MEC FM 處理
	 */
	public void handleMecFm(final JsonNode msgNode) throws Exception
	{
		// ---[ Must ]-------------------------------------------------------------------------------------------------//
		final NetworkType networkType = NetworkType.valueOf(msgNode.path("networkType").asText().toLowerCase());
		final String alarmId = msgNode.path("alarmId").asText();
		final AlarmType alarmType = AlarmType.valueOf(msgNode.path("alarmType").asText().toUpperCase());
		final String probableCause = msgNode.path("probableCause").asText();
		final PerceivedSeverity perceivedSeverity = PerceivedSeverity.valueOf(msgNode.path("perceivedSeverity").asText().toUpperCase());
		final ZonedDateTime alarmTime = DateTimeUtil.castIsoToUtcZonedDateTime(msgNode.path("alarmTime").asText());

		// ---[ Optional ]---------------------------------------------------------------------------------------------//
		final String proposedRepairActions = (msgNode.path("proposedRepairActions").isValueNode()) ? msgNode.path("proposedRepairActions").asText() : null;
		final String additionalText = (msgNode.path("additionalText").isValueNode()) ? msgNode.path("additionalText").asText() : null;        // Source
		final ObjectNode additionalInformation = (msgNode.path("additionalInformation").isContainerNode()) ? (ObjectNode) msgNode.path("additionalInformation") : null;
		final String alarmErrorCode = (msgNode.path("alarmErrorCode").isValueNode()) ? msgNode.path("alarmErrorCode").asText() : null;

		// MEC 所獨有的
		// ---[ Additional ]-------------------------------------------------------------------------------------------//
		final String regionId = msgNode.path("additionalInformation").path("regionId").asText();
		final String appId = msgNode.path("additionalInformation").path("appId").asText();
		final String appIp = msgNode.path("additionalInformation").path("appIp").asText();
		// final String componentId = msgNode.path("additional").path("componentId").asText(); // 目前沒有用到

		MecFaultAlarm faultAlarm = this.mecFmAlarmDao.findTopByNetworkTypeAndAlarmId(networkType, alarmId);

		if (faultAlarm == null)
		{
			// cleared = true 代表問題已經解決，那就不需要紀錄
			if (perceivedSeverity == PerceivedSeverity.CLEARED) return;

			final MecFaultAlarm faultAlarmDetach = new MecFaultAlarm();
			faultAlarmDetach.setNetworkType(networkType);
			faultAlarmDetach.setAlarmId(alarmId);
			faultAlarmDetach.setPerceivedSeverity(perceivedSeverity);
			faultAlarmDetach.setAlarmRaisedTime(alarmTime);
			faultAlarmDetach.setAlarmType(alarmType);
			faultAlarmDetach.setProbableCause(probableCause);
			faultAlarmDetach.setAdditionalText(additionalText);
			faultAlarmDetach.setAdditionalInformation(additionalInformation);
			faultAlarmDetach.setProposedRepairActions(proposedRepairActions);
			faultAlarmDetach.setDuplicateCount(0);
			faultAlarmDetach.setDuplicateTime(null);
			faultAlarmDetach.setErrorCode(alarmErrorCode);
			// MEC 所獨有的
			faultAlarmDetach.setRegionId(regionId);
			faultAlarmDetach.setAppId(appId);
			faultAlarmDetach.setAppIp(appIp);
			faultAlarm = this.mecFmAlarmDmo.createOne(faultAlarmDetach);

			handleAlarm(MessageType.FaultAlarm, faultAlarm);

			return;
		}

		// 在這之後，faultAlarm 必定存在

		if (perceivedSeverity == PerceivedSeverity.CLEARED)
		{
			faultAlarm.setPerceivedSeverity(PerceivedSeverity.CLEARED);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setAlarmClearedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.mecFmAlarmDmo.removeOne(faultAlarm.getId());
		} else if (faultAlarm.getPerceivedSeverity() != perceivedSeverity)
		{
			faultAlarm.setPerceivedSeverity(perceivedSeverity);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.mecFmAlarmDmo.modifyOne(faultAlarm);
		} else
		{
			faultAlarm.setDuplicateCount(faultAlarm.getDuplicateCount() + 1);
			faultAlarm.setDuplicateTime(alarmTime);
			this.mecFmAlarmDmo.modifyOne(faultAlarm);
		}

		handleAlarm(MessageType.FaultAlarm, faultAlarm);
	}

	/**
	 * 進行 RIC FM 處理
	 */
	public void handleRicFm(final JsonNode msgNode) throws Exception
	{
		// ---[ Must ]-------------------------------------------------------------------------------------------------//
		final NetworkType networkType = NetworkType.valueOf(msgNode.path("networkType").asText().toLowerCase());
		final String alarmId = msgNode.path("alarmId").asText();
		final AlarmType alarmType = AlarmType.valueOf(msgNode.path("alarmType").asText().toUpperCase());
		final String probableCause = msgNode.path("probableCause").asText();
		final PerceivedSeverity perceivedSeverity = PerceivedSeverity.valueOf(msgNode.path("perceivedSeverity").asText().toUpperCase());
		final ZonedDateTime alarmTime = DateTimeUtil.castIsoToUtcZonedDateTime(msgNode.path("alarmTime").asText());

		// ---[ Optional ]---------------------------------------------------------------------------------------------//
		final String proposedRepairActions = (msgNode.path("proposedRepairActions").isValueNode()) ? msgNode.path("proposedRepairActions").asText() : null;
		final String additionalText = (msgNode.path("additionalText").isValueNode()) ? msgNode.path("additionalText").asText() : null;        // Source
		final ObjectNode additionalInformation = (msgNode.path("additionalInformation").isContainerNode()) ? (ObjectNode) msgNode.path("additionalInformation") : null;
		final String alarmErrorCode = (msgNode.path("alarmErrorCode").isValueNode()) ? msgNode.path("alarmErrorCode").asText() : null;

		// RIC 獨有的
		// ---[ Additional ]-------------------------------------------------------------------------------------------//
		final String fieldId = msgNode.path("additionalInformation").path("fieldId").asText();
		final String nci = msgNode.path("additionalInformation").path("nci").asText();

		RicFaultAlarm faultAlarm = this.ricFmAlarmDao.findTopByNetworkTypeAndAlarmId(networkType, alarmId);

		if (faultAlarm == null)
		{
			// cleared = true 代表問題已經解決，那就不需要紀錄
			if (perceivedSeverity == PerceivedSeverity.CLEARED) return;

			final RicFaultAlarm faultAlarmDetach = new RicFaultAlarm();
			faultAlarmDetach.setNetworkType(networkType);
			faultAlarmDetach.setAlarmId(alarmId);
			faultAlarmDetach.setPerceivedSeverity(perceivedSeverity);
			faultAlarmDetach.setAlarmRaisedTime(alarmTime);
			faultAlarmDetach.setAlarmType(alarmType);
			faultAlarmDetach.setProbableCause(probableCause);
			faultAlarmDetach.setAdditionalText(additionalText);
			faultAlarmDetach.setAdditionalInformation(additionalInformation);
			faultAlarmDetach.setProposedRepairActions(proposedRepairActions);
			faultAlarmDetach.setDuplicateCount(0);
			faultAlarmDetach.setDuplicateTime(null);
			faultAlarmDetach.setErrorCode(alarmErrorCode);
			// RIC 獨有的
			faultAlarmDetach.setFieldId(fieldId);
			faultAlarmDetach.setNci(nci);
			faultAlarm = this.ricFmAlarmDmo.createOne(faultAlarmDetach);

			handleAlarm(MessageType.FaultAlarm, faultAlarm);

			return;
		}

		// 在這之後，faultAlarm 必定存在

		if (perceivedSeverity == PerceivedSeverity.CLEARED)
		{
			faultAlarm.setPerceivedSeverity(PerceivedSeverity.CLEARED);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setAlarmClearedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.ricFmAlarmDmo.removeOne(faultAlarm.getId());
		} else if (faultAlarm.getPerceivedSeverity() != perceivedSeverity)
		{
			faultAlarm.setPerceivedSeverity(perceivedSeverity);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.ricFmAlarmDmo.modifyOne(faultAlarm);
		} else
		{
			faultAlarm.setDuplicateCount(faultAlarm.getDuplicateCount() + 1);
			faultAlarm.setDuplicateTime(alarmTime);
			this.ricFmAlarmDmo.modifyOne(faultAlarm);
		}

		handleAlarm(MessageType.FaultAlarm, faultAlarm);
	}

	/**
	 * 進行 Physical FM 處理
	 */
	public void handlePhysicalFm(final JsonNode msgNode) throws Exception
	{
		// ---[ Must ]-------------------------------------------------------------------------------------------------//
		final NetworkType networkType = NetworkType.valueOf(msgNode.path("networkType").asText().toLowerCase());
		final String alarmId = msgNode.path("alarmId").asText();
		final AlarmType alarmType = AlarmType.valueOf(msgNode.path("alarmType").asText().toUpperCase());
		final String probableCause = msgNode.path("probableCause").asText();
		final PerceivedSeverity perceivedSeverity = PerceivedSeverity.valueOf(msgNode.path("perceivedSeverity").asText().toUpperCase());
		final ZonedDateTime alarmTime = DateTimeUtil.castIsoToUtcZonedDateTime(msgNode.path("alarmTime").asText());

		// ---[ Optional ]---------------------------------------------------------------------------------------------//
		final String proposedRepairActions = (msgNode.path("proposedRepairActions").isValueNode()) ? msgNode.path("proposedRepairActions").asText() : null;
		final String additionalText = (msgNode.path("additionalText").isValueNode()) ? msgNode.path("additionalText").asText() : null;        // Source
		final ObjectNode additionalInformation = (msgNode.path("additionalInformation").isContainerNode()) ? (ObjectNode) msgNode.path("additionalInformation") : null;
		final String alarmErrorCode = (msgNode.path("alarmErrorCode").isValueNode()) ? msgNode.path("alarmErrorCode").asText() : null;

		FaultAlarmPhysical faultAlarm = this.fmAlarmPhysicalDao.findTopByNetworkTypeAndAlarmId(networkType, alarmId);

		if (faultAlarm == null)
		{
			// cleared = true 代表問題已經解決，那就不需要紀錄
			if (perceivedSeverity == PerceivedSeverity.CLEARED) return;

			final FaultAlarmPhysical faultAlarmDetach = new FaultAlarmPhysical();
			faultAlarmDetach.setNetworkType(networkType);
			faultAlarmDetach.setAlarmId(alarmId);
			faultAlarmDetach.setPerceivedSeverity(perceivedSeverity);
			faultAlarmDetach.setAlarmRaisedTime(alarmTime);
			faultAlarmDetach.setAlarmType(alarmType);
			faultAlarmDetach.setProbableCause(probableCause);
			faultAlarmDetach.setAdditionalText(additionalText);
			faultAlarmDetach.setAdditionalInformation(additionalInformation);
			faultAlarmDetach.setProposedRepairActions(proposedRepairActions);
			faultAlarmDetach.setDuplicateCount(0);
			faultAlarmDetach.setDuplicateTime(null);
			faultAlarmDetach.setErrorCode(alarmErrorCode);
			faultAlarm = this.fmAlarmPhysicalDmo.createOne(faultAlarmDetach);

			handleAlarm(MessageType.FaultAlarm, faultAlarm);
			return;
		}

		// 在這之後，faultAlarm 必定存在

		if (perceivedSeverity == PerceivedSeverity.CLEARED)
		{
			faultAlarm.setPerceivedSeverity(PerceivedSeverity.CLEARED);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setAlarmClearedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.fmAlarmPhysicalDmo.removeOne(faultAlarm.getId());
		} else if (faultAlarm.getPerceivedSeverity() != perceivedSeverity)
		{
			faultAlarm.setPerceivedSeverity(perceivedSeverity);
			faultAlarm.setAlarmChangedTime(alarmTime);
			faultAlarm.setDuplicateCount(0);
			faultAlarm.setDuplicateTime(null);
			this.fmAlarmPhysicalDmo.modifyOne(faultAlarm);
		} else
		{
			faultAlarm.setDuplicateCount(faultAlarm.getDuplicateCount() + 1);
			faultAlarm.setDuplicateTime(alarmTime);
			this.fmAlarmPhysicalDmo.modifyOne(faultAlarm);
		}

		handleAlarm(MessageType.FaultAlarm, faultAlarm);
	}

	/**
	 * 進行資安戰術協防 DTM alarm 處理
	 */
	public void handleSmDtmAlarm(final JsonNode msgNode) throws Exception
	{
		// ---[ Required ]---------------------------------------------------------------------------------------------//
		final AlarmNotificationType notificationType = AlarmNotificationType.valueOf(msgNode.path("notificationType").asText());
		final String alarmId = msgNode.path("alarmId").asText();
		final ZonedDateTime alarmTime = DateTimeUtil.castIsoToUtcZonedDateTime(msgNode.path("alarmTime").asText());
		final CvssSeverity perceivedSeverity = CvssSeverity.valueOf(msgNode.path("perceivedSeverity").asText());
		final DetectionInterface detectionInterface = DetectionInterface.valueOf(msgNode.path("interface").asText().replace("/", ""));
		final DetectionType detectionType = DetectionType.valueOf(msgNode.path("detectionType").asText());

		// ---[ Optional ]---------------------------------------------------------------------------------------------//
		final JsonNode endPoint = (msgNode.path("endPoint").isContainerNode()) ? msgNode.path("endPoint") : null;
		final String probableCause = (msgNode.path("probableCause").isValueNode()) ? msgNode.path("probableCause").asText() : null;
		final String proposedRepairActions = (msgNode.path("proposedRepairActions").isValueNode()) ? msgNode.path("proposedRepairActions").asText() : null;

		final ObjectNode additionalInformation = (msgNode.path("additionalInformation").isContainerNode()) ? (ObjectNode) msgNode.path("additionalInformation") : JsonNodeFactory.instance.objectNode();
		if (msgNode.path("SeverityNumber").isValueNode())
		{
			additionalInformation.put("severityNumber", msgNode.path("SeverityNumber").asDouble());
		}

		DtmAlarm alarmEntity = this.dtmAarmDao.findTopByAlarmId(alarmId);

		if (alarmEntity == null)
		{
			// cleared = true 代表問題已經解決，那就不需要紀錄
			if (notificationType == AlarmNotificationType.notifyClearedAlarm) return;

			final DtmAlarm alarmDetach = new DtmAlarm();
			alarmDetach.setAlarmId(alarmId);
			alarmDetach.setPerceivedSeverity(perceivedSeverity);
			alarmDetach.setAlarmRaisedTime(alarmTime);
			alarmDetach.setProbableCause(probableCause);
			alarmDetach.setSource(null);
			alarmDetach.setAdditionalInformation(additionalInformation);
			alarmDetach.setProposedRepairActions(proposedRepairActions);
			alarmDetach.setDuplicateCount(0);
			alarmDetach.setDuplicateTime(null);
			alarmDetach.setErrorCode(null);
			// InfoSecurity DTM Only
			alarmDetach.setDetectionType(detectionType);
			alarmDetach.setDetectionInterface(detectionInterface);
			alarmDetach.setEndPoint(endPoint);
			alarmEntity = this.dtmAlarmDmo.createOne(alarmDetach);

			handleSecurityAlarm(MessageType.SecurityDtmAlarm, alarmEntity);
			return;
		}

		// 在這之後，faultAlarm 必定存在

		if (notificationType == AlarmNotificationType.notifyClearedAlarm)
		{
			alarmEntity.setAlarmChangedTime(alarmTime);
			alarmEntity.setAlarmClearedTime(alarmTime);
			alarmEntity.setDuplicateCount(0);
			alarmEntity.setDuplicateTime(null);
			this.dtmAlarmDmo.removeOne(alarmEntity.getId());
		} else if (alarmEntity.getPerceivedSeverity() != perceivedSeverity)
		{
			alarmEntity.setPerceivedSeverity(perceivedSeverity);
			alarmEntity.setAlarmChangedTime(alarmTime);
			alarmEntity.setDuplicateCount(0);
			alarmEntity.setDuplicateTime(null);
			this.dtmAlarmDmo.modifyOne(alarmEntity);
		} else
		{
			alarmEntity.setDuplicateCount(alarmEntity.getDuplicateCount() + 1);
			alarmEntity.setDuplicateTime(alarmTime);
			this.dtmAlarmDmo.modifyOne(alarmEntity);
		}

		handleSecurityAlarm(MessageType.SecurityDtmAlarm, alarmEntity);
	}

	/**
	 * 進行資安聯合偵防 APM alarm 處理
	 */
	public void handleSmApmAlarm(final JsonNode msgNode) throws Exception
	{
		final AlarmNotificationType notificationType = AlarmNotificationType.valueOf(msgNode.path("notificationType").asText());
		final String alarmId = msgNode.path("eventId").asText();
		final ZonedDateTime alarmTime = DateTimeUtil.castIsoToUtcZonedDateTime(msgNode.path("eventTime").asText());
		final CvssSeverity perceivedSeverity = CvssSeverity.valueOf(msgNode.path("perceivedSeverity").asText());
		final ObjectNode additionalInformation = JsonNodeFactory.instance.objectNode();
		additionalInformation.set("namespace", msgNode.path("namespace"));
		additionalInformation.set("pod", msgNode.path("pod"));
		additionalInformation.set("policyTypes", msgNode.path("policyTypes"));

		final String probableCause = msgNode.path("detail").asText();
		final JsonNode endPoint = msgNode.path("endPoint");
		final String proposedRepairActions = msgNode.path("recommend").asText();

		ApmAlarm alarmEntity = this.apmAarmDao.findTopByAlarmId(alarmId);

		if (alarmEntity == null)
		{
			// cleared = true 代表問題已經解決，那就不需要紀錄
			if (notificationType == AlarmNotificationType.notifyClearedAlarm) return;

			final ApmAlarm alarmDetach = new ApmAlarm();

			alarmDetach.setAlarmId(alarmId);
			alarmDetach.setPerceivedSeverity(perceivedSeverity);
			alarmDetach.setAlarmRaisedTime(alarmTime);
			alarmDetach.setProbableCause(probableCause);
			alarmDetach.setSource(null);
			alarmDetach.setAdditionalInformation(additionalInformation);
			alarmDetach.setProposedRepairActions(proposedRepairActions);
			alarmDetach.setDuplicateCount(0);
			alarmDetach.setDuplicateTime(null);
			alarmDetach.setErrorCode(null);
			// InfoSecurity APM Only
			alarmDetach.setEndPoint(endPoint);
			alarmEntity = this.apmAlarmDmo.createOne(alarmDetach);

			handleSecurityAlarm(MessageType.SecurityApmAlarm, alarmEntity);
			return;
		}

		// 在這之後，faultAlarm 必定存在

		if (notificationType == AlarmNotificationType.notifyClearedAlarm)
		{
			alarmEntity.setAlarmChangedTime(alarmTime);
			alarmEntity.setAlarmClearedTime(alarmTime);
			alarmEntity.setDuplicateCount(0);
			alarmEntity.setDuplicateTime(null);
			this.apmAlarmDmo.removeOne(alarmEntity.getId());
		} else if (alarmEntity.getPerceivedSeverity() != perceivedSeverity)
		{
			alarmEntity.setPerceivedSeverity(perceivedSeverity);
			alarmEntity.setAlarmChangedTime(alarmTime);
			alarmEntity.setDuplicateCount(0);
			alarmEntity.setDuplicateTime(null);
			this.apmAlarmDmo.modifyOne(alarmEntity);
		} else
		{
			alarmEntity.setDuplicateCount(alarmEntity.getDuplicateCount() + 1);
			alarmEntity.setDuplicateTime(alarmTime);
			this.apmAlarmDmo.modifyOne(alarmEntity);
		}

		handleSecurityAlarm(MessageType.SecurityApmAlarm, alarmEntity);
	}

	/**
	 * 處理 Fault Alarm 的發送作業
	 */
	public void handleAlarm(final MessageType messageType, final FaultAlarmBase faultAlarm) throws JsonParseException, JsonMappingException, IOException
	{
		final MessageBean messageBean = new MessageBean(faultAlarm.getNetworkType(), messageType);
		final ObjectNode contentNode = this.objectMapper.valueToTree(faultAlarm);

		FaultErrorMessage fem = null;
		if (StringUtils.hasText(faultAlarm.getErrorCode()) && PrimitiveTypeUtil.isInteger(faultAlarm.getErrorCode()))
		{
			fem = this.errorMessageDao.findTopByNetworkTypeAndCode(faultAlarm.getNetworkType(), Integer.parseInt(faultAlarm.getErrorCode()));
		}
		log.debug("={}", (new ObjectMapper()).valueToTree(fem).toPrettyString());
		final String mappedAlarmDescription = (fem != null) ? fem.getMessage() : "";
		final String mappedRecommendedOperation = (fem != null) ? fem.getSop() : "";

		contentNode.put("mappedAlarmDescription", mappedAlarmDescription);
		contentNode.put("mappedRecommendedOperation", mappedRecommendedOperation);
		messageBean.setContent(contentNode);
		log.debug("\t messageBean (FM)=\n{}", this.objectMapper.valueToTree(messageBean).toPrettyString());

		// Send AMQP
		this.amqpService.sendMsgToLm(messageBean);
		// Send WebSocket
		this.webSocketService.broadcastAll(this.objectMapper.valueToTree(messageBean).toPrettyString());
		// Send mail
		sendMailForFm(faultAlarm, fem);
	}

	/**
	 * 處理 Security Alarm 的發送作業
	 */
	public void handleSecurityAlarm(final MessageType messageType, final SecurityAlarmBase baseAlarm) throws JsonParseException, JsonMappingException, IOException
	{
		final MessageBean messageBean = switch (messageType)
		{
			case SecurityDtmAlarm -> new MessageBean(NetworkType.fgc, MessageType.SecurityDtmAlarm);
			case SecurityApmAlarm -> new MessageBean(NetworkType.mec, MessageType.SecurityApmAlarm);
			default -> throw new IllegalArgumentException("Unexpected value: " + messageType);
		};

		final ObjectNode contentNode = this.objectMapper.valueToTree(baseAlarm);

		FaultErrorMessage fem = null;
		if (StringUtils.hasText(baseAlarm.getErrorCode()) && PrimitiveTypeUtil.isInteger(baseAlarm.getErrorCode()))
		{
			fem = this.errorMessageDao.findTopByNetworkTypeAndCode(NetworkType.fgc, Integer.parseInt(baseAlarm.getErrorCode()));
		}
		final String mappedAlarmDescription = (fem != null) ? fem.getMessage() : "";
		final String mappedRecommendedOperation = (fem != null) ? fem.getSop() : "";

		contentNode.put("mappedAlarmDescription", mappedAlarmDescription);
		contentNode.put("mappedRecommendedOperation", mappedRecommendedOperation);
		messageBean.setContent(contentNode);
		log.debug("\t Security MessageBean =\n{}", this.objectMapper.valueToTree(messageBean).toPrettyString());

		// Send AMQP
		this.amqpService.sendMsgToLm(messageBean);
		// Send WebSocket
		this.webSocketService.broadcastAll(this.objectMapper.valueToTree(messageBean).toPrettyString());
		// Send mail
		// sendMailForFm(faultAlarm, fem);
	}

	@Async
	private void sendMailForFm(final FaultAlarmBase faultAlarm, final FaultErrorMessage fem) throws JsonParseException, JsonMappingException, IOException
	{
		final PerceivedSeverity severity = faultAlarm.getPerceivedSeverity();
		Set<String> mailAddresses = null;

		// Mail 處理邏輯
		// ErrorMessage 優先度大於 Severity。若 ErrorMessage.mailDisabled = false，就會覆蓋 Severity 的設定

		// 取得 severity 層級的 mail address
		final SystemParameter systemParameter = this.systemParameterDao.findTopByName("fmAlarmMailAddressSetting");
		if (systemParameter != null)
		{
			final JsonNode severityNode = systemParameter.getParameter().path(severity.name());
			final Boolean mailDisabled = severityNode.path("mailDisabled").asBoolean();
			if (mailDisabled == false) mailAddresses = this.objectMapper.readValue(severityNode.path("mailAddresses").traverse(), new TypeReference<LinkedHashSet<String>>()
			{});
		}

		// 取得 ErrorMessage 層級的 mail address
		if (fem != null && fem.getMailDisabled() == false && fem.getMailAddresses() != null) mailAddresses = new HashSet<>(fem.getMailAddresses());

		if (mailAddresses == null || mailAddresses.size() == 0) return;
		for (final String mailAddress : mailAddresses)
			if (mailAddress == null) return;

		String emailContent = MessageFormat.format("Network: {0}\nSeverity: {1}\nError Code: {2}\nDescription: {3}\nEvent Time: {4}\nProposed Repair Actions: {5}", faultAlarm.getNetworkType().name(),
				severity, faultAlarm.getErrorCode(), faultAlarm.getProbableCause(), DateTimeUtil.castZonedDateTimeToString(faultAlarm.getAlarmRaisedTime()), faultAlarm.getProposedRepairActions());
		if (faultAlarm.getNetworkType().equals(NetworkType.mec))
		{
			final MecFaultAlarm mecFaultAlarm = (MecFaultAlarm) faultAlarm;
			final String extra = MessageFormat.format("\nRegion ID: {0}\nApp ID: {1}\nApp IP: {2}", mecFaultAlarm.getRegionId(), mecFaultAlarm.getAppId(), mecFaultAlarm.getAppIp());
			emailContent += extra;
		}
		if (faultAlarm.getNetworkType().equals(NetworkType.ric))
		{
			final RicFaultAlarm ricFaultAlarm = (RicFaultAlarm) faultAlarm;
			final String extra = MessageFormat.format("\nField ID: {0}\nNR Cell ID: {1}", ricFaultAlarm.getFieldId(), ricFaultAlarm.getNci());
			emailContent += extra;
		}
		log.debug("\t[Mail] Mail Addresses={}", mailAddresses);
		log.debug("\t[Mail] Mail Content=\n{}", emailContent);

		this.mailService.sendFm(mailAddresses, emailContent);
	}
}