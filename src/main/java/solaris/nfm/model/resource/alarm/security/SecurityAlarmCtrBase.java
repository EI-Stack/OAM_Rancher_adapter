package solaris.nfm.model.resource.alarm.security;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.EntityIdInvalidException;
import solaris.nfm.exception.EntityIsNullException;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;
import solaris.nfm.model.resource.alarm.fault.comment.Comment;
import solaris.nfm.model.resource.alarm.fault.comment.CommentIsDto;
import solaris.nfm.model.resource.alarm.fault.comment.CommentSsDto;
import solaris.nfm.model.resource.alarm.fault.comment.CommentVo;
import solaris.nfm.model.resource.alarm.fault.comment.MergePatchAlarmDto;
import solaris.nfm.service.AlarmService;
import solaris.nfm.util.DateTimeUtil;
import solaris.nfm.util.ReflectionUtil;

public abstract class SecurityAlarmCtrBase<E extends SecurityAlarmBase, DAO, DMO, VO>
{
	// 此行不能使用 @Slf4j 代替，因為權限是 protected，可以被繼承的類使用
	protected static final Logger	log	= LoggerFactory.getLogger(SecurityAlarmBase.class.getClass());
	Class<E>						entityClass;
	Class<DAO>						daoClass;
	Class<DMO>						dmoClass;
	Class<VO>						voClass;
	String							entityName;
	private Method					methodCheckOneById;
	private Method					methodFindOne;
	private Method					methodModifyOne;
	private Method					methodRemoveOne;

	@Autowired
	protected ObjectMapper			objectMapper;
	@Autowired
	public DAO						dao;
	@Autowired
	public DMO						dmo;
	@Autowired
	private AlarmService			alarmService;

	@SuppressWarnings("unchecked")
	protected SecurityAlarmCtrBase()
	{
		this.entityClass = (Class<E>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.daoClass = (Class<DAO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
		this.dmoClass = (Class<DMO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[2];
		this.voClass = (Class<VO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[3];
		this.entityName = this.entityClass.getSimpleName();
		this.methodCheckOneById = ReflectionUtil.getMethod(this.dmoClass, "checkOneById", Long.class);
		this.methodFindOne = ReflectionUtil.getMethod(this.dmoClass, "findOne", Long.class);
		this.methodModifyOne = ReflectionUtil.getMethod(this.dmoClass, "modifyOne", Object.class);
		this.methodRemoveOne = ReflectionUtil.getMethod(this.dmoClass, "removeOne", Long.class);
	}

	protected VO findAlarm(final Long alarmId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		ReflectionUtil.invokeMethod(this.methodCheckOneById, this.dmo, alarmId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		// 此處 entity 必定不為空值
		final E entity = getAlarmById(alarmId);
		// 將 entity 內容值複製至 dro
		final VO vo = _copyEntityValueToVo(entity);

		return vo;
	}

	// ---[ Comment ]--------------------------------------------------------------------------------------------------[START]
	/**
	 * Fetch all comments from a specific alarm
	 */
	protected JsonNode getCommentsFromAlarm(final Long id) throws ExceptionBase, InvocationTargetException
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		ReflectionUtil.invokeMethod(this.methodCheckOneById, this.dmo, id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		@SuppressWarnings("unchecked")
		final Optional<E> optionalEntity = (Optional<E>) ReflectionUtil.invokeMethod(this.methodFindOne, this.dmo, id);
		if (optionalEntity.isEmpty()) throw new EntityNotFoundException(this.entityName, id);
		// 此處 entity 必定不為空值
		final E alarm = optionalEntity.get();

		final ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		return mapper.valueToTree(alarm.getComments());
	}

	/**
	 * Add a comment to a specific alarm
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	protected JsonNode addCommentToAlarm(final Long alarmId, final CommentSsDto dto) throws InvocationTargetException, ExceptionBase
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		ReflectionUtil.invokeMethod(this.methodCheckOneById, this.dmo, alarmId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Comment comment = _setComment(dto.getCommentText());
		return _addCommentToAlarm(alarmId, comment);
	}

	/**
	 * Add a comment to multiple alarms
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	protected JsonNode addCommentToAlarms(final CommentIsDto dto) throws InvocationTargetException, ExceptionBase
	{
		final Map<Long, String> badAlarmInformationReferenceList = new LinkedHashMap<>();
		final Comment comment = _setComment(dto.getCommentText());
		final Set<Long> alarmIds = dto.getAlarmInformationReferenceList();

		for (final Long alarmId : alarmIds)
		{
			try
			{
				// ---[ 入參驗證 ]-----------------------------------------------------------------------------------------[S]
				ReflectionUtil.invokeMethod(this.methodCheckOneById, this.dmo, alarmId);
				// ---[ 入參驗證 ]-----------------------------------------------------------------------------------------[E]

				_addCommentToAlarm(alarmId, comment);
			} catch (final Exception e)
			{
				badAlarmInformationReferenceList.put(alarmId, "Alarm ID does not exist.");
			}
		}

		final ObjectNode json = JsonNodeFactory.instance.objectNode();
		json.set("badAlarmInformationReferenceList", this.objectMapper.valueToTree(badAlarmInformationReferenceList));
		String status = "";
		if (badAlarmInformationReferenceList.size() == 0)
		{
			status = "OperationSucceeded";
		} else if (badAlarmInformationReferenceList.size() < alarmIds.size())
		{
			status = "OperationPartiallyFailed";
		} else if (badAlarmInformationReferenceList.size() == alarmIds.size())
		{
			status = "OperationFailed";
		}
		json.put("status", status);

		return json;

	}

	private JsonNode _addCommentToAlarm(final Long alarmId, final Comment comment) throws EntityIdInvalidException, EntityNotFoundException, InvocationTargetException, EntityIsNullException
	{
		final E alarm = getAlarmById(alarmId);

		Map<String, Comment> comments = alarm.getComments();
		if (comments == null) comments = new LinkedHashMap<>();
		comments.put(String.valueOf(comments.size() + 1), comment);
		alarm.setComments(comments);

		// Update alarm
		ReflectionUtil.invokeMethod(this.methodModifyOne, this.dmo, alarm);
		// Copy comment fields to commentVo
		final CommentVo vo = new CommentVo();
		BeanUtils.copyProperties(comment, vo);

		return this.objectMapper.valueToTree(vo);
	}

	private Comment _setComment(final String commentText) throws ExceptionBase
	{
		final Comment comment = new Comment();
		comment.setCommentUserId(ControllerUtil.getUserId());
		comment.setCommentUserName(ControllerUtil.getUserName());
		comment.setCommentTime(DateTimeUtil.castZonedDateTimeToZonedIsoString(ZonedDateTime.now()));
		comment.setCommentText(commentText);

		return comment;
	}

	// ---[ Comment ]--------------------------------------------------------------------------------------------------[END]

	/**
	 * Patch a specific alarm by clear, acknowledge or unacknowledge
	 * TS 28 532 R16 11.1 Clause 11.2.1.1.9
	 */
	public void patchAlarm(final MessageType messageType, final Long alarmId, final MergePatchAlarmDto dto) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		ReflectionUtil.invokeMethod(this.methodCheckOneById, this.dmo, alarmId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		if ((dto.getPerceivedSeverity() == null) || (dto.getPerceivedSeverity() != PerceivedSeverity.CLEARED))
		{
			throw new ExceptionBase(400, "Request parameter is wrong.");
		}
		// 手動刪除 alarm
		clearAlarmByUser(messageType, alarmId);
	}

	/**
	 * 手動清除 alarm
	 */
	public void clearAlarmByUser(final MessageType messageType, final Long alarmId) throws Exception
	{
		final E alarmDetached = getAlarmById(alarmId);
		// Remove an alarm
		ReflectionUtil.invokeMethod(this.methodRemoveOne, this.dmo, alarmId);

		alarmDetached.setClearUserId(ControllerUtil.getUserId());
		alarmDetached.setAlarmClearedTime(ZonedDateTime.now());
		alarmDetached.setClearUserName(ControllerUtil.getUserName());

		alarmDetached.setDuplicateCount(0);
		alarmDetached.setDuplicateTime(null);

		alarmDetached.setAlarmChangedTime(ZonedDateTime.now());

		// 使用 AMQP 轉發，需要在 log 紀錄"手動刪除"
		this.alarmService.handleSecurityAlarm(messageType, alarmDetached);
	}

	protected VO _copyEntityValueToVo(final E entity) throws Exception
	{
		return _copyEntityValueToBean(entity, this.entityClass, this.voClass);
	}

	protected static <GenericEntity, GenericBean> GenericBean _copyEntityValueToBean(final GenericEntity entity, final Class<GenericEntity> genericEntityClass,
			final Class<GenericBean> genericBeanClass) throws Exception
	{
		final Constructor<GenericBean> constructor = genericBeanClass.getDeclaredConstructor();
		final GenericBean bean = constructor.newInstance();
		BeanUtils.copyProperties(entity, bean);
		return bean;
	}

	private E getAlarmById(final Long alarmId) throws EntityNotFoundException, InvocationTargetException
	{
		@SuppressWarnings("unchecked")
		final Optional<E> optionalEntity = (Optional<E>) ReflectionUtil.invokeMethod(this.methodFindOne, this.dmo, alarmId);
		if (optionalEntity.isEmpty()) throw new EntityNotFoundException(this.entityName, alarmId);
		// 此處 entity 必定不為空值
		return optionalEntity.get();
	}
}