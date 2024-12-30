package solaris.nfm.model.resource.alarm.performance.rule;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import solaris.nfm.support.YamlSourceFactory;

@Component
@PropertySource(value = "config/custom/performance-rule-name.yaml", factory = YamlSourceFactory.class)
@ConfigurationProperties(prefix = "performance-rule-name")
@Getter
@Setter
public class PerformanceRuleNameDefBean
{
	private Map<String, List<String>> nameList;

	// private List<String> fgc;
	// private List<String> mec;
	// private List<String> ric;
}