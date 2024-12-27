package solaris.nfm.util;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.instrument.classloading.LoadTimeWeaver;

public class OpenJPALoadTimeWeaver implements LoadTimeWeaver
{
	public OpenJPALoadTimeWeaver()
	{
		return;
	}

	@Override
	public void addTransformer(final ClassFileTransformer transformer)
	{
		// nothing
		return;
	}

	@Override
	public ClassLoader getInstrumentableClassLoader()
	{
		return this.getClass().getClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader()
	{
		return this.getClass().getClassLoader();
	}
}