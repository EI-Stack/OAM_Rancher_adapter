package solaris.nfm.capability.system.aop.dto;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class DeviceLogDto extends LogBaseDto
{
	private Long		deviceId;
	private String		endPointName;
	private String		operationName;
	private JsonNode	operationDetail;
	private JsonNode	operationResult;
	private String		operationError;
	private Integer		coapStatus;
	private Boolean		success;

	public DeviceLogDto()
	{
		super.setLogType(LogBaseDto.LogType.Device);
	}
}
