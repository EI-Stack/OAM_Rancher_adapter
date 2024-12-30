package solaris.nfm.capability.system.json.util;

import org.owasp.encoder.Encode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonNodeUtil
{
	/**
	 * 為了避免 code injection，所以
	 * 將傳入的 JsonNode，以遞迴的方式，進行 sanitize (Encode.forHtml)
	 */
	public static JsonNode sanitize(final JsonNode jsonNode)
	{
		if (jsonNode.isObject())
		{
			jsonNode.fields().forEachRemaining(entry ->
			{
				if (entry.getValue().isTextual())
				{
					final String textSanitized = Encode.forHtml(entry.getValue().textValue());
					entry.setValue(new TextNode(textSanitized));
				} else if (entry.getValue().isContainerNode())
				{
					JsonNodeUtil.sanitize(entry.getValue());
				}
			});
		} else if (jsonNode.isArray())
		{
			final ArrayNode arrayNode = (ArrayNode) jsonNode;
			int count = 0;
			for (final JsonNode node : arrayNode)
			{
				if (node.isValueNode() && node.isTextual())
				{
					final String textSanitized = Encode.forHtml(node.asText());
					arrayNode.set(count, new TextNode(textSanitized));
				} else if (node.isContainerNode())
				{
					JsonNodeUtil.sanitize(node);
				}
				count++;
			}
		}
		return jsonNode;
	}

	/**
	 * 將傳入的 JsonNode，以遞迴的方式，去除字串類型 node 的前後空格 (包含 array，例如 ["a", "b"])
	 */
	public static void trimStringField(final JsonNode jsonNode)
	{
		if (jsonNode.isObject())
		{
			jsonNode.fields().forEachRemaining(entry ->
			{
				if (entry.getValue().isTextual())
				{
					final String textTrimmed = entry.getValue().textValue().strip();
					entry.setValue(new TextNode(textTrimmed));
				} else if (entry.getValue().isContainerNode())
				{
					JsonNodeUtil.trimStringField(entry.getValue());
				}
			});
		} else if (jsonNode.isArray())
		{
			final ArrayNode arrayNode = (ArrayNode) jsonNode;
			int count = 0;
			for (final JsonNode node : arrayNode)
			{
				if (node.isValueNode() && node.isTextual())
				{
					final String textTrimmed = node.asText().strip();
					arrayNode.set(count, new TextNode(textTrimmed));
				} else if (node.isContainerNode())
				{
					JsonNodeUtil.trimStringField(node);
				}
				count++;
			}
		}
	}

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
	 * 在 ArrayNode 中尋找某個特定欄位的值，當找到第一筆就會中止迴圈，然後傳回
	 */
	public static JsonNode findArrayMember(final ArrayNode arrayNode, final String key, final String value)
	{
		JsonNode result = null;

		for (final JsonNode jsonNode : arrayNode)
		{
			final String tmpValue = jsonNode.path(key).asText();
			if (value.equals(tmpValue))
			{
				result = jsonNode;
				break;
			}
		}
		return result;
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