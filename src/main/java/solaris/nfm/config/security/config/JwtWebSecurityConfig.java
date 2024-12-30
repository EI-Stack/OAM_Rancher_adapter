package solaris.nfm.config.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

import solaris.nfm.config.security.NeoAccessDecisionManager;
import solaris.nfm.config.security.bean.RoleHierarchyBean;
import solaris.nfm.config.security.domain.JwtAuthenticationEntryPoint;
import solaris.nfm.config.security.filter.JwtTokenAuthenticationFilter;
import solaris.nfm.config.security.util.RoleHierarchyUtil;

@Configuration
@EnableWebSecurity
public class JwtWebSecurityConfig
{
	@Autowired
	private JwtTokenAuthenticationFilter	jwtTokenAuthenticationFilterFilter;
	// @Autowired
	// private RestResourceDaoService restResourceDaoService;
	// 未授權請求的回覆處理
	@Autowired
	private JwtAuthenticationEntryPoint		unauthorizedHandler;
	// private Http403ForbiddenEntryPoint unauthorizedHandler;
	@Autowired
	// 限定实现类实例名
	@Qualifier("jwtUserDetailsService")   // 限定接口 UserDetailsService 必须绑 jwtUserDetailsService
	private UserDetailsService				userDetailsService;
	// Spring会自动寻找同样类型的具体类注入，这里就是JwtUserDetailsServiceImpl了

	@Bean
	RoleHierarchy roleHierarchy(final RoleHierarchyBean roleHierarchyBean)
	{
		return RoleHierarchyUtil.getRoleHierarchyFromMap(roleHierarchyBean.getRoleHierarchy());
	}

	@Autowired
	public void configureAuthentication(final AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception
	{}

	@Bean
	protected SecurityFilterChain filterChain(final HttpSecurity httpSecurity) throws Exception
	{
		// httpSecurity.requiresChannel(channel -> channel.anyRequest().requiresSecure());

		// @formatter:off
		httpSecurity
		// 禁用 basic 明文驗證
        .httpBasic().disable()
		// 由於採用了 JWT，所以不需要 CSRF 功能
		.csrf().ignoringRequestMatchers("/v1/**").and()
		// 支持跨域访问
		.cors().and()
		// 由於採用了 token，所以不需要 session，關閉不需要的功能以節省記憶體
		.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
		.authorizeHttpRequests()
		// .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		// 讀取 json schema 不須驗證，直接放行 (本質上是讀取靜態文件資源，預設目錄是 /public, /static, /resources)
		// .antMatchers("/static/**").hasAnyRole("GUEST")
		// 讀取 swagger 不須驗證，直接放行
		.requestMatchers("/v2/api-docs", "/configuration/**", "/swagger-resources/**", "/configuration/security", "/swagger-ui.html", "/webjars/**").hasAnyRole("GUEST")
		// 讀取健康狀態不須驗證，直接放行 (但日後應該提高讀取權限)
		.requestMatchers("/actuator/**", "/webSocketServer/**","/ws/**").hasAnyRole("GUEST")
		// 讀取 grafana webhook 不須驗證，直接放行
		.requestMatchers("/v1/webhook/**").hasAnyRole("GUEST")
		// 讀取 Affirmed webhook 不須驗證，直接放行
		.requestMatchers("/hooks/Elastalert/**").hasAnyRole("GUEST")
		// 讀取資安稽核事件，不須驗證，直接放行
		.requestMatchers("/v1/securityEvents/**").hasAnyRole("GUEST")
		.requestMatchers(HttpMethod.POST, "/v1/statistics/**").hasAnyRole("LogManager")
		.anyRequest().hasAnyRole("Portal");


		/*
		 * .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
		 * @Override
		 * public <O extends FilterSecurityInterceptor> O postProcess(O filterSecurityInterceptor) {
		 * filterSecurityInterceptor.setAccessDecisionManager(myAccessDecisionManager());
		 * filterSecurityInterceptor.setSecurityMetadataSource(mySecurityMetadataSource());
		 * return filterSecurityInterceptor;
		 * }
		 * });
		 */
		httpSecurity    // 把过滤器添加到安全策略里面去
		.addFilterBefore(jwtTokenAuthenticationFilterFilter, UsernamePasswordAuthenticationFilter.class);
		//.addFilterBefore(new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint()), jwtTokenAuthenticationFilterFilter.getClass());

		// 禁用缓存
		httpSecurity.headers()
		.frameOptions()
		.sameOrigin()  // required to set for H2 else H2 Console will be blank.
		.cacheControl();

		// 這段是針對 Spring Security 6 所作的修正，為官網建議的預設值
		httpSecurity
		.securityContext(securityContext -> securityContext
			.securityContextRepository(new DelegatingSecurityContextRepository(
				new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository()
			))
		)
		.securityContext(securityContext -> securityContext.requireExplicitSave(true));
		// @formatter:on

		return httpSecurity.build();
	}

	// Spring Security 是通过 SecurityMetadataSource 来加载访问时所需要的具体权限
	// 有 2 個界面可用，SecurityMetadataSource 與 FilterInvocationSecurityMetadataSource
	// 因为我们做的一般都是 web 项目，所以实际需要实现的接口是 FilterInvocationSecurityMetadataSource，
	// 這是因為 Spring Security 中很多 web 使用的類別參數類型都是 FilterInvocationSecurityMetadataSource。
	// 注意！ 跟我想的不一樣 @Bean 是不能移除，否則會運作異常
	// 原先以為沒有別的程序使用，不用宣告 @Bean，但，由於會使用到 restResourceDaoService
	// 需要一起做 bean initialization，否則 restResourceDaoService 就會取到 null
	// @Bean
	public FilterInvocationSecurityMetadataSource mySecurityMetadataSource()
	{
		// NeoFilterInvocationSecurityMetadataSource securityMetadataSource = new NeoFilterInvocationSecurityMetadataSource(restResourceDaoService);
		// return securityMetadataSource;
		return null;
	}

	// @Bean
	public AccessDecisionManager myAccessDecisionManager()
	{
		return new NeoAccessDecisionManager();
	}
}
