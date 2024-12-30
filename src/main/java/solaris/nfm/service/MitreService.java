package solaris.nfm.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.JsonService;

@Service
@Slf4j
public class MitreService
{
	private JsonNode		fight;
	private ArrayNode		tactics;
	private ArrayNode		techniques;

	@Autowired
	private JsonService		jsonService;
	@Autowired
	private ObjectMapper	objectMapper;

	@PostConstruct
	public void init()
	{
		try
		{
			fight = jsonService.getJsonNodeFromClasspathForYaml("mitre/fight.yaml");
		} catch (final IOException e)
		{
			log.error("Can not read file mitre/fight.yaml");
		}
		tactics = (ArrayNode) fight.path("tactics");
		techniques = (ArrayNode) fight.path("techniques");
	}

	public JsonNode getFight()
	{
		return this.fight;
	}


	public ArrayNode getMitigationsByTechniqueId(final String techniqueId)
	{
		ArrayNode resultMitigations = JsonNodeFactory.instance.arrayNode();
		for (final JsonNode technique : techniques)
		{
			if (technique.path("id").asText().equalsIgnoreCase(techniqueId))
			{
				resultMitigations = (ArrayNode) technique.path("mitigations");
				break;
			}
		}
		return resultMitigations;
	}

	public String getTacticNameById(final String tacticId)
	{
		String tacticName = null;
		for (final JsonNode tactic : tactics)
		{
			if (tactic.path("id").asText().equalsIgnoreCase(tacticId)) tacticName = tactic.path("name").asText();
			break;
		}

		return tacticName;
	}

	public String getTacticIdByName(final String tacticName)
	{
		String tacticId = null;
		for (final JsonNode tactic : tactics)
		{
			if (tactic.path("name").asText().equalsIgnoreCase(tacticName)) tacticId = tactic.path("id").asText();
			break;
		}

		return tacticId;
	}

	public ArrayNode getTechniquesByTacticId(final String tacticId) throws IOException
	{
		final ArrayNode resultTactics = JsonNodeFactory.instance.arrayNode();

		for (final JsonNode technique : techniques)
		{
			final Set<String> tacticIdSet = objectMapper.readValue(technique.path("tactics").traverse(), new TypeReference<LinkedHashSet<String>>()
			{});
			if (tacticIdSet.contains(tacticId)) resultTactics.add(technique);
		}

		return resultTactics;
	}
}