package solaris.nfm.model.resource.security;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.databind.node.ArrayNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import solaris.nfm.controller.dto.ControllerBaseDto;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.CvssSeverity;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.DetectionInterface;
import solaris.nfm.model.resource.alarm.security.dtm.DtmAlarm.DetectionType;

@Getter
@Setter
public class MitigationDto extends ControllerBaseDto
{
	@NotBlank(groups = Create.class)
	@Null(groups = Update.class)
	private String				name;

	@Length(min = 1, max = 1024, groups = {Create.class, Update.class})
	private String				description;

	private DetectionType		detectionType;

	private DetectionInterface	detectionInterface;

	private CvssSeverity		perceivedSeverity;

	@NotNull(groups = Create.class)
	private ArrayNode			procedures;

	@NotNull(groups = Create.class)
	private ArrayNode			parameters;

	public interface Create
	{}

	public interface Update
	{}
}