package solaris.nfm.model.resource.alarm.fault.ric;

import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.querydsl.core.types.Predicate;

import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.alarm.AlarmCtrBase;
import solaris.nfm.model.resource.alarm.fault.comment.CommentIsDto;
import solaris.nfm.model.resource.alarm.fault.comment.CommentSsDto;
import solaris.nfm.model.resource.alarm.fault.comment.MergePatchAlarmDto;
import solaris.nfm.model.resource.alarm.mapping.FaultErrorMessage;
import solaris.nfm.model.resource.alarm.mapping.FaultErrorMessageDao;
import solaris.nfm.service.RicService;

/**
 * @author Holisun Wu
 */
@RestController
@RequestMapping("/v1/fm/ric/alarms")
@Validated
public class RicFaultAlarmCtr extends AlarmCtrBase<RicFaultAlarm, RicFaultAlarmDao, RicFaultAlarmDmo, RicFaultAlarmVo>
{
	@Autowired
	private RicFaultAlarmDao		dao;
	@Autowired
	private RicService				ricService;
	@Autowired
	private FaultErrorMessageDao	faultErrorMessageDao;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public RicFaultAlarmVo fetchAlarm(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final RicFaultAlarm entity = this.dao.getReferenceById(id);
		final RicFaultAlarmVo vo = new RicFaultAlarmVo();
		BeanUtils.copyProperties(entity, vo);

		try
		{
			final FaultErrorMessage fem = this.faultErrorMessageDao.findTopByNetworkTypeAndCode(vo.getNetworkType(), Integer.parseInt(vo.getErrorCode()));
			if (fem != null)
			{
				vo.setErrorMessage(fem.getMessage());
				vo.setSop(fem.getSop());
			}
		} catch (final NumberFormatException e)
		{}

		return vo;
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<RicFaultAlarmVo> fetchAllAlarms(@QuerydslPredicate(root = RicFaultAlarm.class) Predicate predicate,
			@PageableDefault(sort = {"duplicateTime"}, direction = Sort.Direction.DESC) final Pageable pageable,
			@RequestParam(name = "startFilterTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final ZonedDateTime startFilterTime,
			@RequestParam(name = "endFilterTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final ZonedDateTime endFilterTime) throws Exception
	{
		if (startFilterTime != null && endFilterTime != null)
		{
			predicate = QRicFaultAlarm.ricFaultAlarm.alarmRaisedTime.between(startFilterTime, endFilterTime).and(predicate);
		}

//		final Map<String, String> fieldMapping = new HashMap<>();
//		final ArrayNode fields = (ArrayNode) this.ricService.doMethodGet("/fields");
//		for (final JsonNode field : fields)
//		{
//			fieldMapping.put(field.path("fieldId").asText(), field.path("fieldName").asText());
//		}

		final Page<RicFaultAlarm> entityPage = this.dao.findAll(predicate, pageable);
		final List<RicFaultAlarmVo> voList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
		for (final RicFaultAlarm entity : entityPage)
		{
			final RicFaultAlarmVo vo = new RicFaultAlarmVo();
			BeanUtils.copyProperties(entity, vo);
//			vo.setFieldName(fieldMapping.get(entity.getFieldId()));

			try
			{
				final FaultErrorMessage fem = this.faultErrorMessageDao.findTopByNetworkTypeAndCode(vo.getNetworkType(), Integer.parseInt(vo.getErrorCode()));
				if (fem != null)
				{
					vo.setErrorMessage(fem.getMessage());
					vo.setSop(fem.getSop());
				}
			} catch (final NumberFormatException e)
			{}

			voList.add(vo);
		}

		// 建立並回傳分頁資料集合
		return new RestResultDto<>(voList, new PaginationDto(entityPage));
	}

	// ---[ Comment ]--------------------------------------------------------------------------------------------------[START]
	/**
	 * Fetch all comments from a specific alarm
	 */
	@GetMapping(value = "/{id}/comments")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchComments(@PathVariable("id") final Long id) throws ExceptionBase, InvocationTargetException
	{
		return getCommentsFromAlarm(id);
	}

	/**
	 * Add a comment to a specific alarm
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	@PostMapping(value = "/{id}/comments")
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
	@PostMapping(value = "/comments")
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
	@PatchMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void patchSingleAlarm(@PathVariable("id") final Long id, @RequestBody final MergePatchAlarmDto dto) throws Exception
	{
		patchAlarm(MessageType.FaultAlarm, id, dto);
	}
}
