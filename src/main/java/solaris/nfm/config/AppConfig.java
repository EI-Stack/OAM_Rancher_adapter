package solaris.nfm.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.config.security.filter.JwtTokenAuthenticationFilter;

/**
 * @author Holisun Wu
 */
@Slf4j
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages =
{"web"})
@EnableScheduling
// @import 注解允许從另一個配置 XML 檔案中匯入 @Bean 的定義。
// @Import({ DbConfig.class })
// @ImportResource 注解允许從另一個配置類別裡匯入 @Bean 的定義。
// @ImportResource("classpath:skyline/policy/javaconfig/AppConfig.xml")
public class AppConfig
{
	// ================================================================================================================
	// Spring Security
	// ================================================================================================================
	@Bean
	CorsConfigurationSource corsConfigurationSource()
	{
		final CorsConfiguration configuration = new CorsConfiguration();
		// final String[] originArray = {"http://192.168.0.179:8080", "http://192.168.0.179:3000"};
		configuration.addAllowedOriginPattern("*");
		// configuration.setAllowedOrigins(Arrays.asList("http://192.168.0.179:8080"));
		configuration.setAllowedMethods(List.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
		configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
		configuration.setAllowCredentials(true);
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);  // 非常關鍵的 1 個地方，不能設成 /RESTful/** ，否則會攔截不到
		return source;
	}

	@Bean
	public org.springframework.security.web.context.SecurityContextPersistenceFilter securityContextPersistenceFilter()
	{
		return new org.springframework.security.web.context.SecurityContextPersistenceFilter();
	}

	// @Bean
	public ConcurrentSessionFilter concurrentSessionFilter()
	{
		return new ConcurrentSessionFilter(sessionRegistry());
	}

	// 追蹤有效的 session，統計線上人數，顯示線上使用者
	// @Bean
	public SessionRegistry sessionRegistry()
	{
		return new SessionRegistryImpl();
	}

	// @Bean
	public ConcurrentSessionControlAuthenticationStrategy sessionAuthenticationStrategy()
	{
		final ConcurrentSessionControlAuthenticationStrategy concurrentSessionControlAuthenticationStrategy = new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry());
		concurrentSessionControlAuthenticationStrategy.setMaximumSessions(10);
		// true 限制不允许第二個使用者登錄, false 第二個登錄使用者踢掉前一個登錄使用者
		concurrentSessionControlAuthenticationStrategy.setExceptionIfMaximumExceeded(true);
		return concurrentSessionControlAuthenticationStrategy;
	}

	/**
	 * Spring security 使用此 filter 處理 JWT token
	 */
	@Bean
	public JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter() throws Exception
	{
		return new JwtTokenAuthenticationFilter();
	}

	// 裝載 BCrypt 密碼編碼器
	@Bean
	public PasswordEncoder passwordEncoder()
	{
		// return new BCryptPasswordEncoder();
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
		// return new ShaPasswordEncoder(256);
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Thread Pool
	// ----------------------------------------------------------------------------------------------------------------
	@Bean("taskExecutor")
	public ThreadPoolTaskExecutor taskExecutor()
	{
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(500);                    // 线程池创建时候初始化的线程数
		executor.setMaxPoolSize(750);                     // 线程池最大的线程数，只有在缓冲队列满了之后，才会申请超过核心线程数的线程
		executor.setQueueCapacity(100);                   // 用来缓冲执行任务的队列
		executor.setKeepAliveSeconds(60);                 // 超过了核心线程數量上限的线程，當閒置时间到达后会被销毁
		executor.setThreadNamePrefix("taskExecutor-");    // 线程池名稱的前綴
		executor.setRejectedExecutionHandler(new CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		return executor;
	}

	@Bean
	public RestTemplate restTemplate()
	{
		final RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		return restTemplate;
	}

	// ================================================================================================================
	// Jackson 使用以下序列化程序將 Json 時間字串 (2018-12-12 10:10:10) 轉成 LocalDate 類型。
	// 此為全局設定，不必再用 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	// ================================================================================================================
	@Bean
	public ObjectMapper objectMapper()
	{
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
		objectMapper.registerModule(new Jdk8Module());
		final JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
		javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

		objectMapper.registerModules(javaTimeModule, new StringTrimmerModule());
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return objectMapper;
	}

	// ================================================================================================================
	// 此類別主要目的在於解決一個問題，以全局角度自動去除 JSON 字串型別欄位前後的空白字元
	// ================================================================================================================
	class StringTrimmerModule extends SimpleModule
	{
		private static final long serialVersionUID = 1L;

		public StringTrimmerModule()
		{
			addDeserializer(String.class, new StdScalarDeserializer<String>(String.class)
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException, JsonProcessingException
				{
					final String stringValue = jsonParser.getValueAsString();
					return (stringValue == null) ? null : stringValue.trim();
				}
			});
		}
	}

	/**
	 * 会自动注册使用了 @ServerEndpoint 注解声明的 Websocket endpoint
	 * 要注意，如果使用独立的servlet容器，而不是直接使用 springboot 的内置容器，
	 * 就不要注入 ServerEndpointExporter，因为它将由容器自己提供和管理。
	 */
	// @Bean
	// public ServerEndpointExporter serverEndpointExporter()
	// {
	// // log.info("\t[Boot] WebSocket server 啟動完成");
	// return new ServerEndpointExporter();
	// }

	// @Bean("dynamicRoutingDataSource")
	// public DynamicRoutingDataSource dynamicRoutingDataSource(@Value("${solaris.default-tenant.psqlConfig.driver-class-name}") final String driverClassName,
	// @Value("${solaris.default-tenant.psqlConfig.url}") final String jdbcUrl, @Value("${solaris.default-tenant.psqlConfig.username}") final String username,
	// @Value("${solaris.default-tenant.psqlConfig.password}") final String password)
	// {
	// log.debug("\t[Boot] [DDS] 初始化動態 data source");
	// final DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
	// return dataSource;
	// }
}