package solaris.nfm.model.resource.statistic;

import java.time.LocalDate;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import solaris.nfm.model.base.domain.IdentityEntityBase;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class Statistic extends IdentityEntityBase
{
	private static final long	serialVersionUID	= 1L;

	private LocalDate			date;
	@Type(JsonType.class)
	private JsonNode			data;
}
