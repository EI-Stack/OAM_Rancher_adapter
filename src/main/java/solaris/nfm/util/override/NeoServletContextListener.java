package solaris.nfm.util.override;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeoServletContextListener implements ServletContextListener
{
	private static final Logger logger = LoggerFactory.getLogger(NeoServletContextListener.class.getName());

	@Override
	public void contextInitialized(final ServletContextEvent event)
	{
		logger.trace("\t [System Initialization] - ServletContext has been created.");
		try
		{
			// DefaultScheduler.getInstance().startScheduler();
			// GuaranteeScheduler.getInstance().startScheduler();
			// SystemInternalParameter.getInstance();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		// ServletContext servletContext = event.getServletContext();
	}

	@Override
	public void contextDestroyed(final ServletContextEvent event)
	{
		try
		{
			// 有些資源關閉比較慢，等一下。例如 C3P0
			Thread.sleep(1000);
			// DefaultScheduler.getInstance().stopScheduler();
			// GuaranteeScheduler.getInstance().stopScheduler();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
