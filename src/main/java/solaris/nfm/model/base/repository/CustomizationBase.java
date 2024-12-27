package solaris.nfm.model.base.repository;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;

import solaris.nfm.model.base.domain.EntityBase;

public class CustomizationBase<T extends EntityBase, ID extends Serializable>
{
	protected String	entityClassName	= null;
	protected Class<T>	entityClass;

	@SuppressWarnings("unchecked")
	public CustomizationBase()
	{
		this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.entityClassName = this.entityClass.getSimpleName();
	}
}
