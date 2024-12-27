package solaris.nfm.capability.db.redis.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

//@Component
@ConfigurationProperties(prefix = "spring.redis")
@Getter
@Setter
public class RedisConfigBean
{
	private String		host;
	private Integer		port;
	private String		password;
	private Integer		database;
	private Long		timeout;
	private Boolean		ssl;

	private Lettuce		lettuce;
	private Sentinel	sentinel;

	@Getter
	@Setter
	public static class Lettuce
	{
		private Pool pool;
	}

	@Getter
	@Setter
	public static class Sentinel
	{
		private String		master;
		private Set<String>	nodes;
	}

	@Getter
	@Setter
	public static class Pool
	{
		private Integer	maxActive;
		private Integer	maxIdle;
		private Integer	minIdle;
		private Long	maxWait;
	}
}
