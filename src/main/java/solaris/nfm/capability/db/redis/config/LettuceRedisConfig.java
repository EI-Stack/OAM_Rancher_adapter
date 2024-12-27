package solaris.nfm.capability.db.redis.config;

import java.time.Duration;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.db.redis.RedisService.RedisConfigMode;
import solaris.nfm.capability.db.redis.config.RedisConfigBean.Pool;

/**
 * @author Holisun Wu
 */
// @Configuration
@Slf4j
public class LettuceRedisConfig extends CachingConfigurerSupport
{
	@Autowired
	private RedisConfigBean				configBean;
	@Autowired
	private LettuceConnectionFactory	connectionFactory;
	// @Autowired
	// private RedisMessageListenerContainer redisMessageListenerContainer;

	@Bean
	public StringRedisTemplate stringRedisTemplate()
	{
		final StringRedisTemplate template = getRedisTemplate();
		template.setConnectionFactory(this.connectionFactory);
		// 參數設置完成後，必須執行此函式，進行初始化 RedisTemplate。
		template.afterPropertiesSet();
		return template;
	}

	private StringRedisTemplate getRedisTemplate()
	{
		final RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		final Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
		final StringRedisTemplate template = new StringRedisTemplate();
		template.setKeySerializer(stringSerializer);
		template.setValueSerializer(jackson2JsonRedisSerializer);
		template.setHashKeySerializer(stringSerializer);
		template.setHashValueSerializer(jackson2JsonRedisSerializer);

		return template;
	}

	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory()
	{
		final RedisConfiguration connConfig = getConnConfig(this.configBean);
		final GenericObjectPoolConfig<Object> poolConfig = getPoolConfig(this.configBean.getLettuce().getPool());

		LettuceClientConfiguration clientConfiguration;
		if (this.configBean.getSsl() == true)
		{
			clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig).useSsl().and().commandTimeout(Duration.ofMillis(this.configBean.getTimeout()))
					.shutdownTimeout(Duration.ZERO).build();
		} else
		{
			clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig).commandTimeout(Duration.ofMillis(this.configBean.getTimeout())).shutdownTimeout(Duration.ZERO)
					.build();
		}
		return new LettuceConnectionFactory(connConfig, clientConfiguration);
	}

	/**
	 * 新建 Redis 的連線池組態
	 *
	 * @param maxActive
	 * @param maxWait
	 * @param maxIdle
	 * @param minIdle
	 * @return
	 */
	public GenericObjectPoolConfig<Object> getPoolConfig(final Pool pool)
	{
		final GenericObjectPoolConfig<Object> poolconfig = new GenericObjectPoolConfig<>();
		poolconfig.setMaxTotal(pool.getMaxActive());
		poolconfig.setMinIdle(pool.getMinIdle());
		poolconfig.setMaxIdle(pool.getMaxIdle());
		poolconfig.setMaxWaitMillis(pool.getMaxWait());
		return poolconfig;
	}

	/**
	 * 設置 Redis 的連線參數組態。依據運行環境不同，支援 5 種，分別為 RedisStandaloneConfiguration, RedisStaticMasterReplicaConfiguration, RedisSocketConfiguration, RedisSentinelConfiguration, RedisClusterConfiguration
	 * 目前先使用基本型 RedisStandaloneConfiguration，EI-PAAS 估計需要使用 RedisSentinelConfiguration
	 *
	 * @param hostName
	 * @param port
	 * @param password
	 * @param database
	 * @return
	 */
	private RedisConfiguration getConnConfig(final RedisConfigBean configBean)
	{
		RedisConfiguration connConfig = null;
		RedisConfigMode configMode = RedisConfigMode.Unknown;

		if (StringUtils.hasText(configBean.getSentinel().getMaster()) && configBean.getSentinel().getNodes() != null && configBean.getDatabase() > -1 && StringUtils.hasText(configBean.getPassword()))
			configMode = RedisConfigMode.Sentinel;
		if (StringUtils.hasText(configBean.getHost()) && configBean.getPort() > 0 && configBean.getDatabase() > -1 && StringUtils.hasText(configBean.getPassword()))
			configMode = RedisConfigMode.Standalone;

		// 依據環境，配置相對應的 config
		switch (configMode)
		{
			case Standalone :
				log.debug("\t[Default Redis] 依照所給環境變數綜合判斷採用 Standalone 模式。hostName=[{}], port=[{}], database=[{}]", configBean.getHost(), configBean.getPort(), configBean.getDatabase());

				final RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
				standaloneConfig.setHostName(configBean.getHost());
				standaloneConfig.setPort(configBean.getPort());
				standaloneConfig.setDatabase(configBean.getDatabase());
				if (!ObjectUtils.isEmpty(configBean.getPassword())) standaloneConfig.setPassword(RedisPassword.of(configBean.getPassword()));

				connConfig = standaloneConfig;
				break;

			case Sentinel :
				log.debug("\t[Default Redis] 依照所給環境變數綜合判斷採用 Sentinel 模式。master=[{}], nodes=[{}]", configBean.getSentinel().getMaster(), configBean.getSentinel().getNodes());

				final RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration(configBean.getSentinel().getMaster(), configBean.getSentinel().getNodes());
				sentinelConfig.setDatabase(configBean.getDatabase());
				if (!ObjectUtils.isEmpty(configBean.getPassword())) sentinelConfig.setPassword(RedisPassword.of(configBean.getPassword()));

				connConfig = sentinelConfig;
				break;

			default :
				log.error("\t[Default Redis] Redis 參數設定有誤，無法決定使用何種 Redis connection mode.");
				log.debug("\t[Default Redis] hostName=[{}]", configBean.getHost());
				log.debug("\t[Default Redis] database=[{}]", configBean.getDatabase());
				log.debug("\t[Default Redis] password=[{}]", configBean.getPassword());
				log.debug("\t[Default Redis] master=[{}]", configBean.getSentinel().getMaster());
				log.debug("\t[Default Redis] nodes=[{}]", configBean.getSentinel().getNodes());
				break;
		}

		return connConfig;
	}

	// @Bean
	public RedisMessageListenerContainer redisMessageListenerContainer()
	{
		final RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
		redisMessageListenerContainer.setConnectionFactory(this.connectionFactory);
		return redisMessageListenerContainer;
	}

	// @Bean
	public KeyExpiredListener keyExpiredListener()
	{
		// return new KeyExpiredListener(this.redisMessageListenerContainer);
		return null;
	}
}