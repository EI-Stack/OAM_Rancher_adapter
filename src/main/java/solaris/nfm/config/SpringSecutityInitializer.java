package solaris.nfm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

// To register the springSecurityFilterChain with the war.
// AbstractSecurityWebApplicationInitializer that will ensure the springSecurityFilterChain gets registered
@Configuration
public class SpringSecutityInitializer extends AbstractSecurityWebApplicationInitializer
{}