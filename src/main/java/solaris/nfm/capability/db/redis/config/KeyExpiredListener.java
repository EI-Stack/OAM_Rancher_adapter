package solaris.nfm.capability.db.redis.config;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import lombok.extern.slf4j.Slf4j;

/**
 * @Title: KeyExpiredListener.java
 * @Package com.spring.pro.listener
 * @Description:
 * @author ybwei
 * @date 2018年8月13日 下午3:54:41
 * @version V1.0
 */

// @Component
@Slf4j
public class KeyExpiredListener extends KeyExpirationEventMessageListener
{
	public KeyExpiredListener(final RedisMessageListenerContainer listenerContainer)
	{
		super(listenerContainer);
	}

	@Override
	public void onMessage(final Message message, final byte[] pattern)
	{
		System.out.println("onPMessage pattern " + pattern + " " + " " + message);
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		String key = new String(message.getBody(), StandardCharsets.UTF_8);
		log.info("\t [Redis] redis key 过期：pattern={}, channel={}, key={}", new String(pattern), channel, key);
	}
}