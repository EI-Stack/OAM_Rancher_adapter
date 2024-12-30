package solaris.nfm.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.controller.dto.OdlResponseDto;
import solaris.nfm.controller.dto.OpenflowMeterInputDto;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.util.NamingUtil;

@Service
@Slf4j
public class OpenflowService
{
	@Autowired
	private OdlReqService		odlService;
	@Autowired
	private FlowBodyGenService	openflowBodyGenService;
	@Autowired
	private ObjectMapper		objectMapper;

	public JsonNode getTopology() throws ExceptionBase
	{
		final String path = "/restconf/operational/network-topology:network-topology/topology/flow%3A1";
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		final ArrayNode arrayNode = (ArrayNode) responseDto.getContent().path("topology");
		return clearTopology(arrayNode);
	}

	private JsonNode clearTopology(final ArrayNode arrayNode)
	{
		ObjectNode openflowTopology = objectMapper.createObjectNode();
		for (final JsonNode jsonNode : arrayNode)
		{
			if (jsonNode.path("topologyId").asText().equals("flow:1"))
			{
				openflowTopology = (ObjectNode) jsonNode;
			}
		}
		if (openflowTopology.isEmpty() || openflowTopology.path("node").isMissingNode())
		{
			return openflowTopology;
		}
		final ArrayNode nodes = (ArrayNode) openflowTopology.path("node");
		if (nodes.size() > 0)
		{
			for (int i = 0; i < nodes.size(); i++)
			{
				final ObjectNode node = (ObjectNode) nodes.get(i);
				node.remove("opendaylightTopologyInventory:inventoryNodeRef");
				Integer localIdx = null;
				final ArrayNode ports = (ArrayNode) node.path("terminationPoint");
				for (int j = 0; j < ports.size(); j++)
				{
					final ObjectNode port = (ObjectNode) ports.get(j);
					if (port.path("tpId").asText().endsWith("LOCAL"))
					{
						localIdx = j;
						continue;
					}
					port.remove("opendaylightTopologyInventory:inventoryNodeConnectorRef");
				}
				if (localIdx != null)
				{
					ports.remove(localIdx);
				}
			}
		}
		openflowTopology.remove("topologyId");
		return NamingUtil.removeNamespaceInKey(openflowTopology);
	}

	public JsonNode getInventory(final String node) throws ExceptionBase
	{
		final String path = "/restconf/operational/opendaylight-inventory:nodes/node/" + node;
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		return clearInventory(NamingUtil.removeNamespaceInKey(responseDto.getContent()));
	}

	private JsonNode clearInventory(final JsonNode inventory)
	{
		final ArrayNode nodes = (ArrayNode) inventory.path("node");
		if (nodes.isEmpty())
		{
			return inventory;
		}
		for (final JsonNode node : nodes)
		{
			final ObjectNode objectNode = (ObjectNode) node;
			objectNode.remove("groupFeatures");
			objectNode.remove("switchFeatures");
			objectNode.remove("snapshotGatheringStatusStart");
			objectNode.remove("snapshotGatheringStatusEnd");
		}
		return inventory;
	}

	public JsonNode getFlowTableById(final String node, final Long tableId) throws ExceptionBase
	{
		final String path = "/restconf/operational/opendaylight-inventory:nodes/node/" + node + "/flow-node-inventory:table/" + tableId;
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		return responseDto.getContent();
	}

	public JsonNode getFlowTableOverview(final String node) throws ExceptionBase
	{
		final JsonNode jsonNode = getInventory(node);
		if (jsonNode.path("node").path(0).path("table").isMissingNode())
		{
			return objectMapper.createArrayNode();
		}
		return jsonNode.path("node").path(0).path("table");
	}

	private JsonNode clearFlowTable(final JsonNode jsonNode)
	{
		if (jsonNode.path("table").isMissingNode())
		{
			return objectMapper.createArrayNode();
		}
		final ArrayNode arrayNode = (ArrayNode) jsonNode.path("table");
		final ArrayNode tableNodes = objectMapper.createArrayNode();
		for (final JsonNode tableNode : arrayNode)
		{
			if (tableNode.path("flow").isMissingNode())
			{
				continue;
			}
			tableNodes.add(tableNode);
		}
		return tableNodes;
	}

	public JsonNode getMeterTableById(final String node, final Long meterId) throws ExceptionBase
	{
		final String path = "/restconf/operational/opendaylight-inventory:nodes/node/" + node + "/flow-node-inventory:meter/" + meterId;
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		return responseDto.getContent();
	}

	public JsonNode getMeterTableOverview(final String node) throws ExceptionBase
	{
		final JsonNode jsonNode = getInventory(node);
		if (jsonNode.path("node").path(0).path("meter").isMissingNode())
		{
			return objectMapper.createArrayNode();
		}
		return jsonNode.path("node").path(0).path("meter");
	}

	public JsonNode getNodePort(final String node, final String port) throws ExceptionBase
	{
		final String path = "/restconf/operational/opendaylight-inventory:nodes/node/" + node + "/node-connector/" + port;
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		return responseDto.getContent();
	}

	public JsonNode getNodePortQueueById(final String node, final String port, final String queueId) throws ExceptionBase
	{
		final String path = "/restconf/operational/opendaylight-inventory:nodes/node/" + node + "/node-connector/" + node + ":" + port + "/flow-node-inventory:queue/" + queueId;
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		return responseDto.getContent();
	}

	public JsonNode getPortInfo(final String node, final String port) throws ExceptionBase
	{
		final String path = "/restconf/operational/opendaylight-inventory:nodes/node/" + node + "/node-connector/" + node + ":" + port;
		final OdlResponseDto responseDto = odlService.get(path);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
		return responseDto.getContent();
	}

	public JsonNode getQueueOverview(final String node, final String port) throws ExceptionBase
	{
		return getPortInfo(node, port).path("flowNodeInventory:queue");
	}

	public void putFlowTable(final String node, final Long tableId, final String flowId, final JsonNode content) throws ExceptionBase
	{
		final String path = "/restconf/config/opendaylight-inventory:nodes/node/" + node + "/flow-node-inventory:table/" + tableId + "/flow/" + flowId;
		final OdlResponseDto responseDto = odlService.put(path, content);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
	}

	public void putMeterTable(final String node, final String meterId, final JsonNode content) throws ExceptionBase
	{
		final String path = "/restconf/config/opendaylight-inventory:nodes/node/" + node + "/flow-node-inventory:meter/" + meterId;
		final OdlResponseDto responseDto = odlService.put(path, content);
		if (!responseDto.isSuccess())
		{
			throw new ExceptionBase(responseDto.getErrorCode(), responseDto.getErrorMessage());
		}
	}

	public void deleteFlow(final String nodeId, final Long tableId, final String flowId) throws ExceptionBase
	{
		final String path = "/restconf/config/opendaylight-inventory:nodes/node/" + nodeId + "/flow-node-inventory:table/" + tableId + "/flow-node-inventory:flow/" + flowId;
		final OdlResponseDto odlResponseDto = odlService.delete(path);
		if (!odlResponseDto.isSuccess())
		{
			throw new ExceptionBase(odlResponseDto.getErrorCode(), odlResponseDto.getErrorMessage());
		}
	}

	public void deleteMeter(final String nodeId, final String meterId) throws ExceptionBase
	{
		final String path = "/restconf/config/opendaylight-inventory:nodes/node/" + nodeId + "/flow-node-inventory:meter/" + meterId;
		final OdlResponseDto odlResponseDto = odlService.delete(path);
		if (!odlResponseDto.isSuccess())
		{
			throw new ExceptionBase(odlResponseDto.getErrorCode(), odlResponseDto.getErrorMessage());
		}
	}

	public Long findMeter(final String nodeId, final Long dropRate) throws ExceptionBase
	{
		if (dropRate == null || nodeId == null || dropRate <= 0)
		{
			return null;
		}
		Long meterId = 0L;
		final JsonNode meterArray = getMeterTableOverview(nodeId);
		if (!meterArray.isMissingNode())
		{
			for (final JsonNode jsonNode : meterArray)
			{
				if (jsonNode.path("meterId").asLong() > meterId)
				{
					meterId = jsonNode.path("meterId").asLong();
				}
				if (jsonNode.path("meterBandHeaders").path("meterBandHeader").path(0).path("dropRate").asLong() == dropRate)
				{
					return jsonNode.path("meterId").asLong();
				}
			}
		}
		final OpenflowMeterInputDto meterInputDto = new OpenflowMeterInputDto();
		meterInputDto.setMeterId(meterId + 1);
		meterInputDto.setDropRate(dropRate);
		final JsonNode content = openflowBodyGenService.getMeterTableBody(meterInputDto);
		putMeterTable(nodeId, content.path("flow-node-inventory:meter").path("meter-id").asText(), content);

		log.info("findMeter: meterId = " + meterInputDto.getMeterId() + " dropRate = " + dropRate);
		return meterInputDto.getMeterId();
	}

	/**
	 * 定義 switch queue 的編號與對應的速率
	 */
	private final Map<Integer, Long> queueMap = Map.of(0, 50L, 1, 100L, 2, 200L, 3, 300L, 4, 400L, 5, 500L, 6, 600L, 7, 700L);

	public Integer findQueue(final Long minRate)
	{
		if (minRate == null || minRate <= 0L)
		{
			return null;
		}
		int queueId = 0;
		for (final Map.Entry<Integer, Long> entry : queueMap.entrySet())
		{
			final Integer id = entry.getKey();
			final Long value = entry.getValue();
			if (minRate < value)
			{
				break;
			}
			queueId = id;
		}
		return queueId;
	}
}
