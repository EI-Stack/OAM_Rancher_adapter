package solaris.nfm.model.resource.alarm.mapping;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import com.querydsl.core.types.dsl.StringPath;

import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.repository.DaoBase;

public interface FaultErrorMessageDao extends DaoBase<FaultErrorMessage, Long>, QuerydslBinderCustomizer<QFaultErrorMessage>
{
	Long countByNetworkTypeAndCode(NetworkType networkType, Integer errorCode);

	FaultErrorMessage findTopByNetworkTypeAndCode(NetworkType networkType, Integer errorCode);

	/**
	 * 自定義預設的 Predicate 的處理規則
	 */
	@Override
	default void customize(final QuerydslBindings bindings, final QFaultErrorMessage qEntity)
	{
		// 自定義綁定關係，使用白名单模式 (true)，只有明確列出的欄位才適用於搜尋
		bindings.excludeUnlistedProperties(true);
		// 設置白名單
		bindings.including(qEntity.networkType, qEntity.code, qEntity.message);
		// 一次設定所有字串類型
		bindings.bind(String.class).first((final StringPath path, final String value) -> path.containsIgnoreCase(value));
	}
}