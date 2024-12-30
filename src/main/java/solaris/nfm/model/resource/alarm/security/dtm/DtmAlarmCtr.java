package solaris.nfm.model.resource.alarm.security.dtm;

import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.querydsl.core.types.Predicate;

import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.alarm.fault.comment.CommentIsDto;
import solaris.nfm.model.resource.alarm.fault.comment.CommentSsDto;
import solaris.nfm.model.resource.alarm.fault.comment.MergePatchAlarmDto;
import solaris.nfm.model.resource.alarm.security.SecurityAlarmCtrBase;
import solaris.nfm.service.SecurityAdapterService;

/**
 * @author Holisun Wu
 */
@RestController
@RequestMapping("/v1/sm/dtm")
@Validated
public class DtmAlarmCtr extends SecurityAlarmCtrBase<DtmAlarm, DtmAlarmDao, DtmAlarmDmo, DtmAlarmVo>
{
	@Autowired
	private DtmAlarmDao				dao;
	@Autowired
	private SecurityAdapterService	securityAdapterService;

	@GetMapping(value = "/alarms/{id}")
	@ResponseStatus(HttpStatus.OK)
	public DtmAlarmVo fetchFgcAlarm(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findAlarm(id);
	}

	@GetMapping(value = "/alarms")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<DtmAlarmVo> fetchAllFgcAlarms(@QuerydslPredicate(root = DtmAlarm.class) Predicate predicate,
			@PageableDefault(sort = {"duplicateTime"}, direction = Sort.Direction.DESC) final Pageable pageable,
			@RequestParam(name = "startFilterTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final ZonedDateTime startFilterTime,
			@RequestParam(name = "endFilterTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final ZonedDateTime endFilterTime) throws Exception
	{
		if (startFilterTime != null && endFilterTime != null)
		{
			predicate = QDtmAlarm.dtmAlarm.alarmRaisedTime.between(startFilterTime, endFilterTime).and(predicate);
		}

		final Page<DtmAlarm> entityPage = this.dao.findAll(predicate, pageable);
		final List<DtmAlarmVo> droList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
		for (final DtmAlarm entity : entityPage)
		{
			final DtmAlarmVo vo = new DtmAlarmVo();
			BeanUtils.copyProperties(entity, vo);
			droList.add(vo);
		}

		// 建立並回傳分頁資料集合
		return new RestResultDto<>(droList, new PaginationDto(entityPage));
	}

	// ---[ Comment ]--------------------------------------------------------------------------------------------------[START]

	/**
	 * Fetch all comments from a specific alarm
	 */
	@GetMapping(value = "/alarms/{id}/comments")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchCommentsFromAlarm(@PathVariable("id") final Long id) throws ExceptionBase, InvocationTargetException
	{
		return getCommentsFromAlarm(id);
	}

	/**
	 * Add a comment to a specific alarm
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	@PostMapping(value = "/alarms/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode setCommentForSingleAlarm(@PathVariable("id") final Long id, @RequestBody final CommentSsDto dto) throws ExceptionBase, InvocationTargetException
	{
		return addCommentToAlarm(id, dto);
	}

	/**
	 * Add a comment to multiple alarms
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	@PostMapping(value = "/alarms/comments")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode setCommentForAlarms(@RequestBody final CommentIsDto dto) throws ExceptionBase, InvocationTargetException
	{
		return addCommentToAlarms(dto);
	}
	// ---[ Comment ]--------------------------------------------------------------------------------------------------[END]

	/**
	 * Patch a specific alarm by clear, acknowledge or unacknowledge
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	@PatchMapping(value = "/alarms/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void patchSingleAlarm(@PathVariable("id") final Long id, @RequestBody final MergePatchAlarmDto dto) throws Exception
	{
		patchAlarm(MessageType.SecurityDtmAlarm, id, dto);
	}

	// ---[ 資安緩解措施 ]---------------------------------------------------------------------------------------------------[START]

	/**
	 * 資安緩解措施 - 屏蔽掉 UE
	 */
	@PostMapping(value = "/operations/banUe")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void banUe(@RequestBody final JsonNode json) throws Exception
	{
		// final String supi = json.path("supi").asText();

		this.securityAdapterService.doMethodPost("/v1/sm/dtm/operations/banUe", json);

		// 調用者 (portal) 隨後會繼續調用 API (clear)，刪除相關的複數 alarm
	}

	/**
	 * 資安緩解措施 - Uplink 限速
	 */
	@PostMapping(value = "/operations/limitUplink")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void limitUplink(@RequestBody final JsonNode json) throws Exception
	{
		// final String supi = json.path("supi").asText();
		// final Integer linkSpeed = json.path("limitSpeed").asInt();

		this.securityAdapterService.doMethodPost("/v1/sm/dtm/operations/limitUplink", json);

		// 調用者 (portal) 隨後會繼續調用 API (clear)，刪除相關的複數 alarm
	}

	/**
	 * 資安緩解措施 - Downlink 限速
	 */
	@PostMapping(value = "/operations/limitDownlink")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void limitDownlink(@RequestBody final JsonNode json) throws Exception
	{
		// final String supi = json.path("supi").asText();
		// final Integer linkSpeed = json.path("limitSpeed").asInt();

		this.securityAdapterService.doMethodPost("/v1/sm/dtm/operations/limitDownlink", json);

		// 調用者 (portal) 隨後會繼續調用 API (clear)，刪除相關的複數 alarm
	}
	// ---[ 資安緩解措施 ]---------------------------------------------------------------------------------------------------[END]
}