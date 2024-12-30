package solaris.nfm.model.resource.alarm.fault.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentSsDto
{
	@NotBlank
	private String commentText;
}