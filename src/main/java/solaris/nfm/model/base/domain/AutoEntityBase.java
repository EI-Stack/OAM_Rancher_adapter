package solaris.nfm.model.base.domain;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public abstract class AutoEntityBase extends EntityBase
{
	private static final long	serialVersionUID	= 1L;
	@Id
	@Basic(optional = false)
	@Column(name = "id", unique = true, nullable = false, columnDefinition = "BIGINT UNSIGNED")
	protected Long				id;
	@Column(name = "entity_version")
	@Version
	private Integer				entityVersion;

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public void setId(final Long id)
	{
		this.id = id;
	}

	public Integer getEntityVersion()
	{
		return this.entityVersion;
	}

	@Override
	public int hashCode()
	{
		int hash = 0;
		hash += (this.getId() != null ? this.getId().hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(final Object object)
	{
		if (this == object) return true;
		if (object == null) return false;
		if (getClass() != object.getClass()) return false;
		AutoEntityBase other = (AutoEntityBase) object;
		if (this.getId() != other.getId() && (this.getId() == null || !this.id.equals(other.id)))
		{
			return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return this.getClass().getName() + "(ID: " + this.id + ")";
	}

	//	@Override
	//	public String getHumanReadableEntityName()
	//	{
	//		return WordUtils.uncapitalize(this.getClass().getSimpleName().replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2"), ' ');
	//	}
}
