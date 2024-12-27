package solaris.nfm.model.base.relation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import solaris.nfm.model.base.domain.EntityBase;

public class EntityRelationManagerBase2<ME extends EntityBase, SE extends EntityBase, M extends EntityBase, DAO>
{
	protected static final Logger	logger	= LoggerFactory.getLogger(EntityRelationManagerBase2.class);
	private final Class<ME>			masterClass;
	private final Class<SE>			slaveClass;
	private final Class<M>			mappingClass;
	private final Class<DAO>		mappingDaoClass;
	private final Method			getMasterIdMethod;
	@Autowired
	protected DAO					dao;

	@SuppressWarnings("unchecked")
	public EntityRelationManagerBase2() throws NoSuchMethodException, SecurityException
	{
		this.masterClass = (Class<ME>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.slaveClass = (Class<SE>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
		this.mappingClass = (Class<M>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[2];
		this.mappingDaoClass = (Class<DAO>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[3];
		this.getMasterIdMethod = this.masterClass.getMethod("getId");
	}

	@SuppressWarnings("unchecked")
	public void reset(final ME masterEntity, final Collection<Long> slaveIdSet)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException
	{
		logger.debug("slaveIdSet.size()=" + slaveIdSet.size());
		for (Long slaveId : slaveIdSet)
		{
			logger.debug("slaveId=" + slaveId);
		}
		Long masterId = (Long) this.getMasterIdMethod.invoke(masterEntity);
		// [ Method for get slave list ]
		final Method getSlaveListMethod = this.mappingDaoClass.getDeclaredMethod("findBy" + this.masterClass.getSimpleName() + "Id", Long.class);
		// [Slave Method]
		Method getSlaveIdMethod = this.mappingClass.getMethod("get" + this.slaveClass.getSimpleName() + "Id");
		// [Create an existed slave id collection]
		final Set<Long> existedSlaveIdSet = new HashSet<>();
		for (M mappingEntity : (Collection<M>) getSlaveListMethod.invoke(this.dao, masterId))
		{
			existedSlaveIdSet.add((Long) getSlaveIdMethod.invoke(mappingEntity));
		}
		logger.debug("existedSlaveIdSet.size()=" + existedSlaveIdSet.size());
		for (Long slaveId : existedSlaveIdSet)
		{
			logger.debug("slaveId=" + slaveId);
		}
		// [ Remove Set ]
		final Set<Long> preparedRemoveSet = new HashSet<>();
		preparedRemoveSet.addAll(existedSlaveIdSet);
		preparedRemoveSet.removeAll(slaveIdSet);
		logger.debug("preparedRemoveSet.size()=" + preparedRemoveSet.size());
		for (Long slaveId : preparedRemoveSet)
		{
			logger.debug("slaveId=" + slaveId);
		}
		remove(masterEntity, preparedRemoveSet);
		// [ Add Set ]
		final Set<Long> preparedAddSet = new HashSet<>();
		preparedAddSet.addAll(slaveIdSet);
		preparedAddSet.removeAll(existedSlaveIdSet);
		logger.debug("preparedAddSet.size()=" + preparedAddSet.size());
		for (Long slaveId : preparedAddSet)
		{
			logger.debug("slaveId=" + slaveId);
		}
		add(masterEntity, preparedAddSet);
	}

	public void add(final ME masterEntity, final Collection<Long> slaveIdSet)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException
	{
		logger.debug("add() slaveIdSet.size()=" + slaveIdSet.size());
		for (Long deviceId : slaveIdSet)
		{
			logger.debug("deviceId=" + deviceId);
		}
		if (slaveIdSet.size() == 0) return;
		// [Mapping Method]
		final Method setMasterId = this.mappingClass.getDeclaredMethod("set" + this.masterClass.getSimpleName() + "Id", Long.class);
		final Method setSlaveId = this.mappingClass.getDeclaredMethod("set" + this.slaveClass.getSimpleName() + "Id", Long.class);
		final Constructor<M> constructor = this.mappingClass.getDeclaredConstructor();
		// [DaoService Method]
		final Method countMapping = this.mappingDaoClass.getDeclaredMethod("countBy" + this.masterClass.getSimpleName() + "IdAnd" + this.slaveClass.getSimpleName() + "Id", Long.class, Long.class);
		final Method saveMapping = this.mappingDaoClass.getMethod("save", Object.class);
		final Long masterId = (Long) this.getMasterIdMethod.invoke(masterEntity);
		// [ Add Set ]
		for (Long slaveId : slaveIdSet)
		{
			if (((Long) countMapping.invoke(this.dao, masterId, slaveId)) == 0)
			{
				M mapping = constructor.newInstance();
				setMasterId.invoke(mapping, masterId);
				setSlaveId.invoke(mapping, slaveId);
				saveMapping.invoke(this.dao, mapping);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void remove(final ME masterEntity, final Collection<Long> slaveIdSet)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		logger.debug("remove() slaveIdSet.size()=" + slaveIdSet.size());
		for (Long slaveId : slaveIdSet)
		{
			logger.debug("slaveId=" + slaveId);
		}
		if (slaveIdSet.size() == 0) return;
		final Long masterId = (Long) this.getMasterIdMethod.invoke(masterEntity);
		// [ MappingDaoService Method]
		final Method findMappingByMasterIdAndSlaveIdInMethod = this.mappingDaoClass.getDeclaredMethod("findBy" + this.masterClass.getSimpleName() + "IdAnd" + this.slaveClass.getSimpleName() + "IdIn",
				Long.class, Collection.class);
		final Method findMappingByMasterIdAndSlaveIdMethod = this.mappingDaoClass.getDeclaredMethod("findBy" + this.masterClass.getSimpleName() + "IdAnd" + this.slaveClass.getSimpleName() + "Id",
				Long.class, Long.class);
		final Method deleteMethod = this.mappingDaoClass.getMethod("delete", Iterable.class);
		// [ Remove Set ]
		logger.debug("findMappingByMasterIdAndSlaveIdInMethod method=[ " + findMappingByMasterIdAndSlaveIdInMethod.getName() + " ]");
		logger.debug("findMappingByMasterIdAndSlaveIdMethod method=[ " + findMappingByMasterIdAndSlaveIdMethod.getName() + " ]");
		logger.debug("masterId=[ " + masterId + " ]");
		// List<M> mappingList = (List<M>) findMappingByMasterIdAndSlaveIdInMethod.invoke(this.dao, masterId, slaveIdSet);
		List<M> mappingList = new ArrayList<>();
		for (Long slaveId : slaveIdSet)
		{
			M mapping = (M) findMappingByMasterIdAndSlaveIdMethod.invoke(this.dao, masterId, slaveId);
			if (mapping != null)
			{
				mappingList.add(mapping);
			}
		}
		logger.debug("mappingList size=" + mappingList.size());
		for (M mapping : mappingList)
		{
			logger.debug("mapping.getId()=" + mapping.getId());
		}
		deleteMethod.invoke(this.dao, mappingList);
	}

	@SuppressWarnings("unchecked")
	public void removeAll(final ME masterEntity) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		final Long masterId = (Long) this.getMasterIdMethod.invoke(masterEntity);
		// [ MappingDaoService Method]
		final Method findByMasterIdMethod = this.mappingDaoClass.getDeclaredMethod("findBy" + this.masterClass.getSimpleName() + "Id", Long.class);
		final Method deleteMethod = this.mappingDaoClass.getMethod("deleteAll", Iterable.class);
		// [ Remove Set ]
		List<M> mappingList = (List<M>) findByMasterIdMethod.invoke(this.dao, masterId);
		deleteMethod.invoke(this.dao, mappingList);
	}
}
