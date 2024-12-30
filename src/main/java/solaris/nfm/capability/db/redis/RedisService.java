package solaris.nfm.capability.db.redis;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.exception.util.ExceptionUtil;

/**
 * @author Holisun Wu
 */
// @
@Slf4j
public class RedisService
{
	// 檢查 tenant 的 Redis 資料庫連結時，重試的次數
	private static final Integer	redisRetryCount		= 3;
	// 檢查 tenant 的 Redis 資料庫連結時，重試的間隔時間，單位為秒
	private static final Integer	redisRetryPeriod	= 2;
	@Value("${solaris.redis.ssl:false}")
	private Boolean					isRedisUseSsl;
	@Autowired
	private ObjectMapper			objectMapper;

	public StringRedisTemplate getTemplate(final JsonNode redisConfigNode)
	{
		final StringRedisTemplate template = getStringRedisTemplate();
		template.setConnectionFactory(getConnectionFactory(redisConfigNode));

		// 參數設置完成後，必須執行此函式，進行初始化 RedisTemplate。
		template.afterPropertiesSet();

		return template;
	}

	private StringRedisTemplate getStringRedisTemplate()
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

	/**
	 * 新建 Redis 的連線工廠
	 */
	private LettuceConnectionFactory getConnectionFactory(final JsonNode redisConfigNode)
	{
		// ---[ 設置連線參數 ]-----------------------------------------------------------------------------------------------[S]
		final Long timeout = redisConfigNode.path("timeout").asLong();
		// ---[ 設置連線參數 ]-----------------------------------------------------------------------------------------------[E]

		final RedisConfiguration connConfig = getConnConfig(redisConfigNode);
		final GenericObjectPoolConfig<Object> poolConfig = getPoolConfig(8, 3000, 8, 0);
		LettuceClientConfiguration clientConfiguration;
		if (this.isRedisUseSsl == true)
		{
			clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig).useSsl().and().commandTimeout(Duration.ofMillis(timeout)).shutdownTimeout(Duration.ZERO).build();
		} else
		{
			clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig).commandTimeout(Duration.ofMillis(timeout)).shutdownTimeout(Duration.ZERO).build();
		}
		final LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(connConfig, clientConfiguration);
		connectionFactory.afterPropertiesSet();

		return connectionFactory;
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
	private GenericObjectPoolConfig<Object> getPoolConfig(final int maxActive, final int maxWait, final int maxIdle, final int minIdle)
	{
		final GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
		poolConfig.setMaxTotal(maxActive);
		poolConfig.setMinIdle(minIdle);
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMaxWaitMillis(maxWait);
		return poolConfig;
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
	private RedisConfiguration getConnConfig(final JsonNode redisConfigNode)
	{
		RedisConfiguration redisConfig = null;
		RedisConfigMode configMode = RedisConfigMode.Unknown;

		// ---[ 設置連線參數 ]-----------------------------------------------------------------------------------------------[S]
		// final String mode = redisConfigNode.path("mode").asText();
		final String pwpwpwpw = redisConfigNode.path("password").asText();
		final Integer database = redisConfigNode.path("database").asInt();
		// ---[ 設置連線參數 ]-----------------------------------------------------------------------------------------------[E]

		if ((redisConfigNode.get("master") != null && StringUtils.hasText(redisConfigNode.path("master").asText()))
				&& (redisConfigNode.get("nodes") != null && StringUtils.hasText(redisConfigNode.path("nodes").asText())) && (database != null && database > -1)
				&& (pwpwpwpw != null && StringUtils.hasText(pwpwpwpw)))
			configMode = RedisConfigMode.Sentinel;
		if ((redisConfigNode.get("host") != null && StringUtils.hasText(redisConfigNode.path("host").asText())) && (redisConfigNode.get("port") != null && redisConfigNode.path("port").asInt() > 0)
				&& (database != null && database > -1) && (pwpwpwpw != null && StringUtils.hasText(pwpwpwpw)))
			configMode = RedisConfigMode.Standalone;

		// 依據環境，配置相對應的 config
		switch (configMode)
		{
			case Standalone :
				// ---[ 設置連線參數 ]---------------------------------------------------------------------------------------[S]
				final String hostName = redisConfigNode.path("host").asText();
				final Integer port = redisConfigNode.path("port").asInt();
				// ---[ 設置連線參數 ]---------------------------------------------------------------------------------------[E]
				log.debug("\t[Tenant Redis] 依照所給環境變數綜合判斷採用 Standalone 模式。hostName=[{}], port=[{}], database=[{}]", hostName, port, database);
				final RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
				standaloneConfig.setHostName(hostName);
				standaloneConfig.setPort(port);
				standaloneConfig.setDatabase(database);
				if (!ObjectUtils.isEmpty(pwpwpwpw)) standaloneConfig.setPassword(RedisPassword.of(pwpwpwpw));

				redisConfig = standaloneConfig;
				break;

			case Sentinel :
				// ---[ 設置連線參數 ]---------------------------------------------------------------------------------------[S]
				final String master = redisConfigNode.path("master").asText();
				final String nodes = redisConfigNode.path("nodes").asText();
				// ---[ 設置連線參數 ]---------------------------------------------------------------------------------------[E]
				log.debug("\t[Tenant Redis] 依照所給環境變數綜合判斷採用 Sentinel 模式。master=[{}], nodes=[{}]", master, nodes);

				final Set<String> sentinelHostAndPorts = new HashSet<>(Arrays.asList(nodes.split(",")));
				final RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration(master, sentinelHostAndPorts);
				sentinelConfig.setDatabase(database);
				if (!ObjectUtils.isEmpty(pwpwpwpw)) sentinelConfig.setPassword(RedisPassword.of(pwpwpwpw));

				redisConfig = sentinelConfig;
				break;

			default :
				try
				{
					log.error("\t[Tenant Redis] Redis 參數設定有誤，無法決定使用何種 Redis connection mode. 檢查下列連線參數 /n{}", this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(redisConfigNode));
				} catch (final JsonProcessingException e)
				{
					e.printStackTrace();
				}
				break;
		}

		return redisConfig;
	}

	public enum RedisConfigMode
	{
		Standalone,
		StaticMasterReplica,
		Socket,
		Sentinel,
		Cluster,
		Unknown
	}

	/**
	 * 檢查 tenant 的 Redis 資料庫連結是否正常，並且加上重試機制
	 *
	 * @throws ExceptionBase
	 */
	public void checkRedisConn(final Long tenantId, final StringRedisTemplate template) throws ExceptionBase
	{
		for (int i = 0; i < RedisService.redisRetryCount; i++)
		{
			try
			{
				_checkRedisConn(tenantId, template);
				return;
			} catch (final ExceptionBase e)
			{
				// log.warn("\t[Boot] [DDS] [嘗試次數 {}] Tenant {} 無法連結資料庫，系統無法完成 tenant 初始化。等候 {} 秒後，將會重新嘗試建立連結。", i + 1, tenantId, DynamicDataSourceSummoner.redisRetryPeriod);
				if (i >= RedisService.redisRetryCount - 1)
				{
					throw e;
				}
				try
				{
					TimeUnit.SECONDS.sleep(RedisService.redisRetryPeriod);
				} catch (final InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * 檢查 tenant 的 Redis 資料庫連結是否正常
	 *
	 * @throws ExceptionBase
	 */
	private void _checkRedisConn(final Long tenantId, final StringRedisTemplate template) throws ExceptionBase
	{
		try
		{
			template.randomKey();
		} catch (final Exception e)
		{
			final String message = "Establish tenant " + tenantId + " Redis connection is failed. Detail information: " + ExceptionUtil.getExceptionRootCauseMessage(e);
			// log.error("\t [DDS] " + message);
			throw new ExceptionBase(400, message);
		}
	}

	/**
	 * 檢查 Redis 資料庫連結是否正常
	 *
	 * @throws ExceptionBase
	 */
	public void checkHealth(final StringRedisTemplate template) throws ExceptionBase
	{
		try
		{
			template.randomKey();
		} catch (final Exception e)
		{
			final String message = "[Redis] Can not connect to redis server. Message: " + ExceptionUtil.getExceptionRootCauseMessage(e);
			throw new ExceptionBase(400, message);
		}
	}

}
