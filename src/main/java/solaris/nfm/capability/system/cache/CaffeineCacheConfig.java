package solaris.nfm.capability.system.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
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
	// 設定最後一次寫入或訪問後,經過固定時間資料過期 (分鐘)
	@Value("${spring.cache.clear-interval}")
	private int CAFFEINE_EXPIRE_TIME;

	@Bean
	CacheManager cacheManager()
	{
		final CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		if(CAFFEINE_EXPIRE_TIME > 60) {  //如果超過 60 分鐘，直接設定為 60 分鐘
			CAFFEINE_EXPIRE_TIME = 60;
		}else if(CAFFEINE_EXPIRE_TIME < 0) {  //如果小於0 直接設為0
			CAFFEINE_EXPIRE_TIME = 0;
		}
		cacheManager.setCaffeine(Caffeine.newBuilder().initialCapacity(100).maximumSize(CAFFEINE_MAX_SIZE).expireAfterAccess(CAFFEINE_EXPIRE_TIME, TimeUnit.MINUTES).recordStats());
		return cacheManager;
	}
}