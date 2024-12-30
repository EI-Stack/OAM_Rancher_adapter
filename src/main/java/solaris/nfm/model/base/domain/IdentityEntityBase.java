package solaris.nfm.model.base.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class IdentityEntityBase extends EntityBase
{
	private static final long	serialVersionUID	= 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
		// 使用 Hibernate 代替 OpenJPA 後，下列判斷式變成不正確，所以不使用
		// if (getClass() != object.getClass()) return false;
		final IdentityEntityBase other = (IdentityEntityBase) object;
		if (this.getId() != other.getId() && (this.getId() == null || !this.id.equals(other.id))) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(ID: " + this.id + ")";
	}
}
