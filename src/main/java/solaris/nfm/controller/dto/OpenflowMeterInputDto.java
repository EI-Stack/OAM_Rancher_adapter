package solaris.nfm.controller.dto;

import lombok.Data;

@Data
public class OpenflowMeterInputDto {
    private Long meterId;
    private Long dropRate;
}
