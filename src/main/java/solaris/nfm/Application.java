package solaris.nfm;

import java.util.TimeZone;

import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
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

import jakarta.annotation.PostConstruct;
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
		final ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
		final Runtime jvmRumtime = Runtime.getRuntime();
		final ApplicationMainParameter applicationMainParameter = applicationContext.getBean(ApplicationMainParameter.class);

		log.info("\t[Boot] CPU Core=[{}], Max Memory=[{} MB], Total Memory=[{} MB], Free Memory=[{} MB]", jvmRumtime.availableProcessors(), jvmRumtime.maxMemory() / (1024 * 1024),
				jvmRumtime.totalMemory() / (1024 * 1024), jvmRumtime.freeMemory() / (1024 * 1024));
		log.info("\t[Boot] OS=[{}], JVM Version=[{}]", System.getProperty("os.name"), Runtime.version());
		log.info("\t[Boot] {} v{} 啟動完成，開始執行。Go ============================================================>>>", applicationMainParameter.getAppName(), applicationMainParameter.getAppVersion());

		// ---[ RMQ Management ]---------------------------------------------------------------------------------------[S]
		// 原本 @RabbitListener 預設是自動開啟的，但這樣會造成一個嚴重問題
		// 所謂的開啟，時機點是在 Spring Boot Application 執行之前，此時資料庫尚未連通，所以接收到的消息無法正確處理
		// 為此，將 @RabbitListener 設成不啟動，確定所有前置都執行完成後，在此做手動開啟
		final RabbitListenerEndpointRegistry registry = applicationContext.getBean(RabbitListenerEndpointRegistry.class);
		try
		{
			// 打開 RMQ listener
			// registry.getListenerContainer("lm-to-nfm").start();
			registry.getListenerContainer("adapter-to-nfm").start();
		} catch (final Exception e1)
		{
			log.error("\t[Boot] [RMQ] 由於無法開啟 RabbitMQ listener，NFM 的初始化已然失敗了。\n\t 錯誤訊息: {} \n\t 請檢查網路組態與 RabbitMQ 是否設置正確 \n\n\t注意囉！ NMS 即刻中止運行，後續的自動重啟程序將交由 Kubernates 接手。", e1.getMessage());
			System.exit(-1);
		}
		// ---[ RMQ Management ]---------------------------------------------------------------------------------------[E]

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
		@Value("${info.app.name}")
		private String	appName;

		@Getter
		@Value("${info.app.version}")
		private String	appVersion;
	}

	@PostConstruct
	void started()
	{
		// 2021-04-19 經過測試，這個方法並不會被執行
		TimeZone.setDefault(TimeZone.getTimeZone("ETC/UTC"));
	}
}