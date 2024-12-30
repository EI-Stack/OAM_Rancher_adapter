package solaris.nfm.model.resource.alarm.performance.alarm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.types.Predicate;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.capability.message.amqp.AmqpService;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.capability.message.websocket.WebSocketService;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.RestResultDto;

/**
 * @author Holisun Wu
 */
@RestController
@RequestMapping("/v1/performanceAlarms")
@Validated
@Slf4j
public class PerformanceAlarmCtr
{
	@Autowired
	private ObjectMapper		objectMapper;
	@Autowired
	private AmqpService			amqpService;
	@Autowired
	private WebSocketService	webSocketService;
	@Autowired
	private PerformanceAlarmDao	dao;
	@Autowired
	private PerformanceAlarmDmo	dmo;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public PerformanceAlarmVo fetchPerformanceAlarm(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final PerformanceAlarm entity = this.dao.getById(id);
		final PerformanceAlarmVo dro = new PerformanceAlarmVo();
		BeanUtils.copyProperties(entity, dro);
		return dro;
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<PerformanceAlarmVo> fetchAllAlarms(@QuerydslPredicate(root = PerformanceAlarm.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		final Page<PerformanceAlarm> entityPage = this.dao.findAll(predicate, pageable);
		final List<PerformanceAlarmVo> droList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
		for (final PerformanceAlarm entity : entityPage)
		{
			final PerformanceAlarmVo dro = new PerformanceAlarmVo();
			BeanUtils.copyProperties(entity, dro);
			droList.add(dro);
		}

		// 建立並回傳分頁資料集合
		return new RestResultDto<>(droList, new PaginationDto(entityPage));
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeAlarm(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final PerformanceAlarm pmAlarm = this.dmo.getOne(id);
		this.dmo.removeOne(id);

		// 使用 AMQP 轉發，需要在 log 紀錄"手動刪除"
		final ObjectNode contentNode = (ObjectNode) this.objectMapper.valueToTree(pmAlarm);
		contentNode.put("isManualCleared", true);
		log.debug("contentNode (PM)=\n{}", this.objectMapper.valueToTree(contentNode).toPrettyString());
		final MessageBean messageBean = new MessageBean(pmAlarm.getNetworkType(), MessageType.PerformanceAlarm);
		messageBean.setContent(contentNode);
		log.debug("messageBean (PM)=\n{}", this.objectMapper.valueToTree(messageBean).toPrettyString());
		this.amqpService.sendMsgToLm(messageBean);
		this.webSocketService.broadcastAll(this.objectMapper.valueToTree(messageBean).toPrettyString());
	}
}
