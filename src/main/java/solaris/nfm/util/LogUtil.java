package solaris.nfm.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class LogUtil
{
	public static String getStackTraceString(final Exception exception)
	{
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		exception.printStackTrace(printWriter);
		return writer.toString();
	}
}
