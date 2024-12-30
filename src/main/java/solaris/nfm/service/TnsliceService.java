package solaris.nfm.service;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import solaris.nfm.controller.dto.OpenflowMeterInputDto;
import solaris.nfm.controller.dto.SwitchNodeDto;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.openflow.tnslice.TnSlice;

@Service
@Slf4j
public class TnsliceService {
    @Autowired
    private OpenflowService openflowService;
    @Autowired
    private FlowBodyGenService openflowBodyGenService;

    public void putMeter(String nodeId, OpenflowMeterInputDto dto) throws ExceptionBase {
        JsonNode content = openflowBodyGenService.getMeterTableBody(dto);
        openflowService.putMeterTable(nodeId,
                content.path("flow-node-inventory:meter").path("meter-id").asText(),
                content);
    }

    public void removeFlows(TnSlice dto, String srcMac) {
        JsonNode content;
        if (dto.getHeadNode().isEnable()) {

            content = openflowBodyGenService.getHeadPopVlanDownBody(dto, srcMac, null, null);
            try {
                openflowService.deleteFlow(dto.getHeadNode().getNodeId(), 0L,
                        content.path("flow-node-inventory:flow").path("id").asText());
            } catch (ExceptionBase ignored) {
            }

            content = openflowBodyGenService.getHeadPushVlanUpBody(dto, srcMac, null, null);
            try {
                openflowService.deleteFlow(dto.getHeadNode().getNodeId(), 0L,
                        content.path("flow-node-inventory:flow").path("id").asText());
            } catch (ExceptionBase ignored) {
            }
        }

        for (SwitchNodeDto middleNode : dto.getMiddleNodes()) {
            if (!middleNode.isEnable()) {
                continue;
            }
            content = openflowBodyGenService.getMiddleDownBody(dto, middleNode, srcMac, null, null);
            try {
                openflowService.deleteFlow(middleNode.getNodeId(), 0L,
                        content.path("flow-node-inventory:flow").path("id").asText());
            } catch (ExceptionBase ignored) {
            }

            content = openflowBodyGenService.getMiddleUpBody(dto, middleNode, srcMac, null, null);
            try {
                openflowService.deleteFlow(middleNode.getNodeId(), 0L,
                        content.path("flow-node-inventory:flow").path("id").asText());
            } catch (ExceptionBase ignored) {
            }
        }

        if (dto.getTailNode().isEnable()) {
            content = openflowBodyGenService.getTailPushVlanDownBody(dto, srcMac, null, null);
            try {
                openflowService.deleteFlow(dto.getTailNode().getNodeId(), 0L,
                        content.path("flow-node-inventory:flow").path("id").asText());
            } catch (ExceptionBase ignored) {
            }

            content = openflowBodyGenService.getTailPopVlanUpBody(dto, srcMac, null, null);
            try {
                openflowService.deleteFlow(dto.getTailNode().getNodeId(), 0L,
                        content.path("flow-node-inventory:flow").path("id").asText());
            } catch (ExceptionBase ignored) {
            }
        }
    }

    public void putFlows(TnSlice dto, String srcMac) throws ExceptionBase {
        JsonNode content;
        Long meterId;
        Integer upQueueId = openflowService.findQueue(dto.getUplinkMinBitrate());
        Integer downQueueId = openflowService.findQueue(dto.getDownlinkMinBitrate());

        JsonNode flooding = openflowBodyGenService.getFloodingBody();

        if (dto.getHeadNode().isEnable()) {
            openflowService.putFlowTable(dto.getHeadNode().getNodeId(), 0L,
                    flooding.path("flow-node-inventory:flow").path("id").asText(), flooding);

            meterId = openflowService.findMeter(dto.getHeadNode().getNodeId(), dto.getDownlinkMaxBitrate());
            content = openflowBodyGenService.getHeadPopVlanDownBody(dto, srcMac, meterId, downQueueId);
            openflowService.putFlowTable(dto.getHeadNode().getNodeId(), 0L,
                    content.path("flow-node-inventory:flow").path("id").asText(), content);

            meterId = openflowService.findMeter(dto.getHeadNode().getNodeId(), dto.getUplinkMaxBitrate());
            content = openflowBodyGenService.getHeadPushVlanUpBody(dto, srcMac, meterId, upQueueId);
            openflowService.putFlowTable(dto.getHeadNode().getNodeId(), 0L,
                    content.path("flow-node-inventory:flow").path("id").asText(), content);

        }

        for (SwitchNodeDto middleNode : dto.getMiddleNodes()) {
            if (!middleNode.isEnable()) {
                continue;
            }
            openflowService.putFlowTable(middleNode.getNodeId(), 0L,
                    flooding.path("flow-node-inventory:flow").path("id").asText(), flooding);

            meterId = openflowService.findMeter(middleNode.getNodeId(), dto.getDownlinkMaxBitrate());
            content = openflowBodyGenService.getMiddleDownBody(dto, middleNode, srcMac, meterId, downQueueId);
            openflowService.putFlowTable(middleNode.getNodeId(), 0L,
                    content.path("flow-node-inventory:flow").path("id").asText(), content);

            meterId = openflowService.findMeter(middleNode.getNodeId(), dto.getUplinkMaxBitrate());
            content = openflowBodyGenService.getMiddleUpBody(dto, middleNode, srcMac, meterId, upQueueId);
            openflowService.putFlowTable(middleNode.getNodeId(), 0L,
                    content.path("flow-node-inventory:flow").path("id").asText(), content);
        }

        if (dto.getTailNode().isEnable()) {
            openflowService.putFlowTable(dto.getTailNode().getNodeId(), 0L,
                    flooding.path("flow-node-inventory:flow").path("id").asText(), flooding);

            meterId = openflowService.findMeter(dto.getTailNode().getNodeId(), dto.getDownlinkMaxBitrate());
            content = openflowBodyGenService.getTailPushVlanDownBody(dto, srcMac, meterId, downQueueId);
            openflowService.putFlowTable(dto.getTailNode().getNodeId(), 0L,
                    content.path("flow-node-inventory:flow").path("id").asText(), content);

            meterId = openflowService.findMeter(dto.getTailNode().getNodeId(), dto.getUplinkMaxBitrate());
            content = openflowBodyGenService.getTailPopVlanUpBody(dto, srcMac, meterId, upQueueId);
            openflowService.putFlowTable(dto.getTailNode().getNodeId(), 0L,
                    content.path("flow-node-inventory:flow").path("id").asText(), content);
        }
    }
}
