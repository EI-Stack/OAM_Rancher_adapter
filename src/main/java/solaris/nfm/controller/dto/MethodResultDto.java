package solaris.nfm.controller.dto;

import lombok.Data;

@Data
public class MethodResultDto
{
	private Boolean	status	= true;
	private Integer	code	= 0;
	private String	error	= "";

	public MethodResultDto()
	{}

	public MethodResultDto(final boolean status, final int code, final String error)
	{
		setStatus(status);
		setCode(code);
		setError(error);
	}
}
