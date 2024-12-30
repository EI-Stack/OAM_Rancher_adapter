package solaris.nfm.capability.system.grafana;

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.controller.base.ControllerBase;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.base.ExceptionBase;

/**
 * @author Holisun Wu
 */
@RestController
@RequestMapping("/v1/systemAlarms")
@Validated
@Slf4j
public class SystemAlarmCtl extends ControllerBase<SystemAlarm, SystemAlarm, SystemAlarm, SystemAlarm, SystemAlarmDmo>
{
	@Autowired
	private SystemAlarmDao alarmDao;

	// @Autowired
	// private NotificationService notificationService;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public SystemAlarm fetchAlarm(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return alarmDao.getById(id);
	}

	// @GetMapping(value = "")
	// @ResponseStatus(HttpStatus.OK)
	// public RestResultDto<SystemAlarm> fetchAllAlarms(@QuerydslPredicate(root = AlarmView.class) final Predicate predicate, final Pageable pageable) throws Exception
	// {
	// final Page<SystemAlarm> entityPage = this.alarmDao.findAll(predicate, pageable);
	//
	// // 建立並回傳分頁資料集合
	// return new RestResultDto<>(entityPage.getContent(), new PaginationDto(entityPage));
	// }

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeAlarm(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final SystemAlarm alarmBean = this.dmo.getOne(id);
		deleteEntity(id);

		// 使用 AMQP 轉發，需要在 log 紀錄手動刪除
		// this.notificationService.sendClearedSystemAlarmWithAmqp(alarmBean, ControllerUtil.getUserId(), ControllerUtil.getUserName());
		// 使用 WebSocket 轉發
		// this.notificationService.sendSystemAlarmWithWebSocket(alarmBean, "d", "");
	}

	/**
	 * 將 alarm 標注為已處理
	 */
	@PostMapping(value = "/{id}/operations/acknowledge")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acknowledgeAlarm(@PathVariable("id") final Long id, @RequestBody final JsonNode requestJson) throws ExceptionBase, InvocationTargetException
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final SystemAlarm alarmEntity = this.dmo.getOne(id);
		alarmEntity.setAcknowledged(true);
		alarmEntity.setAcknowledgeComment(requestJson.path("comment").asText());
		alarmEntity.setAcknowledgeUserId(ControllerUtil.getUserId());
		alarmEntity.setAcknowledgeUserName(ControllerUtil.getUserName());
		this.dmo.modifyOne(alarmEntity);
	}

	/**
	 * 清除 alarm 已處理標注
	 */
	@PostMapping(value = "/{id}/operations/clearAcknowledge")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clearAlarmAcknowledge(@PathVariable("id") final Long id) throws ExceptionBase, InvocationTargetException
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final SystemAlarm alarmEntity = this.dmo.getOne(id);
		alarmEntity.setAcknowledged(false);
		alarmEntity.setAcknowledgeComment(null);
		alarmEntity.setAcknowledgeUserId(null);
		alarmEntity.setAcknowledgeUserName(null);
		this.dmo.modifyOne(alarmEntity);
	}
}
