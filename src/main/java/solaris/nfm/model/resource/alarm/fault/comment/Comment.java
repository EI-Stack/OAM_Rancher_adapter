package solaris.nfm.model.resource.alarm.fault.comment;

import lombok.Data;

@Data
public class Comment
{
	private String	commentText;
	private String	commentTime;
	private Long	commentUserId;
	private String	commentUserName;
}
