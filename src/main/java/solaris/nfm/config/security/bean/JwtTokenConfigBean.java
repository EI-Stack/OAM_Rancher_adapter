package solaris.nfm.config.security.bean;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "solaris.security.jwt-token")
@Validated
@Data
public class JwtTokenConfigBean
{
	private String				tokenHeader;
	private String				tokenPrefixString;
	private Map<String, String>	authenticatedTokens;
}
