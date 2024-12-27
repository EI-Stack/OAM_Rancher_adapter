package solaris.nfm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonNodeUtil
{
	public static String prettyPrint(final JsonNode jsonNode)
	{
		try
		{
			final ObjectMapper mapper = new ObjectMapper();
			final Object json = mapper.readValue(jsonNode.toString(), Object.class);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
		} catch (final Exception e)
		{
			return "Sorry, pretty print didn't work";
		}
	}

	/**
	 * ODL 會將只有 1 個成員的 array 自動轉成 object，所以需要能自動判斷此種處理，避免強制轉型失敗
	 */
	public static ArrayNode castToArrayNode(final JsonNode target)
	{
		ArrayNode arrayNode = null;
		try
		{
			if (target.isObject() == true)
			{
				arrayNode = JsonNodeFactory.instance.arrayNode();
				arrayNode.add(target);
			} else
			{
				arrayNode = (ArrayNode) target;
			}
		} catch (final Exception e)
		{
			log.error("\t Casting json format failed. json={}", target.toPrettyString());
			throw e;
		}

		return arrayNode;
	}
}