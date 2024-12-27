package solaris.nfm.capability.system.multilingual;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

//@Component
@Slf4j
public class EntityBeanPostProcessor implements BeanPostProcessor
{
	@Override
	public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException
	{
		log.debug("beanName={}, class={}", beanName, bean.getClass().getSimpleName());
		log.debug("beanName={}, {}, {}", bean.getClass().isAnnotationPresent(Valid.class), bean.getClass().isAnnotationPresent(RestController.class),
				bean.getClass().isAnnotationPresent(Validated.class));
		// if (!bean.getClass().isAnnotationPresent(Controller.class) && !bean.getClass().isAnnotationPresent(RestController.class))
		// {
		// return bean;
		// }

		if (!beanName.startsWith("javax.validation.constraints") && !beanName.startsWith("org.hibernate.validator.internal.constraintvalidators"))
		{
			// 如果不是 JDK 中的校驗註解並且不是 Hibernate 中的校驗註解，不需要處理
			return bean;
		}

		final Method[] methods = bean.getClass().getDeclaredMethods();
		for (final Method method : methods)
		{
			log.debug("method={}", method);
			final Parameter[] parameters = method.getParameters();
			for (final Parameter parameter : parameters)
			{
				log.debug("parameter={}", parameter.getName());
				log.debug("parameter={}", parameter.isAnnotationPresent(Validated.class));
				// if (!parameter.isAnnotationPresent(Validated.class))
				// {
				// continue;
				// }

				final Class<?> parameterType = parameter.getType();
				final Field[] fields = parameterType.getDeclaredFields();
				for (final Field field : fields)
				{
					log.debug("fieldName={}", field.getName());
					final String fieldName = field.getName();

					final Annotation[] annotations = field.getDeclaredAnnotations();
					log.debug("---------------------annotations.length={}", annotations.length);
					for (final Annotation annotation : annotations)
					{
						log.debug("------------------------------------------------------------------------------------------");
						final String annotationName = annotation.annotationType().getName();
						log.debug("annotationName={}", annotationName);
						if (!annotationName.startsWith("javax.validation.constraints") && !annotationName.startsWith("org.hibernate.validator.internal.constraintvalidators"))
						{
							// 如果不是 JDK 中的校驗註解並且不是 Hibernate 中的校驗註解，不需要處理
							continue;
						}
						log.debug("============================================================================================");

						// 否則，如果註解存在 message 屬性，並且未進行指定，則根據屬性名稱直接為註解指定 message 屬性；
						final Field messageField;
						try
						{
							final InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
							final Field memberValuesField = invocationHandler.getClass().getDeclaredField("memberValues");
							log.debug("memberValuesField={}", memberValuesField);
							if (null == memberValuesField)
							{
								continue;
							}

							memberValuesField.setAccessible(true);

							final Map<String, String> map = (Map<String, String>) memberValuesField.get(invocationHandler);
							final String message = map.get("message");

							// 如果 message 已經存在，並且是預設的訊息，才進行替換，否則不替換
							if (message.startsWith("{javax.validation") || message.startsWith("{org.hibernate.validator.internal.constraintvalidators"))
							{
								map.put("message", "{" + fieldName + "}");
							}
						} catch (NoSuchFieldException | IllegalAccessException e)
						{
							log.error("配置校驗註解的 Message 屬性失敗！", e);

							continue;
						}
					}
				}
			}
		}
		return bean;
	}
}