package solaris.nfm.exception.base;

import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Holisun Wu
 */
@Getter
@Setter
public class ExceptionBase extends Exception
{
	private static final long	serialVersionUID	= 1L;
	private Integer				errorCode;
	private String				errorMessage;

	public ExceptionBase()
	{
		super();
	}

	public ExceptionBase(final String message)
	{
		super(message);
		setErrorMessage(message);
	}

	public ExceptionBase(final Integer errorCode, final String errorMessage)
	{
		setErrorCode(errorCode);
		setErrorMessage(errorMessage);
	}

	/**
	 * 將 DeviceType 轉換成 device type
	 *
	 * @param source
	 * @return
	 */
	public static String toSimple(final String source)
	{
		if (!StringUtils.hasText(source)) return "";
		return source.trim().replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2").toLowerCase();
	}
}
