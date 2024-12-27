package solaris.nfm.controller.dto;

public class SearchCriteriaDto
{
	private String	key;
	private String	operation;
	private Object	value;

	public SearchCriteriaDto(final String key, final String operation, final Object value)
	{
		setKey(key);
		setOperation(operation);
		setValue(value);
	}

	public String getKey()
	{
		return this.key;
	}

	public void setKey(final String key)
	{
		this.key = key;
	}

	public String getOperation()
	{
		return this.operation;
	}

	public void setOperation(final String operation)
	{
		this.operation = operation;
	}

	public Object getValue()
	{
		return this.value;
	}

	public void setValue(final Object value)
	{
		this.value = value;
	}
}
