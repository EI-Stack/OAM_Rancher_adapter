package solaris.nfm.capability.system.aop.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LogBaseDto
{
	private LogType	logType;
	private Long	logTime	= System.currentTimeMillis();

	public enum LogType
	{
		API,
		Device,
		Observe,
		Connection,
		Policy,
		Model;
	}

	public enum OperationType
	{
		Install,
		Uninstall,
		Activate,
		Deactivate,
		Update;
	}
}