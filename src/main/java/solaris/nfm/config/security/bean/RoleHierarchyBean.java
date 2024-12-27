package solaris.nfm.config.security.bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "solaris.security")
@Getter
@Setter
public class RoleHierarchyBean
{
	Map<String, List<String>> roleHierarchy = new LinkedHashMap<>(5);
}
