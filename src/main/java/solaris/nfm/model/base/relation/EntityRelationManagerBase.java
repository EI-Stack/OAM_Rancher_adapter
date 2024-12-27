package solaris.nfm.model.base.relation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.model.base.domain.EntityBase;

@Slf4j
public class EntityRelationManagerBase<ME extends EntityBase, SE extends EntityBase, S>
{
	private final Class<ME>	masterClass;
	private final Class<SE>	slaveClass;
	private final Class<S>	daoClass;
	@Autowired
	protected S				dao;

	@SuppressWarnings("unchecked")
	public EntityRelationManagerBase()
	{
		this.masterClass = (Class<ME>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.slaveClass = (Class<SE>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
		this.daoClass = (Class<S>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[2];
	}

	@SuppressWarnings("unchecked")
	public void reset(final ME masterEntity, final Collection<Long> slaveIdSet)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		// [Master Method]
		Method getSlaveCollection = this.masterClass.getDeclaredMethod("get" + this.slaveClass.getSimpleName() + "Collection");
		// [Slave Method]
		Method getSlaveId = this.slaveClass.getMethod("getId");
		// [DaoService Method]
		Method findSlave = this.daoClass.getMethod("getOne", Object.class);
		// [Create an existed slave id collection]
		final Set<Long> existedSlaveIdSet = new HashSet<>();
		for (SE tmpSlaveEntity : (Collection<SE>) getSlaveCollection.invoke(masterEntity))
		{
			existedSlaveIdSet.add((Long) getSlaveId.invoke(tmpSlaveEntity));
		}
		// [ Remove Set ]
		final Set<Long> preparedRemoveSet = new HashSet<>();
		preparedRemoveSet.addAll(existedSlaveIdSet);
		preparedRemoveSet.removeAll(slaveIdSet);
		for (Long id : preparedRemoveSet)
		{
			SE slaveEntity = (SE) findSlave.invoke(this.dao, id);
			removeRelation(masterEntity, slaveEntity);
		}
		// [ Add Set ]
		final Set<Long> preparedAddSet = new HashSet<>();
		preparedAddSet.addAll(slaveIdSet);
		preparedAddSet.removeAll(existedSlaveIdSet);
		for (Long id : preparedAddSet)
		{
			SE slaveEntity = (SE) findSlave.invoke(this.dao, id);
			createRelation(masterEntity, slaveEntity);
		}
	}

	@SuppressWarnings("unchecked")
	public void add(final ME masterEntity, final Collection<Long> slaveIdSet)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, EntityNotFoundException
	{
		if (slaveIdSet == null || slaveIdSet.size() == 0) return;
		// [Master Method]
		Method getSlaveCollection = this.masterClass.getDeclaredMethod("get" + this.slaveClass.getSimpleName() + "Collection");
		// [Slave Method]
		Method getSlaveId = this.slaveClass.getMethod("getId");
		// [DaoService Method]
		Method findSlave = this.daoClass.getMethod("getOne", Object.class);
		// [Create an existed slave id collection]
		final Set<Long> existedSlaveIdSet = new HashSet<>();
		for (SE tmpSlaveEntity : (Collection<SE>) getSlaveCollection.invoke(masterEntity))
		{
			existedSlaveIdSet.add((Long) getSlaveId.invoke(tmpSlaveEntity));
		}
		// [ Add Set ]
		final Set<Long> preparedAddSet = new HashSet<>();
		preparedAddSet.addAll(slaveIdSet);
		preparedAddSet.removeAll(existedSlaveIdSet);
		for (Long id : preparedAddSet)
		{
			SE slaveEntity = (SE) findSlave.invoke(this.dao, id);
			if (slaveEntity == null)
			{
				throw new EntityNotFoundException("There is no Entity " + this.slaveClass.getSimpleName() + " (id=" + id + ") found!!");
			}
			createRelation(masterEntity, slaveEntity);
		}
	}

	@SuppressWarnings("unchecked")
	public void remove(final ME masterEntity, final Collection<Long> slaveIdSet)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if (slaveIdSet == null || slaveIdSet.size() == 0) return;
		// [Master Method]
		Method getSlaveCollection = this.masterClass.getDeclaredMethod("get" + this.slaveClass.getSimpleName() + "Collection");
		// [Slave Method]
		Method getSlaveId = this.slaveClass.getMethod("getId");
		// [DaoService Method]
		Method findSlave = this.daoClass.getMethod("getOne", Object.class);
		// [Create an existed slave id collection]
		final Set<Long> existedSlaveIdSet = new HashSet<>();
		for (SE tmpSlaveEntity : (Collection<SE>) getSlaveCollection.invoke(masterEntity))
		{
			existedSlaveIdSet.add((Long) getSlaveId.invoke(tmpSlaveEntity));
		}
		// [ Remove Set ]
		final Set<Long> preparedRemoveSet = new HashSet<>();
		preparedRemoveSet.addAll(slaveIdSet);
		preparedRemoveSet.retainAll(existedSlaveIdSet);
		for (Long id : preparedRemoveSet)
		{
			SE slaveEntity = (SE) findSlave.invoke(this.dao, id);
			removeRelation(masterEntity, slaveEntity);
		}
	}

	@SuppressWarnings("unchecked")
	public void removeAll(final ME masterEntity) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		// [Master Method]
		Method getSlaveCollection = this.masterClass.getDeclaredMethod("get" + this.slaveClass.getSimpleName() + "Collection");
		// [Slave Method]
		Method getSlaveId = this.slaveClass.getMethod("getId");
		// [DaoService Method]
		Method findSlave = this.daoClass.getMethod("getOne", Object.class);
		// [Create an existed slave id collection]
		final Set<Long> existedSlaveIdSet = new HashSet<>();
		for (SE tmpSlaveEntity : (Collection<SE>) getSlaveCollection.invoke(masterEntity))
		{
			existedSlaveIdSet.add((Long) getSlaveId.invoke(tmpSlaveEntity));
		}
		// [ Remove Set ]
		final Set<Long> preparedRemoveSet = new HashSet<>();
		preparedRemoveSet.addAll(existedSlaveIdSet);
		for (Long id : preparedRemoveSet)
		{
			SE slaveEntity = (SE) findSlave.invoke(this.dao, id);
			removeRelation(masterEntity, slaveEntity);
		}
	}

	@SuppressWarnings("unchecked")
	protected void removeRelation(final ME masterEntity, final SE slaveEntity)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		// [Master Method]
		Method getSlaveCollection = this.masterClass.getDeclaredMethod("get" + this.slaveClass.getSimpleName() + "Collection");
		// [Slave Method]
		Method getMasterCollection = this.slaveClass.getDeclaredMethod("get" + this.masterClass.getSimpleName() + "Collection");
		if (((Collection<ME>) getMasterCollection.invoke(slaveEntity)).contains(masterEntity) == true)
		{
			((Collection<ME>) getMasterCollection.invoke(slaveEntity)).remove(masterEntity);
		}
		if (((Collection<SE>) getSlaveCollection.invoke(masterEntity)).contains(slaveEntity) == true)
		{
			((Collection<SE>) getSlaveCollection.invoke(masterEntity)).remove(slaveEntity);
		}
	}

	@SuppressWarnings("unchecked")
	protected void createRelation(final ME masterEntity, final SE slaveEntity)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		// [Master Method]
		Method getSlaveCollection = this.masterClass.getDeclaredMethod("get" + this.slaveClass.getSimpleName() + "Collection");
		// [Slave Method]
		Method getMasterCollection = this.slaveClass.getDeclaredMethod("get" + this.masterClass.getSimpleName() + "Collection");
		if (((Collection<ME>) getMasterCollection.invoke(slaveEntity)).contains(masterEntity) == false)
		{
			((Collection<ME>) getMasterCollection.invoke(slaveEntity)).add(masterEntity);
		}
		if (((Collection<SE>) getSlaveCollection.invoke(masterEntity)).contains(slaveEntity) == false)
		{
			((Collection<SE>) getSlaveCollection.invoke(masterEntity)).add(slaveEntity);
		}
	}
}
