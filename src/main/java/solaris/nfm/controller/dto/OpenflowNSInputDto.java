package solaris.nfm.controller.dto;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OpenflowNSInputDto {
    private int vlanId;
    private int pcp;
    private SwitchNodeDto headNode;
    private SwitchNodeDto tailNode;
    private List<SwitchNodeDto> middleNodes = new ArrayList<>();
    private String srcMac;
    private String destMac;

    private Long uploadMaxBandwidth;
    private Long downloadMaxBandwidth;
    private Long uploadMinBandwidth;
    private Long downloadMinBandwidth;

    public void setFlowLimit(LimitInputDto limitInputDto) {
        this.setFlowLimit(
                limitInputDto.getUploadMaxBandwidth(),
                limitInputDto.getDownloadMaxBandwidth(),
                limitInputDto.getUploadMinBandwidth(),
                limitInputDto.getDownloadMinBandwidth()
        );
    }

    public void setFlowLimit(Long uploadMaxBandwidth, Long downloadMaxBandwidth,
                             Long uploadMinBandwidth, Long downloadMinBandwidth) {
        this.uploadMaxBandwidth = uploadMaxBandwidth;
        this.downloadMaxBandwidth = downloadMaxBandwidth;
        this.uploadMinBandwidth = uploadMinBandwidth;
        this.downloadMinBandwidth = downloadMinBandwidth;
    }

    public void clearLimit() {
        this.uploadMaxBandwidth = null;
        this.downloadMaxBandwidth = null;
        this.uploadMinBandwidth = null;
        this.downloadMinBandwidth = null;
    }
}
