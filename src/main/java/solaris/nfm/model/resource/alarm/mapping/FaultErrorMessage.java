package solaris.nfm.model.resource.alarm.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class FaultErrorMessage extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	@Enumerated(EnumType.STRING)
	private NetworkType			networkType;
	private Integer				code;
	private String				message;
	private String				sop;
	private Boolean				mailDisabled;
	@Type(ListArrayType.class)
	private List<String>		mailAddresses		= new ArrayList<>();
}
