package solaris.nfm.model.base.manager;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.Predicate;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.DeleteNotAllowedException;
import solaris.nfm.exception.EntityFieldInvalidException;
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.EntityIdInvalidException;
import solaris.nfm.exception.EntityIsNullException;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.exception.ValueNotFoundException;
import solaris.nfm.exception.util.ExceptionUtil;
import solaris.nfm.util.ReflectionUtil;

@Slf4j
public class DmoBase<E, DAO>
{
	private Class<E>					entityClass;
	private Class<DAO>					daoClass;
	private String						entityClassName;
	private Method						methodGetId;
	private Method						methodSave;

	@Autowired
	protected DAO						dao;
	private static final Set<String>	defaultIgnoreFields			= Set.of("id", "entityVersion");
	// 遭遇樂觀鎖問題時，最大嘗試次數
	private static final Integer		JpaOptimisticLockMaxLoopNo	= 10;
	// 遭遇樂觀鎖問題時，嘗試間隔 (微秒)
	private static final Integer		JpaOptimisticLockTPeriod	= 250;

	@SuppressWarnings("unchecked")
	public DmoBase()
	{
		this.entityClass = (Class<E>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.daoClass = (Class<DAO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
		this.entityClassName = this.entityClass.getSimpleName();
		this.methodGetId = ReflectionUtil.getMethod(this.entityClass, "getId");
		this.methodSave = ReflectionUtil.getMethod(this.daoClass, "save", Object.class);
	}

	// ===[ Check ID and Entity ]======================================================================================[S]
	/**
	 * 檢查 entity ID 不可是空值或者是小於 1
	 *
	 * @param id
	 * @throws EntityIdInvalidException
	 *         id 是空值，或者是小於 1
	 */
	public void checkEntityId(final Long id) throws EntityIdInvalidException
	{
		if (id == null || id.longValue() < 1)
		{
			final EntityIdInvalidException e = new EntityIdInvalidException(this.entityClassName, id);
			log.error(e.getErrorMessage());
			throw e;
		}
	}

	/**
	 * 檢查 entity 不可是空值
	 *
	 * @param entity
	 * @throws EntityIsNullException
	 *         id 是空值，或者是小於 1
	 */
	public void checkEntityIsNull(final E entity) throws EntityIsNullException
	{
		if (entity == null)
		{
			final EntityIsNullException e = new EntityIsNullException(this.entityClassName);
			log.error(e.getErrorMessage());
			throw e;
		}
	}

	/**
	 * 依據 entity ID 檢查 entity 是否存在於資料庫
	 *
	 * @param id
	 *        entity ID 不可是空值或者是小於 1
	 * @throws EntityIdInvalidException
	 *         id 是空值，或者是小於 1
	 * @throws EntityNotFoundException
	 *         Entity 不存在
	 */
	public void checkOneById(final Long id) throws EntityIdInvalidException, EntityNotFoundException
	{
		// existsById 只會噴一個例外 IllegalArgumentException，意思是 id 是空值
		// 在這裡攔住的話，照理論來說，IllegalArgumentException 不會發生
		checkEntityId(id);

		try
		{
			final Method existsByIdMethod = this.daoClass.getMethod("existsById", Object.class);
			final Boolean result = (Boolean) existsByIdMethod.invoke(this.dao, id);
			if (result == false)
			{
				final EntityNotFoundException e = new EntityNotFoundException(this.entityClassName, id);
				// log.error(e.getErrorMessage());
				throw e;
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException e)
		{
			log.error("\t [DMO] [Reflection] There're some problems in reflection process, check whether JDK or Spring JPA Data version is changed.");
			e.printStackTrace();
		} catch (final InvocationTargetException e2)
		{
			// existsById 只會噴一個例外 IllegalArgumentException ，但是包裹在 InvocationTargetException
			// 使用 InvocationTargetException.getCluse() 取出 IllegalArgumentException
			// 但在此處應該不會發生 IllegalArgumentException
			log.error("\t [DMO] [Reflection] 這個例外不該發生的，因為此處 id 不可能是 null");
			e2.printStackTrace();
		}
	}
	// ===[ Check ID and Entity ]======================================================================================[E]

	// ===[ Count Entity ]=============================================================================================[S]
	/**
	 * 計算 entity 的所有數量
	 *
	 * @return
	 */
	public Long count()
	{
		try
		{
			final Method methodCount = this.daoClass.getMethod("count");
			return (Long) methodCount.invoke(this.dao);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t [DMO] [Reflection] There're some problem in reflection process, check whether JDK or Spring JPA Data version is changed.");
			e.printStackTrace();
			return null;
		}
	}

	public Long countByIsSystemReserved(final Boolean isSystemReserved)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ValueNotFoundException
	{
		final Method countByIsSystemReserved = this.daoClass.getMethod("countByIsSystemReserved", Boolean.class);
		final Long count = (Long) countByIsSystemReserved.invoke(this.dao, isSystemReserved);
		if (count == null)
		{
			throw new ValueNotFoundException("Can not find the count value !!");
		}
		return count;
	}
	// ===[ Count Entity ]=============================================================================================[E]

	// ===[ Find Entity ]==============================================================================================[S]
	/**
	 * 依據 entity ID 取得 entity。
	 *
	 * @param id
	 *        entity ID 不可是空值或者是小於 1
	 */
	@SuppressWarnings("unchecked")
	public E getOne(final Long id) throws EntityIdInvalidException, EntityNotFoundException
	{
		// findOne 只會噴一個例外 IllegalArgumentException，意思是 id 是空值
		// 在這裡攔住的話，照理論來說，IllegalArgumentException 不會發生
		checkEntityId(id);

		E entity = null;
		try
		{
			final Method findByIdMethod = this.daoClass.getMethod("findById", Object.class);
			final Optional<E> optional = (Optional<E>) findByIdMethod.invoke(this.dao, id);
			entity = optional.orElse(null);
			if (entity == null)
			{
				throw new EntityNotFoundException("There is no entity " + this.entityClassName + " (id=" + id + ") found.");
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e)
		{
			log.error("\t [DMO] [Reflection] There're some problems in reflection process, check whether JDK or Spring JPA Data version is changed.");
			e.printStackTrace();
		} catch (final InvocationTargetException e2)
		{
			// findById 只會噴一個例外 IllegalArgumentException ，但是包裹在 InvocationTargetException
			// 使用 InvocationTargetException.getCluse() 取出 IllegalArgumentException
			// 但在此處應該不會發生 InvocationTargetException
			log.error("\t [DMO] [Reflection] 這個例外不該發生的，檢查最近是否更動了 JPA library");
			e2.printStackTrace();
		}

		return entity;
	}

	/**
	 * 依據 entity ID 取得 entity。
	 * 此版本與 getOne() 的差別在於不會噴任何例外。使用前須先確認 id 的正確性，基本上，是邏輯內部使用，方便之處在於不須處理例外
	 *
	 * @param id
	 *        entity ID 不可是空值或者是小於 1
	 */
	@SuppressWarnings("unchecked")
	public Optional<E> findOne(final Long id)
	{

		if (id == null || id.longValue() < 1)
		{
			log.error("\t [DMO] 檢查 id ({})，id  不可是空值，或是小於 1");
			return Optional.empty();
		}

		try
		{
			final Method findByIdMethod = this.daoClass.getMethod("findById", Object.class);
			final Optional<E> optionalEntity = (Optional<E>) findByIdMethod.invoke(this.dao, id);
			return optionalEntity;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e)
		{
			log.error("\t[DMO] [Reflection] There're some problems in reflection process, check whether JDK or Spring JPA Data version is changed.");
			e.printStackTrace();
		} catch (final InvocationTargetException e2)
		{
			// findById 只會噴一個例外 IllegalArgumentException ，但是包裹在 InvocationTargetException
			// 使用 InvocationTargetException.getCluse() 取出 IllegalArgumentException
			// 但在此處應該不會發生 InvocationTargetException
			log.error("\t[DMO] [Reflection] 這個例外不該發生的，檢查最近是否更動了 JPA library");
			e2.printStackTrace();
		}

		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll()
	{
		Method methodFindAll = null;
		try
		{
			methodFindAll = this.daoClass.getMethod("findAll");
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		List<E> list = null;
		try
		{
			list = (List<E>) methodFindAll.invoke(this.dao);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public Page<E> findAll(final Pageable pageable)
	{
		Method methodFindAll = null;
		try
		{
			methodFindAll = this.daoClass.getMethod("findAll", Pageable.class);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		Page<E> page = null;
		try
		{
			page = (Page<E>) methodFindAll.invoke(this.dao, pageable);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return page;
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(final Specification<E> spec)
	{
		Method methodFindAll = null;
		try
		{
			methodFindAll = this.daoClass.getMethod("findAll", Specification.class);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		List<E> list = null;
		try
		{
			list = (List<E>) methodFindAll.invoke(this.dao, spec);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public Page<E> findAll(final Specification<E> spec, final Pageable pageable)
	{
		Method methodFindAll = null;
		try
		{
			methodFindAll = this.daoClass.getMethod("findAll", Specification.class, Pageable.class);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		Page<E> page = null;
		try
		{
			page = (Page<E>) methodFindAll.invoke(this.dao, spec, pageable);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return page;
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(final Predicate predicate)
	{
		Method methodFindAll = null;
		try
		{
			methodFindAll = this.daoClass.getMethod("findAll", Predicate.class);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		List<E> list = null;
		try
		{
			list = (List<E>) methodFindAll.invoke(this.dao, predicate);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public Page<E> findAll(final Predicate predicate, final Pageable pageable)
	{
		Method methodFindAll = null;
		try
		{
			methodFindAll = this.daoClass.getMethod("findAll", Predicate.class, Pageable.class);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		Page<E> page = null;
		try
		{
			page = (Page<E>) methodFindAll.invoke(this.dao, predicate, pageable);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return page;
	}
	// ===[ Find Entity ]==============================================================================================[E]

	// ===[ Create Entity ]============================================================================================[S]
	public E createOne(final E bean) throws EntityHasExistedException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			EntityNotFoundException, EntityFieldInvalidException, EntityIsNullException, EntityIdInvalidException, DeleteNotAllowedException
	{
		checkBeanBeforeSave(bean);
		return saveOne(bean);
	}

	public List<E> createAll(final Iterable<E> entityCollection) throws Exception
	{
		return saveAll(entityCollection);
	}

	public void checkBeanBeforeSave(final E bean) throws EntityHasExistedException, EntityIsNullException, EntityFieldInvalidException
	{
		if (bean == null) throw new EntityIsNullException(getEntityClassName());
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public E saveOne(final E bean) throws EntityIsNullException
	{
		if (bean == null) throw new EntityIsNullException(getEntityClassName());

		Method methodSave = null;
		try
		{
			methodSave = this.daoClass.getMethod("save", Object.class);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法取得 method，檢查最近是否更動了 JPA library");
			e.printStackTrace();
		}
		E entity = null;
		try
		{
			entity = (E) methodSave.invoke(this.dao, bean);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ method 執行失敗，檢查 method 的定義是否改變，入參資料類別是否正確，入參是否為空值");
			e.printStackTrace();
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<E> saveAll(final Iterable<E> entityCollection) throws Exception
	{
		final Method save = this.daoClass.getMethod("saveAll", Iterable.class);
		return (List<E>) save.invoke(this.dao, entityCollection);
	}
	// ===[ Create Entity ]============================================================================================[E]

	// ===[ Modify Entity ]============================================================================================[S]
	public E modifyOne_original(final E bean) throws InvocationTargetException, EntityIsNullException, EntityIdInvalidException, EntityNotFoundException
	{
		// 注意囉，這裡的入參是 bean 喔，不是 entity，所以沒必要事先取出 entity，減少資料庫的壓力
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkEntityIsNull(bean);
		final Long id = (Long) ReflectionUtil.invokeMethod(this.methodGetId, bean);
		checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return updateOne(bean);
	}

	public E modifyOne(final E bean) throws InvocationTargetException, EntityIsNullException, EntityIdInvalidException, EntityNotFoundException
	{
		// 注意囉，這裡的入參是 bean 喔，不是 entity，所以沒必要事先取出 entity，減少資料庫的壓力
		// 為了優化效能，關閉入參檢查。那就意味著 Controller 那邊需要確實做好入參檢查
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkEntityIsNull(bean);
		// 執行 bean.getId()
		final Long id = (Long) ReflectionUtil.invokeMethod(this.methodGetId, bean);
		// checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		E updatedEntity = null;
		// 是否結束嘗試更新
		boolean isLoopContinue = true;
		// 嘗試更新的計數器
		int retryCounter = 0;
		// 取得不須複製的欄位名稱數組
		final String[] ignoreFieldNames = getIgnoreFieldNames(bean);

		while (isLoopContinue)
		{
			try
			{
				updatedEntity = updateOne(bean, ignoreFieldNames, id);
				isLoopContinue = false;
			} catch (IllegalArgumentException | InvocationTargetException ite)
			{
				ite.printStackTrace();
				if (retryCounter > DmoBase.JpaOptimisticLockMaxLoopNo)
				{
					isLoopContinue = false;
					log.error("\t[樂觀鎖] 已經嘗試執行 {} 次，仍然更新失敗，只得放棄 !! Entity={}, id={}", retryCounter, this.entityClassName, id);
				} else if (ite.getCause() instanceof org.springframework.orm.jpa.JpaOptimisticLockingFailureException)
				{
					log.error("\t[樂觀鎖] [執行次數 {}] 發生樂觀鎖例外 (OptimisticLockException) Entity={}, id={}", retryCounter, this.entityClassName, id);
					retryCounter++;

					try
					{
						TimeUnit.MILLISECONDS.sleep(DmoBase.JpaOptimisticLockTPeriod);
					} catch (final InterruptedException e)
					{}
				} else
				{
					isLoopContinue = false;
					log.error("\t[樂觀鎖] [執行次數 {}] 本例外並非由樂觀鎖引發，更新失敗 !! Entity={}, id={}, error message={}", retryCounter, this.entityClassName, id, ExceptionUtil.getExceptionRootCauseMessage(ite));
				}
			} catch (final Exception e)
			{
				isLoopContinue = false;
				e.printStackTrace();
				log.error("\t[樂觀鎖] [執行次數 {}] 本例外並非由反射機制或是樂觀鎖引起，更新失敗 !! Entity={}, id={}, eror message={}", retryCounter, this.entityClassName, id, ExceptionUtil.getExceptionRootCauseMessage(e));
			}
		}
		return updatedEntity;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public E updateOne(final E bean) throws InvocationTargetException, EntityIdInvalidException, EntityNotFoundException
	{
		// 資料驗證全部交給 modifyOne() 來執行
		// 為了避免樂觀鎖之類的問題，應該在此重新取出 entity 的值，然後將 bean 的值塞入，進行儲存

		final String[] ignoreFieldNames = getIgnoreFieldNames(bean);
		// 取得 entity ID，bean.getId()
		final Long id = (Long) ReflectionUtil.invokeMethod(this.methodGetId, bean);
		// 藉由 id 重新讀取 entity
		final E entityReadyForUpdate = getOne(id);
		// 將 bean 複製至 entity
		BeanUtils.copyProperties(bean, entityReadyForUpdate, ignoreFieldNames);
		// 執行更新並回傳更新後的 entity
		return (E) ReflectionUtil.invokeMethod(this.methodSave, this.dao, entityReadyForUpdate);
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public E updateOne(final E bean, final String[] ignoreFieldNames, final Long id) throws EntityIdInvalidException, EntityNotFoundException, InvocationTargetException
	{
		// 藉由 id 重新讀取 entity
		final E entityReadyForUpdate = getOne(id);
		// 將 bean 複製至 entity
		BeanUtils.copyProperties(bean, entityReadyForUpdate, ignoreFieldNames);
		// 執行更新並回傳更新後的 entity
		return (E) ReflectionUtil.invokeMethod(this.methodSave, this.dao, entityReadyForUpdate);
	}

	// ===[ Modify Entity ]============================================================================================[E]

	// ===[ Remove Entity ]============================================================================================[S]
	public void removeOne(final Long id) throws Exception
	{
		checkEntityBeforeDelete(id);
		// 應該在 deleteOne 加入刪除 多對多 或 一對多 的程序, 而非 removeOne, 因為 @Transactional 是加在 deleteOne
		deleteOne(id);
	}

	public void checkEntityBeforeDelete(final Long id) throws Exception
	{
		checkOneById(id);
	}

	// deleteOne 沒有檢查 id 與 entity 是否存在。所以，不要直接使用, 而應該使用 removeOne
	@Transactional
	public void deleteOne(final Long id) throws EntityIdInvalidException
	{
		checkEntityId(id);
		try
		{
			final Method methodDeleteById = this.daoClass.getMethod("deleteById", Object.class);
			methodDeleteById.invoke(this.dao, id);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e)
		{
			// deleteById() 只會噴 1 個例外 IllegalArgumentException，發生原因是入參 id 是 null
			log.error("/t [DMO] [Reflection] There're some problem in reflection process, check whether JDK or Spring JPA Data version is changed.");
			e.printStackTrace();
		}
	}
	// ===[ Remove Entity ]============================================================================================[E]

	public void showDaoReflectClass()
	{
		for (final Method method : daoClass.getMethods())
		{
			log.warn("\t [Reflect] method name=[{}]", method.getName());
			for (final Class<?> clazz : method.getParameterTypes())
			{
				log.warn("\t [Reflect] Parameter class=[{}]", clazz.getSimpleName());
			}
		}
	}

	public String getEntityClassName()
	{
		return this.entityClassName;
	}

	private static String[] getUpdatingPropertyNames(final Object source)
	{
		final Set<String> ignoreNames = Set.of("id", "entityVersion");
		final BeanWrapper src = new BeanWrapperImpl(source);
		final java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
		final Set<String> updatingPropertyNames = new HashSet<>();
		for (final java.beans.PropertyDescriptor pd : pds)
		{
			log.debug("name=" + pd.getName());
			if (ignoreNames.contains(pd.getName()))
			{
				updatingPropertyNames.add(pd.getName());
			}
		}
		log.debug("set=" + updatingPropertyNames);
		return updatingPropertyNames.toArray(new String[updatingPropertyNames.size()]);
	}

	/**
	 * 當執行 update entity 時，需要將 bean 取代原本的 entity，但空值與一些固定值是不用複製。
	 * 本方法目的為取得不須複製的欄位名稱數組
	 *
	 * @param source
	 * @return
	 */
	private static String[] getIgnoreFieldNames(final Object source)
	{
		final Set<String> ignoreFields = new HashSet<>(defaultIgnoreFields);
		final BeanWrapper beanWrapper = new BeanWrapperImpl(source);
		final PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();
		for (final PropertyDescriptor pd : pds)
		{
			if (beanWrapper.getPropertyValue(pd.getName()) == null) ignoreFields.add(pd.getName());
		}

		return ignoreFields.toArray(new String[ignoreFields.size()]);
	}
}
