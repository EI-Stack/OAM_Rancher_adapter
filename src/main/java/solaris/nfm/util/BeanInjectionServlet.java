package solaris.nfm.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 支持 {@link Resource} 和 {@link Autowired} 动态注入的 GWT RPC 基类。
 */
@SuppressWarnings("serial")
public abstract class BeanInjectionServlet extends HttpServlet
{
	/** 日志记录实体。 */
	private static final Logger logger = LoggerFactory.getLogger(BeanInjectionServlet.class);

	// protected Log logger = LogFactory.getLog(getClass());
	@Override
	public void init() throws ServletException
	{
		super.init();
		try
		{
			doInjection();
		} catch (Throwable t)
		{
			logger.error(null, t);
			if (t instanceof ServletException)
			{
				throw (ServletException) t;
			}
			throw new ServletException(t);
		}
	}

	/**
	 * 执行服务器端自动依赖注入操作。
	 *
	 * @throws ServletException
	 *         注入过程中发生错误。
	 */
	protected void doInjection() throws ServletException
	{
		// 找到 ApplicationContext
		WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		if (ctx == null)
		{
			logger.error("\t [ Injection Servelet ] Can not find Spring web application context, failed to do injection");
			return;
		}
		// 查找实现类的元数据，并进行依赖注入
		for (Field field : getClass().getDeclaredFields())
		{
			for (Annotation annotation : field.getDeclaredAnnotations())
			{
				if (annotation instanceof Resource)
				{
					Resource res = (Resource) annotation;
					if (res.name() != null && !"".equals(res.name().trim()))
					{
						// beanName injection
						if (!injectWithBeanName(res.name(), field, ctx))
						{
							logger.warn("\t [ Injection Servelet ] Can not find bean with name '" + res.name() + "', auto-injection failed");
						}
					} else if (!Object.class.equals(res.type()))
					{
						// beansType injection
						Collection<?> c = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, (Class<?>) res.type()).values();
						if (field.getType().isAssignableFrom(Collection.class))
						{
							// 集合类型
							try
							{
								field.setAccessible(true);
								field.set(this, c);
							} catch (Exception e)
							{
								logger.error("\t [ Injection Servelet ]Failed to do injection for " + field, e);
								throw new ServletException(e);
							}
						} else
						{
							// 单值类型
							if (c.size() == 1)
							{
								try
								{
									field.setAccessible(true);
									field.set(this, c.iterator().next());
								} catch (Exception e)
								{
									logger.error("Failed to do injection for " + field, e);
									throw new ServletException(e);
								}
							} else if (c.size() > 1)
							{
								logger.error("\t [ Injection Servelet ] Find more than one beans of type '" + res.type() + ", auto-injection failed");
							} else
							{ // c.size() == 0
								logger.error("\t [ Injection Servelet ] Can not find bean of type '" + res.type() + ", auto-injection failed");
							}
						}
					} else
					{
						// 尝试根据字段的名称自动匹配注入
						if (!injectWithBeanName(field.getName(), field, ctx))
						{
							logger.error("\t [ Injection Servelet ] Either beanName or beansType attribute must be set for " + "@Resource annotation, failed to do injection for " + field);
						}
					}
				} else if (annotation instanceof Autowired)
				{
					// 尝试根据字段的名称自动匹配注入
					if (!injectWithBeanName(field.getName(), field, ctx))
					{
						logger.error("\t [ Injection Servelet ] Failed to do injection for " + field);
					}
				}
			}
		}
	}

	/**
	 * 按照 Bean 的名称进行注入。
	 *
	 * @param beanName
	 *        Bean 的名称进行注入。
	 * @param field
	 *        注入的字段。
	 * @param webApplicationContext
	 *        Spring Application Context.
	 * @return 是否注入成功。
	 * @throws ServletException
	 *         无法更改待注入字段的值。
	 */
	private boolean injectWithBeanName(final String beanName, final Field field, final WebApplicationContext webApplicationContext) throws ServletException
	{
		Object bean = null;
		try
		{
			try
			{
				bean = webApplicationContext.getBean(beanName + "Impl");
			} catch (Exception e)
			{
				logger.error("\t [ Injection Servelet ] Can not find Bean (" + beanName + "Impl" + ")");
			}
			if (bean == null)
			{
				bean = webApplicationContext.getBean(beanName);
				if (bean == null)
				{
					logger.error("\t [ Injection Servelet ] Can not find Bean (" + beanName + ")");
					return false;
				}
			}
			logger.debug("\t [ Injection Servelet ] Inject bean(" + beanName + ")");
			if (bean.getClass().isAssignableFrom(field.getClass()))
			{
				// 类型不匹配
				logger.error("类型不匹配" + field);
				return false;
			}
		} catch (Exception e)
		{
			// 找不到 Bean
			logger.error("\t [ Injection Servelet ] Can not find Bean (" + beanName + ")", e);
			return false;
		}
		try
		{
			field.setAccessible(true);
			field.set(this, bean);
			return true;
		} catch (Exception e)
		{
			logger.error("\t  [ Injection Servelet ] Failed to do injection for " + beanName, e);
			throw new ServletException(e);
		}
	}
}