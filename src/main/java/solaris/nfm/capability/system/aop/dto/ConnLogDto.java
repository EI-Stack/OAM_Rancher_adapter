package solaris.nfm.capability.system.aop.dto;

import lombok.Data;

@Data
public class ConnLogDto extends LogBaseDto
{
	private Long		deviceId;
	private String		endPointName;
	private ConnType	connType;

	public ConnLogDto()
	{
		super.setLogType(LogBaseDto.LogType.Connection);
	}

	public enum ConnType
	{
		Register,
		Update,
		Unregister;
	}
}
