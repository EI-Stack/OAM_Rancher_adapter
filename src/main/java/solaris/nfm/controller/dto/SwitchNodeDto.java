package solaris.nfm.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchNodeDto implements Serializable {
    @NotNull
    private String nodeId;
    @NotNull
    private String sourcePort;
    @NotNull
    private String targetPort;
    private boolean enable = true;
}