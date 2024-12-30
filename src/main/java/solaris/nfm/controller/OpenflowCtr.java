package solaris.nfm.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import solaris.nfm.controller.dto.OpenflowMeterInputDto;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.service.OpenflowService;
import solaris.nfm.service.TnsliceService;

@RestController
@RequestMapping("/v1/openflow")
public class OpenflowCtr
{

	@Autowired
	private OpenflowService				openflowService;
	@Autowired
	private TnsliceService				networkSliceService;
	@Autowired
	private ObjectMapper				objectMapper;

	private final Map<String, String>	postNameMap	= new HashMap<>()
													{
														{
															put("s2-eth1", "基站1");
															put("s2-eth2", "基站2");
															put("s3-eth1", "UPF1");
															put("s3-eth2", "UPF2");
															put("te-1/1/5", "基站");
															put("ethernet1/1/5", "UPF");
														}
													};

	@GetMapping("/topology")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getTopology() throws ExceptionBase
	{
		final ObjectNode topology = (ObjectNode) openflowService.getTopology();
		for (final JsonNode jsonNode : topology.path("node"))
		{
			final ObjectNode objectNode = (ObjectNode) jsonNode;
			final ArrayNode portNodes = (ArrayNode) objectNode.path("terminationPoint");
			final String nodeId = jsonNode.path("nodeId").asText();
			final JsonNode inventory = openflowService.getInventory(nodeId);
			final JsonNode nodeInfo = inventory.path("node").get(0);
			nodeInfo.fieldNames().forEachRemaining(fieldName ->
			{
				if (!fieldName.equals("nodeConnector") && !fieldName.equals("id") && !fieldName.equals("table") && !fieldName.equals("meter"))
				{
					objectNode.set(fieldName, nodeInfo.get(fieldName));
				}
			});
			for (final JsonNode nodeConnector : nodeInfo.path("nodeConnector"))
			{
				for (final JsonNode portNode : portNodes)
				{
					final ObjectNode port = (ObjectNode) portNode;
					if (portNode.path("tpId").asText().equals(nodeConnector.path("id").asText()))
					{
						port.set("state", nodeConnector.path("state"));
						port.set("flowCapableNodeConnectorStatistics", nodeConnector.path("flowCapableNodeConnectorStatistics"));
						port.set("currentSpeed", nodeConnector.path("currentSpeed"));
						port.set("maximumSpeed", nodeConnector.path("maximumSpeed"));
						port.set("name", nodeConnector.path("name"));
						port.put("alias", postNameMap.get(nodeConnector.path("name").asText()));
						port.set("portNumber", nodeConnector.path("portNumber"));
						if (!nodeConnector.path("queue").isMissingNode())
						{
							port.set("queue", nodeConnector.path("queue"));
						}
					}
				}
			}
		}
		return topology;
	}

	@GetMapping("/flow")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getFlow() throws ExceptionBase
	{
		final ObjectNode result = objectMapper.createObjectNode();
		final JsonNode topology = openflowService.getTopology();
		for (final JsonNode jsonNode : topology.path("node"))
		{
			final String nodeId = jsonNode.path("nodeId").asText();

			final ArrayNode tableArray = (ArrayNode) openflowService.getFlowTableOverview(nodeId);
			final ArrayNode nodeTable = objectMapper.createArrayNode();
			for (int i = 0; i < tableArray.size(); i++)
			{
				final ObjectNode tableNode = (ObjectNode) tableArray.get(i);
				if (!tableNode.path("flow").isMissingNode())
				{
					nodeTable.add(tableNode);
				}
			}
			result.set(nodeId, nodeTable);
		}
		return result;
	}

	@GetMapping("/{nodeId}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getNode(@PathVariable("nodeId") final String nodeId) throws ExceptionBase
	{
		return openflowService.getInventory(nodeId);
	}

	@PostMapping("/{nodeId}/meter")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void putMeter(@PathVariable("nodeId") final String nodeId, @RequestBody final OpenflowMeterInputDto inputDto) throws ExceptionBase
	{
		networkSliceService.putMeter(nodeId, inputDto);
	}

	@GetMapping("/{nodeId}/meter")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getMeter(@PathVariable("nodeId") final String nodeId) throws ExceptionBase
	{
		return openflowService.getMeterTableOverview(nodeId);
	}

	@DeleteMapping("/{nodeId}/meter/{meterId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMeter(@PathVariable("nodeId") final String nodeId, @PathVariable("meterId") final String meterId) throws ExceptionBase
	{
		openflowService.deleteMeter(nodeId, meterId);
	}

	@GetMapping("/{nodeId}/flow")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getFlow(@PathVariable("nodeId") final String nodeId) throws ExceptionBase
	{
		final ArrayNode tableArray = (ArrayNode) openflowService.getFlowTableOverview(nodeId);
		final ArrayNode resultArray = objectMapper.createArrayNode();
		for (int i = 0; i < tableArray.size(); i++)
		{
			final ObjectNode tableNode = (ObjectNode) tableArray.get(i);
			if (!tableNode.path("flow").isMissingNode())
			{
				resultArray.add(tableNode);
			}
		}
		return resultArray;
	}
}