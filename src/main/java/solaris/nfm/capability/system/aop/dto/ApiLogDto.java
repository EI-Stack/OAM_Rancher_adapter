package solaris.nfm.capability.system.aop.dto;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ApiLogDto extends LogBaseDto
{
	private Long		userId;
	private String		username;
	private ApiType		apiType;
	private String		targetId;
	private String		operationTag;
	private String		operationName;
	// 操作時所傳入的 dto
	private JsonNode	operationInput;
	private JsonNode	operationOutput;
	private String		operationError;
	private String		requestMethod;
	private String		requestUri;
	private Integer		responseStatusCode;
	private Boolean		isSuccessful;
	private String		userIp;

	public ApiLogDto()
	{
		super.setLogType(LogBaseDto.LogType.API);
	}

	public enum ApiType
	{
		DeviceLog,
		SystemLog;
	}
}