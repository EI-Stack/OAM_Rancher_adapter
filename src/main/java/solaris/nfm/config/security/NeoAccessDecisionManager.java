package solaris.nfm.config.security;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.CollectionUtils;

/**
 * 决策管理器，用于判断用户需要访问的资源与用户所拥有的角色是否匹配
 *
 * @author Holisun
 */
public class NeoAccessDecisionManager implements AccessDecisionManager
{
	private static final Logger logger = LoggerFactory.getLogger(NeoAccessDecisionManager.class);

	@Override
	public void decide(final Authentication authentication, final Object object, final Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException
	{
		if (CollectionUtils.isEmpty(configAttributes))
		{
			logger.info("There isn't any role.");
			throw new AccessDeniedException("There isn't any role.");
		}
		// 取得資源與角色對應表
		Iterator<ConfigAttribute> iter = configAttributes.iterator();
		while (iter.hasNext())
		{
			ConfigAttribute configAttribute = iter.next();
			// 取得請求該資源所需要的角色
			String needRole = ((SecurityConfig) configAttribute).getAttribute();
			logger.debug("\t [Authorization] Role Needed=[" + needRole + "]");
			// 從 security context 取得目前登入使用者所擁有的全部角色
			for (GrantedAuthority grantedAuthority : authentication.getAuthorities())
			{
				// 判斷使用者擁有的角色是否與請求該資源所需要的角色匹配
				if (needRole.trim()
						.equals(grantedAuthority.getAuthority()
								.trim()))
				{
					logger.debug("\t [Authorization] Role [{}] matched.", needRole);
					return;
				}
			}
		}
		throw new AccessDeniedException("There is no matched role !!");
	}

	@Override
	public boolean supports(final ConfigAttribute arg0)
	{
		return true;
	}

	@Override
	public boolean supports(final Class<?> arg0)
	{
		return true;
	}
}
