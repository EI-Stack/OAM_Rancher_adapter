package solaris.nfm.capability.message.amqp;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.JsonValidationService;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.capability.message.websocket.WebSocketService;
import solaris.nfm.capability.system.MailService;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;
import solaris.nfm.model.resource.alarm.performance.alarm.PerformanceAlarm;
import solaris.nfm.model.resource.alarm.performance.alarm.PerformanceAlarmDao;
import solaris.nfm.model.resource.alarm.performance.alarm.PerformanceAlarmDmo;
import solaris.nfm.model.resource.alarm.performance.rule.PerformanceRule;
import solaris.nfm.model.resource.alarm.performance.rule.PerformanceRule.Comparison;
import solaris.nfm.model.resource.alarm.performance.rule.PerformanceRuleDao;
import solaris.nfm.model.resource.alarm.performance.rule.PerformanceRuleDmo;
import solaris.nfm.model.resource.systemparam.SystemParameter;
import solaris.nfm.model.resource.systemparam.SystemParameterDao;
import solaris.nfm.service.AlarmService;
import solaris.nfm.util.DateTimeUtil;

@Service
@Slf4j
public class AmqpSubService
{
	@Autowired
	private ObjectMapper		objectMapper;
	@Autowired
	private AmqpService			amqpService;
	@Autowired
	private WebSocketService	webSocketService;
	@Autowired
	private PerformanceRuleDao	ruleDao;
	@Autowired
	private PerformanceRuleDmo	ruleDmo;
	@Autowired
	private PerformanceAlarmDao	pmAlarmDao;
	@Autowired
	private PerformanceAlarmDmo	pmAlarmDmo;
	@Autowired
	private SystemParameterDao	systemParameterDao;
	@Autowired
	private MailService			mailService;
	@Autowired
	private AlarmService		alarmService;
	@Autowired
	private JsonValidationService	jsonValidationService;

	/**
	 * 從 Adapter 取得事件，進行後續處理
	 */
	@RabbitListener(id = "adapter-to-nfm", queues = "adapter-to-nfm", containerFactory = "rabbitListenerContainerFactory", concurrency = "1", autoStartup = "false")
	public void listenFromAdapter(final List<Message> messages, final Channel channel) throws Exception
	{
		final String queueName = "adapter-to-nfm";
		try
		{
			for (final Message tmpMessage : messages)
			{
				final JsonNode message = castMessageToJsonNode(tmpMessage);
				log.debug("\t[Message] Message from Adapter: \n{}", message.toPrettyString());
				// Validate JSON data structure
				jsonValidationService.validateForAmqpEnvelope(message, queueName);

				final MessageType messageType = MessageType.valueOf(message.path("messageType").asText());
				final JsonNode content = message.path("content");
				jsonValidationService.validateForAmqp(content, queueName, messageType);
				NetworkType networkType = null;

				switch (messageType)
				{
					case FaultAlarm :
						networkType = NetworkType.valueOf(content.path("networkType").asText());
						switch (networkType)
						{
							case fgc :
								this.alarmService.handleFgcFm(content);
								break;
							case mec :
								this.alarmService.handleMecFm(content);
								break;
							case ric :
								this.alarmService.handleRicFm(content);
								break;
							case physical :
								this.alarmService.handlePhysicalFm(content);
								break;
							default :
								log.error("\t[Message] Unknown networkType=[{}]", networkType);
								break;
						}
						break;
					case SecurityDtmAlarm :
						this.alarmService.handleSmDtmAlarm(content);
						break;
					case SecurityApmAlarm :
						this.alarmService.handleSmApmAlarm(content);
						break;
					case PerformanceMeasurementData :
						networkType = NetworkType.valueOf(content.path("networkType").asText());
						handlePmMeasurementData(content, networkType);
						break;
					default :
						log.error("\t[Message] Unknown messageType=[{}]", messageType);
						break;
				}
			}
		} catch (final ExceptionBase e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				channel.basicAck(messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag(), false);
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * 將 message 轉送至 log Manager
	 */
	private void transferMessageToLm(final JsonNode msgNode, final NetworkType networkType, final MessageType messageType)
	{
		final ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
		jsonNode.put("networkType", networkType.name());
		jsonNode.put("messageType", messageType.name());
		jsonNode.put("messageTime", DateTimeUtil.LocalDateTimeToMills(LocalDateTime.now()));
		final ObjectNode content = jsonNode.putObject("content");

		if (msgNode.path("content").isMissingNode())
		{
			content.setAll((ObjectNode) msgNode);
		} else
		{
			content.setAll((ObjectNode) msgNode.path("content"));
		}

		// log.debug("transferMessageToLm=\n{}", jsonNode.toPrettyString());
		this.amqpService.sendMsgToLm(jsonNode);
	}

	/**
	 * 進行 5GC/MEC/RIC PM 處理
	 */
	private void handlePmMeasurementData(final JsonNode msgNode, final NetworkType networkType) throws Exception
	{
		log.debug("msgNode={}", msgNode.toPrettyString());
		final String measurementName = msgNode.path("content").path("measurementName").asText();
		final Double value = msgNode.path("content").path("value").asDouble();
		String performanceRuleName = null;
		final Map<String, String> networkDetail = new HashMap<>();
		switch (networkType)
		{
			case fgc :
				// 5GC 若包含 UE 與 gNB，則需要排除
				if (msgNode.path("content").path("gnbIp").isMissingNode() == false || msgNode.path("content").path("imsi").isMissingNode() == false) return;

				final String nfInterface = msgNode.path("content").path("interface").asText();
				final String dir = msgNode.path("content").path("dir").asText();
				// 組成複合鍵
				performanceRuleName = measurementName + "-" + nfInterface + "-" + dir;
				log.debug("performanceRuleName=[{}]", performanceRuleName);
				networkDetail.put("nfInterface", nfInterface);
				networkDetail.put("dir", dir);
				break;
			case mec :
				final String nfInterfaceForMec = msgNode.path("content").path("interface").asText();
				final String dirForMec = msgNode.path("content").path("dir").asText();
				// 組成複合鍵
				performanceRuleName = measurementName + "-" + nfInterfaceForMec + "-" + dirForMec;

				final String regionId = msgNode.path("content").path("regionId").asText();
				networkDetail.put("regionId", regionId);
				networkDetail.put("nfInterface", nfInterfaceForMec);
				networkDetail.put("dir", dirForMec);
				break;
			case ric :
				// RIC 若包含 NCI，，則需要排除
				if (msgNode.path("content").path("nci").isMissingNode() == false) return;

				final String fieldId = msgNode.path("content").path("fieldId").asText();
				// 組成複合鍵
				performanceRuleName = fieldId + "-" + measurementName;
				networkDetail.put("fieldId", fieldId);
				break;
			default :
				break;
		}

		// log.debug("\t[Message] performanceRuleName=[{}]", performanceRuleName);
		if (performanceRuleName == null) return;

		// 讀取 rule
		final PerformanceRule rule = this.ruleDao.findTopByNetworkTypeAndName(networkType, performanceRuleName);
		log.debug("\t[Message] rule=[{}]", rule);
		if (rule == null) return;

		// 若沒有超過 PM 量測時間間隔，則返回
		if (checkPmMeasurementPeriod(msgNode, rule) == false)
		{
			if (rule.getTriggerTime() == null && checkPmMeasurementThreshold(msgNode, rule) == true)
			{
				// 設定 t0 時間點
				log.debug("沒有超過 PM 量測時間間隔, 只設定 t0 時間點=[{}]", rule.getTriggerTime());
				rule.setTriggerTime(DateTimeUtil.castMillsToUtcLocalDateTime(msgNode.path("content").path("measurementTime").asLong()));
				this.ruleDmo.modifyOne(rule);
			}
			return;
		}

		// ---[ 過了此處，表示超過 PM 量測時間間隔 ]----------------------------------------------------------------------------------

		// 若沒有超過臨界值，則返回
		if (checkPmMeasurementThreshold(msgNode, rule) == false)
		{
			log.debug("超過 PM 量測時間間隔, 量測值 {}，沒有超過臨界值 {}", value, rule.getThreshold());
			rule.setTriggerTime(null);
			this.ruleDmo.modifyOne(rule);

			final PerformanceAlarm pmAlarm = this.pmAlarmDao.findTopByNetworkTypeAndName(networkType, performanceRuleName);
			if (pmAlarm == null)
			{
				// 更新 t0 時間點
				rule.setTriggerTime(DateTimeUtil.castMillsToUtcLocalDateTime(msgNode.path("content").path("measurementTime").asLong()));
				this.ruleDmo.modifyOne(rule);
				return;
			}
			// log.debug("超過 PM 量測時間間隔, 沒有超過臨界值 {}, {}，刪除 alarm", rule.getThreshold(), value);
			// 觸發 clear 事件，刪除即時，送出日誌，送出 WS 通知
			this.pmAlarmDmo.removeOne(pmAlarm.getId());

			// 更新 t0 時間點
			rule.setTriggerTime(DateTimeUtil.castMillsToUtcLocalDateTime(msgNode.path("content").path("measurementTime").asLong()));
			this.ruleDmo.modifyOne(rule);

			// 發出 AMQP 到 Log Manager
			final PerformanceAlarmBean pmAlarmBean = new PerformanceAlarmBean();
			BeanUtils.copyProperties(pmAlarm, pmAlarmBean);
			pmAlarmBean.setComparison(rule.getComparison());
			pmAlarmBean.setThreshold(rule.getThreshold());
			pmAlarmBean.setValue(value);
			pmAlarmBean.setIsCleared(true);
			final MessageBean messageBean = new MessageBean(networkType, MessageType.PerformanceAlarm);
			messageBean.setContent(this.objectMapper.valueToTree(pmAlarmBean));
			log.debug("messageBean (PM)=\n{}", this.objectMapper.valueToTree(messageBean).toPrettyString());
			this.amqpService.sendMsgToLm(messageBean);
			// 發出 WebSocket 到 Portal
			this.webSocketService.broadcastAll(this.objectMapper.valueToTree(messageBean).toPrettyString());
			return;
		}

		// ---[ 過了此處，表示超過 PM 量測時間間隔，並且超過臨界值 ]--------------------------------------------------------------------------

		// 若之前已經觸發，則不再二次觸發，只更新觸發時間
		final PerformanceAlarm alarm = this.pmAlarmDao.findTopByNetworkTypeAndName(networkType, performanceRuleName);
		if (alarm != null)
		{
			// 更新 t0 時間點
			log.debug("超過 PM 量測時間間隔, 超過臨界值，已觸發，不再二次觸發，只更新觸發時間，設定 t0 時間點=[{}]", rule.getTriggerTime());
			rule.setTriggerTime(DateTimeUtil.castMillsToUtcLocalDateTime(msgNode.path("content").path("measurementTime").asLong()));
			this.ruleDmo.modifyOne(rule);
			return;
		}

		log.debug("超過 PM 量測時間間隔, 超過臨界值，觸發，更新觸發時間，設定 t0 時間點=[{}]", rule.getTriggerTime());
		// ---[ Trigger Alarm ]----------------------------------------------------------------------------------------[S]
		// 寫入資料庫
		final PerformanceAlarm pmAalarmDetach = new PerformanceAlarm();
		pmAalarmDetach.setName(performanceRuleName);
		pmAalarmDetach.setNetworkType(networkType);
		pmAalarmDetach.setSeverity(rule.getSeverity());
		pmAalarmDetach.setTime(DateTimeUtil.castMillsToUtcLocalDateTime(msgNode.path("content").path("measurementTime").asLong()));
		pmAalarmDetach.setDetail(JsonNodeFactory.instance.objectNode());
		pmAalarmDetach.setComparison(rule.getComparison());
		pmAalarmDetach.setThreshold(rule.getThreshold());
		pmAalarmDetach.setValue(value);
		final PerformanceAlarm pmAlarm = this.pmAlarmDmo.createOne(pmAalarmDetach);

		// 更新 t0 時間點
		rule.setTriggerTime(DateTimeUtil.castMillsToUtcLocalDateTime(msgNode.path("content").path("measurementTime").asLong()));
		this.ruleDmo.modifyOne(rule);

		// 發出 AMQP 到 Log Manager
		final PerformanceAlarmBean pmAlarmBean = new PerformanceAlarmBean();
		BeanUtils.copyProperties(pmAlarm, pmAlarmBean);
		// final String description = MessageFormat.format("The {0} value ({1}) of interface {2} {3} is {4} than threshold ({5}).", measurementName, pmAalarmDetach.getValue(), nfInterface, dir,
		// rule.getComparison().name(), rule.getThreshold());
		final String description = getDescription(msgNode, rule, networkDetail);
		pmAlarmBean.setDescription(description);
		final MessageBean messageBean = new MessageBean(networkType, MessageType.PerformanceAlarm);
		messageBean.setContent(this.objectMapper.valueToTree(pmAlarmBean));
		log.debug("\t[Message] messageBean (PM)=\n{}", this.objectMapper.valueToTree(messageBean).toPrettyString());
		this.amqpService.sendMsgToLm(messageBean);
		// 發出 WebSocket 到 Portal
		this.webSocketService.broadcastAll(this.objectMapper.valueToTree(messageBean).toPrettyString());
		// Send mail
		sendMailForPm(rule, pmAalarmDetach, description);
		// ---[ Trigger Alarm ]----------------------------------------------------------------------------------------[E]
	}

	private String getDescription(final JsonNode msgNode, final PerformanceRule rule, final Map<String, String> networkDetail)
	{
		final String measurementName = msgNode.path("content").path("measurementName").asText();
		final Double value = msgNode.path("content").path("value").asDouble();
		final NetworkType networkType = NetworkType.valueOf(msgNode.path("networkType").asText().toLowerCase());

		String description = null;
		switch (networkType)
		{
			case fgc :
			case mec :
				String dirString = "";
				switch (networkDetail.get("dir"))
				{
					case "ul" :
						dirString = "uplink";
						break;
					case "dl" :
						dirString = "downlink";
						break;
				}

				description = MessageFormat.format("The {0} value ({1}) of interface {2} {3} is {4} than threshold ({5}).", measurementName, value, networkDetail.get("nfInterface"), dirString,
						rule.getComparison().name(), rule.getThreshold());
				break;
			case ric :
				description = MessageFormat.format("The {0} value ({1}) of field ({2}) is {3} than threshold ({4}).", measurementName, value, networkDetail.get("fieldId"), rule.getComparison().name(),
						rule.getThreshold());
				break;
		}

		return description;
	}

	/**
	 * 判斷是否超過 PM 量測時間間隔
	 *
	 * @param t0
	 * @param t1
	 * @param period
	 *        單位：秒
	 * @return
	 */
	private Boolean checkPmMeasurementPeriod(final JsonNode msgNode, final PerformanceRule rule)
	{
		Boolean isTrigger = false;
		// 如果 matchTime 是 null，那等同於一個很久以前的時間，也就是說觸發成立
		if (rule.getTriggerTime() == null) return true;

		// t0
		final Long triggerTime = DateTimeUtil.LocalDateTimeToMills(rule.getTriggerTime());
		// t1
		final Long measurementTime = msgNode.path("content").path("measurementTime").asLong();

		if ((measurementTime - triggerTime) > rule.getPeriod() * 1_000L) isTrigger = true;
		log.debug("TriggerTime={}, measurementTime={}, diff={}, period={}, isTrigger={}", triggerTime, measurementTime, (measurementTime - triggerTime) / 1000L, rule.getPeriod(), isTrigger);

		return isTrigger;
	}

	private Boolean checkPmMeasurementThreshold(final JsonNode msgNode, final PerformanceRule rule)
	{
		Boolean isTrigger = false;

		final Double value = msgNode.path("content").path("value").asDouble();

		switch (rule.getComparison())
		{
			case greater :
				isTrigger = (value > rule.getThreshold()) ? true : false;
				break;
			case less :
				isTrigger = (value < rule.getThreshold()) ? true : false;
				break;

			default :
				break;
		}

		// log.debug("Threshold={}, value={}, isTrigger={}", rule.getThreshold(), value, isTrigger);

		return isTrigger;
	}

	/**
	 * 當 file server (SFTP) 重啟時，接收新的 SSH key
	 */
	@RabbitListener(id = "file-to-nms", queues = "file-to-nms", concurrency = "1", autoStartup = "false")
	public void listenForSftp(final Message message, final Channel channel) throws ExceptionBase, InvocationTargetException, JsonParseException, JsonMappingException, IOException
	{
		try
		{
			final JsonNode eventFromFile = castMessageToJsonNode(message);
			log.debug("\t[Message] Event from file (none-durable): \n{}", eventFromFile.toPrettyString());
			final String newKey = eventFromFile.path("key").asText();
			// this.deviceService.handleFileServerKeyChangeEvent(newKey);
		} catch (final ExceptionBase e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally
		{
			try
			{
				channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private JsonNode castMessageToJsonNode(final Message message) throws ExceptionBase
	{
		final String jsonString = (new String(message.getBody(), StandardCharsets.UTF_8)).trim();

		JsonNode jsonNode;
		try
		{
			jsonNode = this.objectMapper.readTree(jsonString);
			// log.debug("\t[AMQP] 收到 AMQP 訊息：" + this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
		} catch (final Exception e)
		{
			log.error("\t [AMQP] AMQP message can NOT cast to JsonNode.");
			throw new ExceptionBase("[AMQP] AMQP message can NOT cast to JsonNode.");
		}

		return jsonNode;
	}

	/**
	 * 將舊格式的 JsonNode 轉成新格式的 json
	 */
	private JsonNode castMessageVersion(final JsonNode oldMsg)
	{
		final String mgtType = oldMsg.path("mgt_type").asText().toLowerCase();
		final Long msgTime = oldMsg.path("event_time").asLong();

		final ObjectNode newMsg = JsonNodeFactory.instance.objectNode();
		newMsg.put("mgtType", mgtType).put("msgTime", msgTime);
		final ObjectNode content = newMsg.putObject("content");

		final String[] eventNameStrings = oldMsg.path("event_name").asText().split("_");
		if (eventNameStrings[eventNameStrings.length - 1].equals("count"))
		{
			final String measurementNameString = oldMsg.path("event_name").asText().substring(4);
			switch (measurementNameString)
			{
				case "associated_upf_count" :
					content.put("measurementName", "assoUpfCount");
					break;
				case "registered_upf_count" :
					content.put("measurementName", "regUpfCount");
					break;
				case "pdu_session_count" :
					content.put("measurementName", "pduSessCount");
					break;
				case "connected_ue_count" :
					content.put("measurementName", "connUeCount");
					break;
				case "attached_ran_count" :
					content.put("measurementName", "attaRanCount");
					break;
				case "registered_ue_count" :
					content.put("measurementName", "regUeCount");
					break;
				default :
					break;
			}
			content.put("measurementTime", msgTime);
			content.put("value", oldMsg.path("count").asDouble());
		} else
		{
			if (mgtType.equals("am")) content.put("imsi", oldMsg.path("imsi").asText());
			content.put("interface", eventNameStrings[2]);
			content.put("dir", eventNameStrings[3]);
			content.put("measurementName", eventNameStrings[1]);
			content.put("measurementTime", msgTime);
			switch (eventNameStrings[1])
			{
				case "throughput" :
					content.put("value", oldMsg.path("throughput").asDouble());
					break;
				case "pps" :
					content.put("value", oldMsg.path("pps").asDouble());
					break;
				default :
					break;
			}
		}

		return newMsg;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class MessageBean
	{
		private NetworkType	networkType;
		private MessageType	messageType;

		private Long		messageTime	= DateTimeUtil.LocalDateTimeToMills(LocalDateTime.now());
		private JsonNode	content;

		public MessageBean(final NetworkType networkType, final MessageType messageType)
		{
			this.networkType = networkType;
			this.messageType = messageType;
		}

		public MessageBean(final MessageType messageType)
		{
			this.messageType = messageType;
		}

		public enum MessageType
		{
			FaultAlarm,
			FaultAlarmRaw,
			PerformanceAlarm,
			PerformanceMeasurementData,
			SecurityApmAlarm,
			SecurityDtmAlarm,
			InvalidJsonFormat;
		}
	}

	public interface MessageContentBaseBean
	{}

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class PerformanceAlarmBean implements MessageContentBaseBean
	{
		private String				name;
		private PerceivedSeverity	severity;
		private Boolean				isCleared	= false;
		private LocalDateTime		time;
		private Comparison			comparison;
		private Double				threshold;
		private Double				value;
		private JsonNode			detail;
		private String				description;
	}

	@Async
	private void sendMailForPm(final PerformanceRule rule, final PerformanceAlarm pmAalarmDetach, final String description) throws JsonParseException, JsonMappingException, IOException
	{
		if (rule == null) return;

		final PerceivedSeverity severity = rule.getSeverity();

		Set<String> mailAddresses = null;

		// Mail 處理邏輯
		// ErrorMessage 優先度大於 Severity。若 ErrorMessage.mailDisabled = false，就會覆蓋 Severity 的設定

		// 取得 severity 層級的 mail address
		final SystemParameter systemParameter = this.systemParameterDao.findTopByName("pmAlarmMailAddressSetting");
		if (systemParameter != null)
		{
			final JsonNode severityNode = systemParameter.getParameter().path(severity.name());
			final Boolean mailDisabled = severityNode.path("mailDisabled").asBoolean();
			if (mailDisabled == false) mailAddresses = this.objectMapper.readValue(severityNode.path("mailAddresses").traverse(), new TypeReference<LinkedHashSet<String>>()
			{});
		}

		// 取得 ErrorMessage 層級的 mail address
		if (rule.getMailDisabled() == false) mailAddresses = new HashSet<>(rule.getMailAddresses());
		if (mailAddresses == null || mailAddresses.size() == 0) return;
		for (final String mailAddress : mailAddresses)
			if (mailAddress == null) return;

		final String emailContent = MessageFormat.format("Network: {0}\nSeverity: {1}\nDescription: {2}\nEvent Time: {3}\nSOP: {4}", rule.getNetworkType().name(), rule.getSeverity().name(),
				description, DateTimeUtil.castLocalDateTimeToString(pmAalarmDetach.getTime()), rule.getSop());
		log.debug("emailContent={}", emailContent);
		this.mailService.sendPm(mailAddresses, emailContent);
	}

	public enum AlarmNotificationType
	{
		notifyNewAlarm,
		notifyChangedAlarm,
		notifyChangedAlarmGeneral,
		notifyAckStateChanged,
		notifyCorrelatedNotificationChanged,
		notifyComments,
		notifyClearedAlarm,
		notifyAlarmListRebuilt,
		notifyPotentialFaultyAlarmList;
	}
}