package solaris.nfm.capability.system.grafana;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.extern.slf4j.Slf4j;

import org.owasp.encoder.Encode;

@RestController
@RequestMapping("/v1")
@Slf4j
public class GrafanaCtl
{
	@Autowired
	private SystemAlarmDao	alarmDao;
	@Autowired
	private SystemAlarmDmo	alarmDmo;
	@Autowired
	private ObjectMapper objectMapper;
	// @Autowired
	// private NotificationService notificationService;

	@PostMapping(value = "/webhook/systemAlarm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void applyYangConfig(@RequestBody final JsonNode json) throws Exception
	{
		deal(json);
	}
	
	public void deal(JsonNode json) throws Exception {
		if(json != null) {
			JsonNode WebhookDto = checkJson(json);
			log.debug("WebhookDto={}", WebhookDto.toPrettyString());
			final String faultSource = WebhookDto.path("ruleName").asText();
			final Boolean isCleared = (WebhookDto.path("state").asText().equalsIgnoreCase("ok")) ? true : false;
			final ArrayNode evalMatches = (ArrayNode) WebhookDto.path("evalMatches");
			final String sourceValue = evalMatches.path(0).path("value").asText();
	
			SystemAlarm entity = alarmDao.findTopByFaultSource(faultSource);
			String state = null;
	
			if (entity == null)
			{
				final SystemAlarm alarmBean = new SystemAlarm();
				alarmBean.setFaultId(WebhookDto.path("ruleId").asInt());
				alarmBean.setFaultSource(faultSource);
				// alarmBean.setFaultSeverity(FaultAlarmPhysical.Severity.MAJOR);
				alarmBean.setFaultText(WebhookDto.path("message").asText());
				alarmBean.setEventTime(LocalDateTime.now());
				alarmBean.setFaultCount(1);
				alarmBean.setStartTime(LocalDateTime.now());
				alarmBean.setCleared(isCleared);
				alarmBean.setAcknowledged(false);
	
				// 當只有 isCleared = false 時才需要存入資料庫
				if (isCleared)
				{
					state = "d";
				} else
				{
					entity = alarmDmo.createOne(alarmBean);
					state = "c";
				}
	
				// 使用 AMQP 轉發至 alarm history
				// notificationService.sendSystemAlarmWithAmqp(alarmBean);
				// 使用 WebSocket 轉發
				// notificationService.sendSystemAlarmWithWebSocket(alarmBean, state, sourceValue);
			} else if (isCleared)
			{
				// isCleared
				entity.setCleared(isCleared);
				entity.setEventTime(LocalDateTime.now());
				this.alarmDmo.removeOne(entity.getId());
				state = "d";
			} else
			{
				entity.setFaultCount(entity.getFaultCount() + 1);
				entity.setEventTime(LocalDateTime.now());
				entity.setFaultText(WebhookDto.path("message").asText());
				alarmDmo.modifyOne(entity);
				state = "u";
			}
			// 使用 AMQP 轉發至 alarm history
			// notificationService.sendSystemAlarmWithAmqp(entity);
			// 使用 WebSocket 轉發
			// notificationService.sendSystemAlarmWithWebSocket(entity, state, sourceValue);
		}
	}
	
	public JsonNode checkJson(JsonNode json) throws JsonMappingException, JsonProcessingException {
		String jsonString = json.toPrettyString();
		jsonString = Encode.forJava(jsonString);
		jsonString = Encode.forHtml(jsonString);
		return objectMapper.readTree(jsonString);
	}
}
