package solaris.nfm.model.resource.alarm.performance.alarm;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.manager.DmoBase;

@Service
public class PerformanceAlarmDmo extends DmoBase<PerformanceAlarm, PerformanceAlarmDao>
{
	@Transactional
	public void deleteOneByNetworkTypeAndName(final NetworkType networkType, final String name)
	{
		this.dao.deleteByNetworkTypeAndName(networkType, name);
	}
}
