package solaris.nfm.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import solaris.nfm.controller.dto.OpenflowMeterInputDto;
import solaris.nfm.controller.dto.SwitchNodeDto;
import solaris.nfm.model.resource.openflow.tnslice.TnSlice;
import solaris.nfm.util.OpenflowOptionInfo;

@Service
@Slf4j
public class FlowBodyGenService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OpenflowOptionInfo openflowOptionInfo;

    private Long priority = 1L;
    private final String namePrefix = "networkSlice-";

    public JsonNode getFloodingBody() {
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = "flooding";
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", 1000)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        match.putObject("ethernet-match").putObject("ethernet-destination").put("address", "ff:ff:ff:ff:ff:ff");
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        ArrayNode action = instruction.addObject().put("order", 0)
                .putObject("apply-actions")
                .putArray("action");
        action.addObject()
                .put("order", 0)
                .putObject("output-action")
                .put("output-node-connector", "FLOOD")
                .put("max-length", 65509);

        return content;
    }

    public JsonNode getMeterTableBody(OpenflowMeterInputDto dto) {
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode meter = content.putObject("flow-node-inventory:meter");
        meter.put("meter-id", dto.getMeterId())
                .put("meter-name", dto.getMeterId())
                .put("flags", "meter-kbps");
        ArrayNode meterBands = meter.putObject("meter-band-headers").putArray("meter-band-header");
        ObjectNode band = meterBands.addObject();
        band.put("band-id", 0)
                .put("drop-rate", dto.getDropRate())
                .put("drop-burst-size", 0)
                .putObject("meter-band-types")
                .put("flags", "ofpmbt-drop");
        return content;
    }

    public JsonNode getMiddleUpBody(TnSlice dto, SwitchNodeDto node, String srcMac,
                                    Long meterId, Integer queueId) {
        int insOrder = 0;
        int actOrder = 0;
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = namePrefix + "-middle-dest-" + dto.getDestMac();
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", priority++)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        if (openflowOptionInfo.isVlanMatch()) {
            ObjectNode vlanMatch = match.putObject("vlan-match").putObject("vlan-id");
            vlanMatch.put("vlan-id-present", true);
            vlanMatch.put("vlan-id", dto.getVlanId());
        }
        ObjectNode ethMatch = match.putObject("ethernet-match");
        ethMatch.putObject("ethernet-destination").put("address", dto.getDestMac());
        ethMatch.putObject("ethernet-source").put("address", srcMac);
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        if (meterId != null) {
            instruction.add(getMeterInstruction(++insOrder, meterId));
        }
        ObjectNode ins = instruction.addObject();
        ins.put("order", ++insOrder);
        ArrayNode actions = ins.putObject("apply-actions").putArray("action");
        if (queueId != null) {
            actions.add(getQueueAction(++actOrder, queueId));
        }
        ObjectNode output = actions.addObject();
        output.put("order", ++actOrder).putObject("output-action")
                .put("output-node-connector", node.getTargetPort())
                .put("max-length", 65509);

        return content;
    }

    public JsonNode getMiddleDownBody(TnSlice dto, SwitchNodeDto node, String srcMac,
                                      Long meterId, Integer queueId) {
        int insOrder = 0;
        int actOrder = 0;
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = namePrefix + "-middle-dest-" + srcMac;
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", priority++)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        match.putObject("ethernet-match").putObject("ethernet-destination").put("address", srcMac);
        if (openflowOptionInfo.isVlanMatch()) {
            ObjectNode vlanMatch = match.putObject("vlan-match").putObject("vlan-id");
            vlanMatch.put("vlan-id-present", true);
            vlanMatch.put("vlan-id", dto.getVlanId());
        }
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        if (meterId != null) {
            instruction.add(getMeterInstruction(++insOrder, meterId));
        }
        ObjectNode ins = instruction.addObject();
        ins.put("order", ++insOrder);
        ArrayNode actions = ins.putObject("apply-actions").putArray("action");
        if (queueId != null) {
            actions.add(getQueueAction(++actOrder, queueId));
        }
        ObjectNode output = actions.addObject();
        output.put("order", ++actOrder).putObject("output-action")
                .put("output-node-connector", node.getSourcePort())
                .put("max-length", 65509);

        return content;
    }

    public JsonNode getTailPopVlanUpBody(TnSlice dto, String srcMac,
                                         Long meterId, Integer queueId) {
        int insOrder = 0;
        int actOrder = 0;
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = namePrefix + "-tail-pop-dest-" + dto.getDestMac();
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", priority++)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        if (openflowOptionInfo.isVlanMatch()) {
            ObjectNode vlanMatch = match.putObject("vlan-match").putObject("vlan-id");
            vlanMatch.put("vlan-id-present", true);
            vlanMatch.put("vlan-id", dto.getVlanId());
        }
        ObjectNode ethMatch = match.putObject("ethernet-match");
        ethMatch.putObject("ethernet-destination").put("address", dto.getDestMac());
        ethMatch.putObject("ethernet-source").put("address", srcMac);
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        if (meterId != null) {
            instruction.add(getMeterInstruction(++insOrder, meterId));
        }
        ObjectNode vlanIns = instruction.addObject();
        vlanIns.put("order", ++insOrder);
        ArrayNode actions = vlanIns.putObject("apply-actions").putArray("action");
        if (queueId != null) {
            actions.add(getQueueAction(++actOrder, queueId));
        }
        ObjectNode popVlan = actions.addObject();
        popVlan.put("order", ++actOrder)
                .putObject("pop-vlan-action");
        ObjectNode output = actions.addObject();
        output.put("order", ++actOrder)
                .putObject("output-action").put("output-node-connector",
                        dto.getTailNode().getTargetPort())
                .put("max-length", 65509);
        return content;
    }

    public JsonNode getHeadPopVlanDownBody(TnSlice dto, String srcMac,
                                           Long meterId, Integer queueId) {
        int insOrder = 0;
        int actOrder = 0;
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = namePrefix + "-head-pop-dest-" + srcMac;
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", priority++)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        if (openflowOptionInfo.isVlanMatch()) {
            ObjectNode vlanMatch = match.putObject("vlan-match").putObject("vlan-id");
            vlanMatch.put("vlan-id-present", true);
            vlanMatch.put("vlan-id", dto.getVlanId());
        }
        match.putObject("ethernet-match").putObject("ethernet-destination").put("address", srcMac);
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        if (meterId != null) {
            instruction.add(getMeterInstruction(++insOrder, meterId));
        }
        ObjectNode vlanIns = instruction.addObject();
        vlanIns.put("order", ++insOrder);
        ArrayNode actions = vlanIns.putObject("apply-actions").putArray("action");
        if (queueId != null) {
            actions.add(getQueueAction(++actOrder, queueId));
        }
        ObjectNode popVlan = actions.addObject();
        popVlan.put("order", ++actOrder)
                .putObject("pop-vlan-action");

        ObjectNode output = actions.addObject();
        output.put("order", ++actOrder)
                .putObject("output-action").put("output-node-connector",
                        dto.getHeadNode().getSourcePort())
                .put("max-length", 65509);
        return content;
    }

    public JsonNode getHeadPushVlanUpBody(TnSlice dto, String srcMac,
                                          Long meterId, Integer queueId) {
        int insOrder = 0;
        int actOrder = 0;
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = namePrefix + "-head-push-dest-" + dto.getDestMac();
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", priority++)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        ObjectNode ethMatch = match.putObject("ethernet-match");
        ethMatch.putObject("ethernet-destination").put("address", dto.getDestMac());
        ethMatch.putObject("ethernet-source").put("address", srcMac);
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        if (meterId != null) {
            instruction.add(getMeterInstruction(++insOrder, meterId));
        }
        ObjectNode vlanIns = instruction.addObject();
        vlanIns.put("order", ++insOrder);
        ArrayNode actions = vlanIns.putObject("apply-actions").putArray("action");
        ObjectNode pushVlan = actions.addObject();
        if (queueId != null) {
            actions.add(getQueueAction(++actOrder, queueId));
        }
        pushVlan.put("order", ++actOrder).putObject("push-vlan-action").put("ethernet-type", 33024);
        ObjectNode setVlan = actions.addObject();
        setVlan.put("order", ++actOrder).putObject("set-field")
                .putObject("vlan-match").putObject("vlan-id")
                .put("vlan-id", dto.getVlanId()).put("vlan-id-present", true);
        ObjectNode setVlanPcp = actions.addObject();
        setVlanPcp.put("order", ++actOrder).putObject("set-field")
                .putObject("vlan-match").put("vlan-pcp", dto.getPcp());
        ObjectNode output = actions.addObject();
        output.put("order", ++actOrder).putObject("output-action")
                .put("output-node-connector", dto.getHeadNode().getTargetPort())
                .put("max-length", 65509);
        return content;
    }

    public JsonNode getTailPushVlanDownBody(TnSlice dto, String srcMac,
                                            Long meterId, Integer queueId) {
        int insOrder = 0;
        int actOrder = 0;
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode flow = content.putObject("flow-node-inventory:flow");
        String flowId = namePrefix + "-tail-push-dest-" + srcMac;
        flow.put("id", flowId)
                .put("table_id", 0)
                .put("hard-timeout", 0)
                .put("idle-timeout", 0)
                .put("priority", priority++)
                .put("cookie", 1)
                .put("strict", false)
                .put("barrier", false)
                .put("installHw", false)
                .put("flow-name", flowId);
        ObjectNode match = flow.putObject("match");
        match.putObject("ethernet-match").putObject("ethernet-destination").put("address", srcMac);
        ArrayNode instruction = flow.putObject("instructions").putArray("instruction");
        if (meterId != null) {
            instruction.add(getMeterInstruction(++insOrder, meterId));
        }
        ObjectNode vlanIns = instruction.addObject();
        vlanIns.put("order", ++insOrder);
        ArrayNode actions = vlanIns.putObject("apply-actions").putArray("action");
        if (queueId != null) {
            actions.add(getQueueAction(++actOrder, queueId));
        }
        ObjectNode pushVlanAct = actions.addObject();
        pushVlanAct.put("order", ++actOrder).putObject("push-vlan-action").put("ethernet-type", 33024);
        ObjectNode setVlan = actions.addObject();
        setVlan.put("order", ++actOrder).putObject("set-field")
                .putObject("vlan-match").putObject("vlan-id")
                .put("vlan-id", dto.getVlanId()).put("vlan-id-present", true);
        ObjectNode setVlanPcp = actions.addObject();
        setVlanPcp.put("order", ++actOrder).putObject("set-field")
                .putObject("vlan-match").put("vlan-pcp", dto.getPcp());
        ObjectNode output = actions.addObject();
        output.put("order", ++actOrder).putObject("output-action")
                .put("output-node-connector", dto.getTailNode().getSourcePort())
                .put("max-length", 65509);
        return content;
    }

    private JsonNode getMeterInstruction(int order, Long meterId) {
        ObjectNode instruction = objectMapper.createObjectNode();
        instruction.put("order", order);
        instruction.putObject("meter").put("meter-id", meterId);
        return instruction;
    }

    private JsonNode getQueueAction(int order, int queueId) {
        ObjectNode action = objectMapper.createObjectNode();
        action.put("order", order)
                .putObject("set-queue-action")
                .put("queue", queueId)
                .put("queue-id", queueId);
        return action;
    }
}
