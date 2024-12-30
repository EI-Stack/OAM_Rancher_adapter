package solaris.nfm.model.resource.mecapppackage;

import solaris.nfm.model.base.repository.DaoBase;

public interface MecAppPackageDao extends DaoBase<MecAppPackage, Long>
{
	Long countByName(String name);
}
