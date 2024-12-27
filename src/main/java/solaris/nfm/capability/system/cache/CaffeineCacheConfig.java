package solaris.nfm.capability.system.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CaffeineCacheConfig
{
	// 快取的最大筆數
	public static final Integer	CAFFEINE_MAX_SIZE		= 500;
	// 設定最後一次寫入或訪問後,經過固定時間資料過期 (秒)
	public static final Integer	CAFFEINE_EXPIRE_TIME	= 30;

	@Bean
	CacheManager cacheManager()
	{
		final CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeine(Caffeine.newBuilder().initialCapacity(100).maximumSize(CAFFEINE_MAX_SIZE).expireAfterAccess(CAFFEINE_EXPIRE_TIME, TimeUnit.SECONDS).recordStats());
		return cacheManager;
	}
}