package solaris.nfm.capability.system.multilingual;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MultilingualConfig
{
	@Bean
	LocalValidatorFactoryBean localValidatorFactoryBean(final MessageSource messageSource)
	{
		final LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
		// 設置 fail_Fast = true 之後，如果校驗時發現了錯誤就會停止後續參數的校驗
		// bean.getValidationPropertyMap().put("hibernate.validator.fail_fast", "true");
		bean.setValidationMessageSource(messageSource);
		return bean;
	}

	@Bean
	LocaleResolver localeResolver()
	{
		return new HeaderLocaleResolver();
	}

	public class HeaderLocaleResolver extends AcceptHeaderLocaleResolver
	{
		private final String langHeader = "Accept-Language";

		@Override
		public Locale resolveLocale(final HttpServletRequest request)
		{
			log.debug("\t Locale from API = [{}]", request.getHeader(langHeader));
			if (request.getHeader(langHeader) == null || StringUtils.hasText(request.getHeader(langHeader)) == false)
			{
				return Locale.getDefault();
			}
			final List<Locale.LanguageRange> list = Locale.LanguageRange.parse(request.getHeader(langHeader));
			log.debug("\t Locale list={}", list);
			return Locale.lookup(list, Arrays.asList(Locale.US, Locale.TAIWAN));
		}
	}
}