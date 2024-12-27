package solaris.nfm;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
// 讓 JPA 2.1 的 java.sql.Date/Timestamp 能夠映射轉換 JDK 8 的 LocalDateTime
@EntityScan(basePackageClasses = {Application.class, Jsr310JpaConverters.class})
// 啟用異步處理
@EnableAsync
@EnableCaching
@EnableRetry
public class Application
{
	public static void main(final String[] args)
	{
		final ConfigurableApplicationContext applicationContext = ((ConfigurableApplicationContext)SpringApplication.run(Application.class, args));
		final Runtime jvmRumtime = Runtime.getRuntime();
		final ApplicationMainParameter applicationMainParameter = applicationContext.getBean(ApplicationMainParameter.class);

		log.info("\t[Boot] CPU Core=[{}], Max Memory=[{} MB], Total Memory=[{} MB], Free Memory=[{} MB]", jvmRumtime.availableProcessors(), jvmRumtime.maxMemory() / (1024 * 1024),
				jvmRumtime.totalMemory() / (1024 * 1024), jvmRumtime.freeMemory() / (1024 * 1024));
		log.info("\t[Boot] OS=[{}], JVM Version=[{}]", System.getProperty("os.name"), Runtime.version());
		log.info("\t[Boot] NFM (Network Function Manager) v{} 啟動完成，開始執行。Go ============================================================>>>", applicationMainParameter.getAppVersion());

		// 顯示目前的 role hierarchy
		// RoleHierarchy roleHierarchy = SpringApplication.run(Application.class, args).getBean(RoleHierarchy.class);
		// for (String role : List.of("ROLE_ADMIN", "ROLE_Portal", "ROLE_LwM2M-DM", "ROLE_Tenant-Manager"))
		// {
		// log.info("Role: {} implies: {}", role, roleHierarchy.getReachableGrantedAuthorities(createAuthorityList(role)));
		// }
	}

	/**
	 * Application 無法使用 @Value 取值，原因是 Application 並非是 Spring 納管。
	 * 所以宣告此類，用來轉接，讓 main() 能夠使用 @Value 取值
	 */
	@Component
	class ApplicationMainParameter
	{
		@Getter
		@Value("${management.info.app.version}")
		private String appVersion;
	}

	@PostConstruct
	void started()
	{
		// 2021-04-19 經過測試，這個方法並不會被執行
		TimeZone.setDefault(TimeZone.getTimeZone("ETC/UTC"));
	}
}