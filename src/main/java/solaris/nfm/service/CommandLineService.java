package solaris.nfm.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommandLineService
{
	public Boolean runCommand(final String commandString)
	{
		Boolean result = false;
		try
		{
			Runtime runtime = Runtime.getRuntime();
			// Process process = runtime.exec("cmd /c dir");
			Process process = runtime.exec(commandString);
			try (InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream()); BufferedReader bufferedReader = new BufferedReader(inputStreamReader);)
			{
				String line = null;
				while ((line = bufferedReader.readLine()) != null)
				{
					log.debug("\t [CommandLine] {}", line);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			// int exitValue = process.waitFor();
			// logger.debug("\t [CommandLine] Exited with error code " + exitValue);
			// if (exitValue == 0) result = true;
			result = true;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
}