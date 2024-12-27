package solaris.nfm.config.support;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

/**
 * Spring Boot @PropertySource 預設只載入 properties 檔，如果想要載入 yaml，則要自行實做
 */
public class YamlPropertySourceFactory implements PropertySourceFactory
{
	@Override
	public PropertySource<?> createPropertySource(final String name, final EncodedResource encodedResource) throws IOException
	{
		final YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(encodedResource.getResource());

		final Properties properties = factory.getObject();

		return new PropertiesPropertySource(encodedResource.getResource().getFilename(), properties);
	}
}