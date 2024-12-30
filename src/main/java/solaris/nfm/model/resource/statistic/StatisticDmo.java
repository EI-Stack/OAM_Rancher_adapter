package solaris.nfm.model.resource.statistic;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solaris.nfm.model.base.manager.DmoBase;

@Service
public class StatisticDmo extends DmoBase<Statistic, StatisticDao>
{
	@Transactional
	public Long removeAllByDate(final LocalDate date)
	{
		return this.dao.deleteByDate(date);
	}
}
