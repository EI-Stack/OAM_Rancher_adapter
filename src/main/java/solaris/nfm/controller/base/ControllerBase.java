package solaris.nfm.controller.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;

import solaris.nfm.controller.dto.CountResultDto;
import solaris.nfm.controller.dto.MethodResultDto;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.ResourceListDto;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.model.base.domain.EntityBase;
import solaris.nfm.util.ReflectionUtil;

public abstract class ControllerBase<E extends EntityBase, BEAN, DCO, DUO, DMO>
{
	// 此行不能使用 @Slf4j 代替，因為權限是 protected，可以被繼承的類使用
	protected static final Logger	logger	= LoggerFactory.getLogger(ControllerBase.class.getClass());
	Class<E>						entityClass;
	Class<BEAN>						beanClass;
	Class<DCO>						dcoClass;
	Class<DUO>						duoClass;
	Class<DMO>						dmoClass;
	String							entityName;
	private Method					methodCheckEntityId;
	private Method					methodCheckOneById;
	private Method					methodFindOne;
	private Method					methodFindAll;
	private Method					methodCreateOne;
	private Method					methodRemoveOne;
	private Method					methodModifyOne;
	private Method					methodSetId;

	@Autowired
	public DMO						dmo;
	@PersistenceContext
	protected EntityManager			entityManager;
	@Autowired
	protected HttpServletRequest	httpServletRequest;
	@Autowired
	protected ObjectMapper			objectMapper;
	protected JPAQueryFactory		queryFactory;

	@PostConstruct
	public void init()
	{
		this.queryFactory = new JPAQueryFactory(this.entityManager);
	}

	@SuppressWarnings("unchecked")
	public ControllerBase()
	{
		this.entityClass = (Class<E>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.beanClass = (Class<BEAN>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
		this.dcoClass = (Class<DCO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[2];
		this.duoClass = (Class<DUO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[3];
		this.dmoClass = (Class<DMO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[4];
		this.entityName = this.entityClass.getSimpleName();
		this.methodCheckEntityId = ReflectionUtil.getMethod(this.dmoClass, "checkEntityId", Long.class);
		this.methodCheckOneById = ReflectionUtil.getMethod(this.dmoClass, "checkOneById", Long.class);
		this.methodFindOne = ReflectionUtil.getMethod(this.dmoClass, "findOne", Long.class);
		this.methodFindAll = ReflectionUtil.getMethod(this.dmoClass, "findAll", Pageable.class);
		this.methodCreateOne = ReflectionUtil.getMethod(this.dmoClass, "createOne", Object.class);
		this.methodModifyOne = ReflectionUtil.getMethod(this.dmoClass, "modifyOne", Object.class);
		this.methodRemoveOne = ReflectionUtil.getMethod(this.dmoClass, "removeOne", Long.class);
		this.methodSetId = ReflectionUtil.getMethod(this.entityClass, "setId", Long.class);
	}

	protected BEAN copyEntityValueToBean(final E entity) throws Exception
	{
		// Constructor<BEAN> constructor = this.beanClass.getDeclaredConstructor();
		// BEAN bean = constructor.newInstance();
		// BeanUtils.copyProperties(entity, bean);
		// return bean;
		return _copyEntityValueToBean(entity, this.entityClass, this.beanClass);
	}

	protected static <GenericEntity, GenericBEAN> GenericBEAN _copyEntityValueToBean(final GenericEntity entity, final Class<GenericEntity> genericEntityClass,
			final Class<GenericBEAN> genericBeanClass) throws Exception
	{
		final Constructor<GenericBEAN> constructor = genericBeanClass.getDeclaredConstructor();
		final GenericBEAN bean = constructor.newInstance();
		BeanUtils.copyProperties(entity, bean);
		return bean;
	}

	protected void addEntityRelation(final E entity, final DCO bean) throws SecurityException, IllegalArgumentException, Exception
	{}

	protected void removeEntityRelation(final E entity) throws Exception
	{}

	protected void resetEntityRelation(final E entity, final DUO bean) throws Exception
	{}

	protected void modifyBeanList(final List<BEAN> beanList)
	{}

	protected void modifyBean(final BEAN bean)
	{}

	protected void saveEntityPost(final E entity, final DCO bean) throws Exception
	{}

	protected void updateEntityPost(final E entity, final DUO bean) throws Exception
	{}

	protected E fillEntityValueForCreation(final DCO bean) throws Exception
	{
		final Constructor<E> constructor = this.entityClass.getDeclaredConstructor();
		final E entity = constructor.newInstance();
		BeanUtils.copyProperties(bean, entity);
		return entity;
	}

	protected void fillEntityValueForUpdate(final E entity, final DUO bean) throws Exception
	{
		BeanUtils.copyProperties(bean, entity, getNullPropertyNames(bean));
	}

	protected void checkEntityValueForSave(final DCO bean) throws SecurityException, IllegalArgumentException, Exception
	{}

	protected void checkEntityValueForUpdate(final DUO bean) throws Exception
	{}

	@SuppressWarnings("unchecked")
	protected BEAN findEntity(final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查此處 id 必定不為空值或是小於 1
		ReflectionUtil.invokeMethod(this.methodCheckEntityId, this.dmo, id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Optional<E> optionalEntity = (Optional<E>) ReflectionUtil.invokeMethod(this.methodFindOne, this.dmo, id);
		if (optionalEntity.isEmpty()) throw new EntityNotFoundException(this.entityName, id);

		// 此處 entity 必定不為空值
		final E entity = optionalEntity.get();
		// 將 entity 內容值複製至 dro
		final BEAN dro = copyEntityValueToBean(entity);
		// 對 dro 進行修改欄位值，準備回傳 dro
		modifyBean(dro);

		return dro;
	}

	@SuppressWarnings("unchecked")
	protected static <GENTITY, GBEAN, GSERVICE> List<Object> findAllEntity(final GSERVICE genericDaoService, final Method findMethod, final Method countMethod, final PageRequest pageRequest,
			final Class<GENTITY> genericEntityClass, final Class<GBEAN> genericBeanClass) throws Exception
	{
		final List<Object> objectList = new ArrayList<>();
		final ResourceListDto<Object> resourceListBean = new ResourceListDto<>();
		List<GENTITY> entityList = null;
		final List<GBEAN> beanList = new ArrayList<>();
		Long entityAmount;
		entityList = ((Page<GENTITY>) findMethod.invoke(genericDaoService, pageRequest)).getContent();
		entityAmount = (Long) countMethod.invoke(genericDaoService);
		for (final GENTITY entity : entityList)
		{
			final GBEAN bean = _copyEntityValueToBean(entity, genericEntityClass, genericBeanClass);
			beanList.add(bean);
		}
		objectList.addAll(beanList);
		// [Create page meta bean]
		final PaginationDto pageMetaBean = new PaginationDto(pageRequest, entityAmount);
		objectList.add(pageMetaBean);
		resourceListBean.setPageInfo(pageMetaBean);
		return objectList;
	}

	@SuppressWarnings("unchecked")
	protected RestResultDto<BEAN> findAllEntity(final Pageable pageable) throws Exception
	{
		final Page<E> pageEntities = (Page<E>) ReflectionUtil.invokeMethod(this.methodFindAll, this.dmo, pageable);
		final List<BEAN> droList = new ArrayList<>(Math.toIntExact(pageEntities.getTotalElements()));

		for (final E entity : pageEntities)
		{
			final BEAN dro = copyEntityValueToBean(entity);
			droList.add(dro);
		}
		// 對 DRO list 進行修改欄位值
		modifyBeanList(droList);
		// 建立並回傳分頁資料集合
		return new RestResultDto<>(droList, new PaginationDto(pageEntities));
	}

	/**
	 * QueryDSL 專用
	 *
	 * @param entityList
	 * @param count
	 * @param pageable
	 * @return
	 * @throws Exception
	 */
	protected RestResultDto<BEAN> findAllEntity(final List<E> entityList, final Long count, final Pageable pageable) throws Exception
	{
		final List<BEAN> droList = new ArrayList<>(entityList.size());
		for (final E entity : entityList)
		{
			final BEAN bean = copyEntityValueToBean(entity);
			droList.add(bean);
		}
		// 對 DRO list 進行修改欄位值
		modifyBeanList(droList);
		// 建立並回傳分頁資料集合
		return new RestResultDto<>(droList, new PaginationDto(pageable, count));
	}

	@SuppressWarnings("unchecked")
	protected RestResultDto<BEAN> findAllEntity(final Predicate predicate, final Pageable pageable) throws Exception
	{
		final Method findAllMethod = this.dmoClass.getMethod("findAll", Predicate.class, Pageable.class);
		final Page<E> entityPage = (Page<E>) findAllMethod.invoke(this.dmo, predicate, pageable);
		final List<BEAN> droList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
		for (final E entity : entityPage)
		{
			final BEAN bean = copyEntityValueToBean(entity);
			droList.add(bean);
		}
		// 對 DRO list 進行修改欄位值
		modifyBeanList(droList);
		// 建立並回傳分頁資料集合
		return new RestResultDto<>(droList, new PaginationDto(entityPage));
	}

	public JsonNode saveEntity(final DCO creationBean) throws Exception
	{
		checkEntityValueForSave(creationBean);
		final E bean = fillEntityValueForCreation(creationBean);
		final E entity = saveEntityInTransaction(creationBean, bean, this.methodCreateOne);
		saveEntityPost(entity, creationBean);
		return this.objectMapper.readTree("{\"id\":\"" + entity.getId() + "\"}");
	}

	@Transactional
	private E saveEntityInTransaction(final DCO creationBean, final E bean, final Method saveEntityMethod) throws Exception
	{
		@SuppressWarnings("unchecked")
		final E entity = (E) saveEntityMethod.invoke(this.dmo, bean);
		addEntityRelation(entity, creationBean);
		return entity;
	}

	public void updateEntity(final Long id, final DUO duo) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		ReflectionUtil.invokeMethod(this.methodCheckOneById, this.dmo, id);
		checkEntityValueForUpdate(duo);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		// 取得 entity 預設無參數建構式
		final Constructor<E> entityConstructor = this.entityClass.getConstructor();
		// 執行預設無參數建構式，等同於 new Entity()
		final E beanReadyForUpdate = entityConstructor.newInstance();
		// 執行 setId(id)
		ReflectionUtil.invokeMethod(this.methodSetId, beanReadyForUpdate, id);
		// 把 duo 複製到 bean
		BeanUtils.copyProperties(duo, beanReadyForUpdate, getNullPropertyNames(duo));

		// ReflectionUtil.invokeMethod(this.methodSetId, beanReadyForUpdate, id);
		// ReflectionUtil.invokeMethod(this.methodModifyOne, this.dmo, beanReadyForUpdate);

		updateEntityInTransaction(duo, beanReadyForUpdate, this.methodModifyOne);
		updateEntityPost(beanReadyForUpdate, duo);
	}

	@Transactional(readOnly = false)
	private void updateEntityInTransaction(final DUO bean, final E entity, final Method modifyOneMethod) throws Exception
	{
		modifyOneMethod.invoke(this.dmo, entity);
		resetEntityRelation(entity, bean);
	}

	@Transactional
	protected void deleteEntity(final Long id) throws Exception
	{
		// removeOne() 自帶入參驗證 checkOneById()
		ReflectionUtil.invokeMethod(this.methodRemoveOne, this.dmo, id);
	}

	protected ResponseEntity<?> countEntity() throws Exception
	{
		final CountResultDto countResultBean = new CountResultDto();
		Method countMethod;
		Long entityAmount;
		Method getIsSystemReservedMethod;
		try
		{
			getIsSystemReservedMethod = this.entityClass.getMethod("getIsSystemReserved");
		} catch (final Exception e)
		{
			getIsSystemReservedMethod = null;
		}
		if (getIsSystemReservedMethod == null)
		{
			countMethod = this.dmoClass.getMethod("count");
			entityAmount = (Long) countMethod.invoke(this.dmo);
		} else
		{
			countMethod = this.dmoClass.getMethod("countByIsSystemReserved", Boolean.class);
			entityAmount = (Long) countMethod.invoke(this.dmo, false);
		}
		countResultBean.setAmount(entityAmount);
		// [Send a HTTP response]
		return sendResponse(countResultBean, getResponseHeadersForMethodGet(), HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	protected static ResponseEntity<Object> sendResponse(final Object objectList, final HttpHeaders httpHeaders, final HttpStatus httpStatus)
	{
		return (ResponseEntity<Object>) sendResponse(httpHeaders, httpStatus, objectList);
	}

	@SuppressWarnings("unchecked")
	protected static ResponseEntity<List<Object>> sendResponse(final List<Object> objectList, final HttpHeaders httpHeaders, final HttpStatus httpStatus)
	{
		return (ResponseEntity<List<Object>>) sendResponse(httpHeaders, httpStatus, objectList);
	}

	@SuppressWarnings("unchecked")
	protected static ResponseEntity<Map<?, ?>> sendMapToResponse(final Map<?, ?> objectList, final HttpHeaders httpHeaders, final HttpStatus httpStatus)
	{
		return (ResponseEntity<Map<?, ?>>) sendResponse(httpHeaders, httpStatus, objectList);
	}

	@SuppressWarnings("unchecked")
	protected ResponseEntity<List<Object>> sendResponse(final ResourceListDto<BEAN> objectList, final HttpHeaders httpHeaders, final HttpStatus httpStatus)
	{
		return (ResponseEntity<List<Object>>) sendResponse(httpHeaders, httpStatus, objectList);
	}

	@SuppressWarnings("unchecked")
	protected static ResponseEntity<MethodResultDto> sendResponse(final MethodResultDto methodResultBean, final HttpHeaders httpHeaders, final HttpStatus httpStatus)
	{
		return (ResponseEntity<MethodResultDto>) sendResponse(httpHeaders, httpStatus, methodResultBean);
	}

	private static ResponseEntity<?> sendResponse(final HttpHeaders httpHeaders, final HttpStatus httpStatus, final Object object)
	{
		ResponseEntity<?> responseEntity = null;
		try
		{
			responseEntity = new ResponseEntity<>(object, httpHeaders, httpStatus);
		} catch (final Exception e)
		{
			e.printStackTrace();
			responseEntity = new ResponseEntity<>(httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return responseEntity;
	}

	protected HttpHeaders getResponseHeadersForMethodGet()
	{
		final String origin = this.httpServletRequest.getHeader("Origin");
		final HttpHeaders responseHeaders = new HttpHeaders();
		// responseHeaders.set("Access-Control-Allow-Credentials", "true");
		// if (StringUtils.hasText(origin))
		// {
		// responseHeaders.add("Access-Control-Allow-Origin", origin);
		// }
		responseHeaders.setCacheControl("no-store");
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-HTTP-Method-Override");
		return responseHeaders;
	}

	public static String[] getNullPropertyNames(final Object source)
	{
		final BeanWrapper src = new BeanWrapperImpl(source);
		final java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
		final Set<String> emptyNames = new HashSet<>();
		for (final java.beans.PropertyDescriptor pd : pds)
		{
			final Object srcValue = src.getPropertyValue(pd.getName());
			if (srcValue == null)
			{
				emptyNames.add(pd.getName());
			}
		}
		final String[] result = new String[emptyNames.size()];
		return emptyNames.toArray(result);
	}

	protected String getHumanReadableInstanceName(final E entity)
	{
		String instanceName = "";
		Method getNameMethod;
		final Long entityId = getEntityId(entity);
		try
		{
			getNameMethod = this.entityClass.getMethod("getName");
			instanceName = (String) getNameMethod.invoke(entity);
		} catch (final Exception e)
		{}
		String humanReadableInstanceName = null;
		final String humanReadableEntityName = entity.getHumanReadableEntityName();
		if (instanceName == null)
		{
			humanReadableInstanceName = humanReadableEntityName + "(ID: " + entityId + ")";
		} else
		{
			humanReadableInstanceName = humanReadableEntityName + "(" + instanceName + ", ID: " + entityId + ")";
		}
		return humanReadableInstanceName;
	}

	protected String getHumanReadableInstanceNameWithoutId(final E bean)
	{
		String instanceName = "";
		Method getNameMethod;
		try
		{
			getNameMethod = this.entityClass.getMethod("getName");
			instanceName = (String) getNameMethod.invoke(bean);
		} catch (final Exception e)
		{}
		String humanReadableInstanceName = null;
		final String humanReadableEntityName = bean.getHumanReadableEntityName();
		if (instanceName == null)
		{
			humanReadableInstanceName = humanReadableEntityName;
		} else
		{
			humanReadableInstanceName = humanReadableEntityName + "(" + instanceName + ")";
		}
		return humanReadableInstanceName;
	}

	protected Long getEntityId(final E entity)
	{
		Long id = null;
		Method getId;
		try
		{
			getId = this.entityClass.getMethod("getId");
			id = (Long) getId.invoke(entity);
		} catch (final Exception e)
		{}
		return id;
	}
}
