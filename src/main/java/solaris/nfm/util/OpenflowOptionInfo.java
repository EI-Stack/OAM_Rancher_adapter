package solaris.nfm.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "solaris.server.open-daylight.openflow")
public class OpenflowOptionInfo
{
	// 作用？
	private boolean vlanMatch;
}