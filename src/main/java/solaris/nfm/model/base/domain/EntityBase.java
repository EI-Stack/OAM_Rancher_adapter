package solaris.nfm.model.base.domain;

import java.io.Serializable;

import org.apache.commons.text.WordUtils;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class EntityBase implements Serializable
{
	private static final long serialVersionUID = 1L;

	public Long getId()
	{
		return null;
	}

	public void setId(final Long id)
	{}

	public String getHumanReadableEntityName()
	{
		return WordUtils.uncapitalize(this.getClass().getSimpleName().replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2"), ' ');
	}

	public String getHumanReadableEntityBrief()
	{
		return getHumanReadableEntityName();
	}
}
