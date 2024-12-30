package solaris.nfm.model.resource.alarm.fault.comment;

import java.util.Set;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentIsDto
{
	@NotNull
	@Length(min = 1)
	private Set<Long>	alarmInformationReferenceList;
	@NotBlank
	private String		commentText;
}