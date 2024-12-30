package solaris.nfm.capability.annotation.jsonvalid;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.swagger.v3.core.util.Yaml;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JsonService
{
	@Autowired
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ===[ JsonNode ]================================================================================================= //

	/**
	 * 讀取 OpenApi/YAML 格式內容的檔案，然後轉成 JsonNode
	 */
	public JsonNode getJsonNodeFromClasspathForYaml(final String yamlFileName) throws IOException
	{
		final String filePath = yamlFileName;
		final Resource resource = new ClassPathResource(filePath);
		final String yamlContent = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
		final JsonNode jsonNode = Yaml.mapper().readValue(yamlContent, JsonNode.class);

		return jsonNode;
	}

	/**
	 * 讀取 JSON 格式內容的檔案，然後轉成 JsonNode
	 */
	public JsonNode getJsonNodeFromClasspath(final String fileName) throws IOException
	{
		final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		return objectMapper.readTree(is);
	}

	public JsonNode getJsonNodeFromStringContent(final String content) throws IOException
	{
		return objectMapper.readTree(content);
	}

	public JsonNode getJsonNodeFromUrl(final String url) throws IOException
	{
		return objectMapper.readTree(new URL(url));
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