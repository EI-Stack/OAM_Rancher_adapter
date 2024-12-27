package solaris.nfm.config.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class SystemAbsolutionPath
{
	private String path;

	public SystemAbsolutionPath()
	{
		String classPath = getClass().getResource("/")
				.getFile();
		try
		{
			this.path = URLDecoder.decode(classPath, "utf-8");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}

	public String getPath()
	{
		return this.path;
	}

	public void setPath(final String path)
	{
		this.path = path;
	}
}
