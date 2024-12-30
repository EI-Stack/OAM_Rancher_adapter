package solaris.nfm.model.resource.alarm.performance.rule;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import com.querydsl.core.types.dsl.StringPath;

import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.repository.DaoBase;

public interface PerformanceRuleDao extends DaoBase<PerformanceRule, Long>, QuerydslBinderCustomizer<QPerformanceRule>
{
	Long countByNetworkTypeAndName(NetworkType networkType, String name);

	PerformanceRule findTopByNetworkTypeAndName(NetworkType networkType, String name);

	/**
	 * 自定義預設的 Predicate 的處理規則
	 */
	@Override
	default void customize(final QuerydslBindings bindings, final QPerformanceRule qEntity)
	{
		// 自定義綁定關係，使用白名单模式 (true)，只有明確列出的欄位才適用於搜尋
		bindings.excludeUnlistedProperties(true);
		// 設置白名單
		bindings.including(qEntity.name, qEntity.networkType);
		// 一次設定所有字串類型
		bindings.bind(String.class).first((final StringPath path, final String value) -> path.containsIgnoreCase(value));
	}
}