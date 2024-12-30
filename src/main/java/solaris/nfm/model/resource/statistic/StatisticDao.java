package solaris.nfm.model.resource.statistic;

import java.time.LocalDate;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import solaris.nfm.model.base.repository.DaoBase;

public interface StatisticDao extends DaoBase<Statistic, Long>, QuerydslBinderCustomizer<QStatistic>
{
	Long countByDate(LocalDate date);

	Long deleteByDate(LocalDate date);

	Statistic findTopByDate(LocalDate date);

	/**
	 * 自定義預設的 Predicate 的處理規則
	 */
	@Override
	default void customize(final QuerydslBindings bindings, final QStatistic qEntity)
	{}
}