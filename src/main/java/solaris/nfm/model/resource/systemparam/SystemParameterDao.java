package solaris.nfm.model.resource.systemparam;

import solaris.nfm.model.base.repository.DaoBase;

public interface SystemParameterDao extends DaoBase<SystemParameter, Long>
{
	Long countByName(String name);

	SystemParameter findTopByName(String name);
}