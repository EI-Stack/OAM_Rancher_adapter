package solaris.nfm.controller.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchNodeDto implements Serializable
{
	@NotNull
	private String	nodeId;
	@NotNull
	private String	sourcePort;
	@NotNull
	private String	targetPort;
	private boolean	enable	= true;
}