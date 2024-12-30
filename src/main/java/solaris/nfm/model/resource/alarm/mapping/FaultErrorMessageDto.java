package solaris.nfm.model.resource.alarm.mapping;

import java.util.Set;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import solaris.nfm.controller.dto.ControllerBaseDto;
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;

@Getter
@Setter
public class FaultErrorMessageDto extends ControllerBaseDto
{
	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	private NetworkType	networkType;

	@NotNull(groups = Create.class)
	@Null(groups = Update.class)
	@Positive(groups = {Create.class, Update.class})
	private Integer		code;

	@NotBlank(groups = Create.class)
	@Length(min = 1, max = 256, groups = {Create.class, Update.class})
	private String		message;

	// @NotBlank(message = "Argument networkType (${validatedValue}) must not be blank.", groups = Create.class)
	// @Pattern(regexp = "^(fgc|mec)$", message = "Argument networkType only accepts fgc, mec or ric.", groups = Create.class)
	// @NotNull(message = "Argument code (${validatedValue}) must not be null.", groups = Create.class)
	// @EnumNamePattern(regexp = "^(fgc|mec)$", groups = Create.class)
	// @EnumValidator(value = NetworkType.class, groups = Create.class)
	// @Enum(enumClass = Gender.class, groups = Create.class)

	@Length(min = 1, max = 1024, groups = {Create.class, Update.class})
	private String		sop;

	@NotNull(groups = Create.class)
	private Boolean		mailDisabled;

	private Set<String>	mailAddresses;

	public interface Create
	{}

	public interface Update
	{}
}