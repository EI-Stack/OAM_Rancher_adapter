package solaris.nfm.model.base.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
public abstract class AuditEntityBase extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;
	@Column(name = "creation_time")
	private LocalDateTime		creationTime;
	@Column(name = "creator")
	private Long				creator;
	@Column(name = "modification_time")
	private LocalDateTime		modificationTime;
	@Column(name = "modifier")
	private Long				modifier;

	public LocalDateTime getCreationTime()
	{
		return this.creationTime;
	}

	public void setCreationTime(final LocalDateTime creationTime)
	{
		this.creationTime = creationTime;
	}

	public Long getCreator()
	{
		return this.creator;
	}

	public void setCreator(final Long creator)
	{
		this.creator = creator;
	}

	public LocalDateTime getModificationTime()
	{
		return this.modificationTime;
	}

	public void setModificationTime(final LocalDateTime modificationTime)
	{
		this.modificationTime = modificationTime;
	}

	public Long getModifier()
	{
		return this.modifier;
	}

	public void setModifier(final Long modifier)
	{
		this.modifier = modifier;
	}

	/**
	 * Sets createdAt before insert
	 */
	@PrePersist
	public void onPrePersist()
	{
		this.creationTime = LocalDateTime.now();
	}

	/**
	 * Sets updatedAt before update
	 */
	@PreUpdate
	public void onPreUpdate()
	{
		this.modificationTime = LocalDateTime.now();
	}
}
