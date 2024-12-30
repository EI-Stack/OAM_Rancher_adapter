package solaris.nfm.capability.annotation.jsonvalid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.exception.JsonSchemaNotFoundException;
import solaris.nfm.capability.annotation.jsonvalid.exception.JsonSchemaValidationException;
import solaris.nfm.capability.message.amqp.AmqpService;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean;
import solaris.nfm.capability.message.amqp.AmqpSubService.MessageBean.MessageType;
import solaris.nfm.exception.base.ExceptionBase;

@Service
@Slf4j
public class JsonValidationService
{
	private static SpecVersion.VersionFlag	schemaVersion			= SpecVersion.VersionFlag.V201909;
	private static ObjectMapper				yamlObjMapper			= new ObjectMapper(new YAMLFactory());
	private static String					baseDir					= "json-schema";
	private static String					appName					= "NFM";

	private JsonSchemaFactory				schemaFactoryForJson	= null;
	private JsonSchemaFactory				schemaFactoryForYaml	= null;
	private Map<String, JsonSchema>			jsonSchemaPool			= new HashMap<>();

	@Value("${solaris.json-valid.amqp.enable}")
	private Boolean							jsonValidEnabled;

	@Autowired
	private final ObjectMapper				objectMapper			= new ObjectMapper();
	@Autowired
	private AmqpService						amqpService;

	@PostConstruct
	public void init()
	{
		schemaFactoryForJson = JsonSchemaFactory.getInstance(schemaVersion);
		schemaFactoryForYaml = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(schemaVersion)).objectMapper(yamlObjMapper).build();
	}

	/**
	 * 驗證 JSON 資料正確性
	 * 專供 Controller 調用，能自動設定 JSON Schema 檔案名稱 (yaml)
	 *
	 * @throws URISyntaxException
	 */
	public void validateForCtr(final JsonNode json) throws IOException, ExceptionBase, URISyntaxException
	{
		// StackTraceElement[] 0 是 JsonSchemaService，1 假設是調用的 Controller
		final Integer stackLevel = 1;
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		final String methodName = stackTrace[stackLevel].getMethodName();
		final String simpleClassName = stackTrace[stackLevel].getClassName().split("\\.")[stackTrace[stackLevel].getClassName().split("\\.").length - 1];
		final String schemaFileName = simpleClassName + "-" + methodName + ".yaml";
		validate(schemaFileName, json);
	}

	/**
	 * 驗證 JSON 資料正確性
	 * 專供 AMQP 信封驗證用，若有錯誤，會發訊息至 LogManager
	 *
	 * @throws URISyntaxException
	 */
	public void validateForAmqpEnvelope(final JsonNode json, final String queueName) throws IOException, ExceptionBase, URISyntaxException
	{
		if (jsonValidEnabled == false) return;
		final String schemaFileName = "amqp-" + queueName.split("-")[0] + "-common-envelope.yaml";
		_validateForAmqp(json, queueName, null, schemaFileName);
	}

	/**
	 * 驗證 JSON 資料正確性
	 * 專供 AMQP 驗證用，若有錯誤，會發訊息至 LogManager
	 *
	 * @throws URISyntaxException
	 */
	public void validateForAmqp(final JsonNode json, final String queueName, final MessageType messageType) throws IOException, ExceptionBase, URISyntaxException
	{
		if (jsonValidEnabled == false) return;
		final String schemaFileName = "amqp-" + queueName.split("-")[0] + "-" + messageType.name() + ".yaml";
		_validateForAmqp(json, queueName, messageType, schemaFileName);
	}

	/**
	 * 驗證 JSON 資料正確性
	 * 專供 AMQP 驗證用，若有錯誤，會發訊息至 LogManager
	 *
	 * @throws URISyntaxException
	 */
	public void validateForAmqp(final JsonNode json, final String queueName, final MessageType messageType, final String schemaUriString) throws IOException, ExceptionBase, URISyntaxException
	{
		if (jsonValidEnabled == false) return;
		_validateForAmqp(json, queueName, messageType, schemaUriString);
	}

	/**
	 * 驗證 JSON 資料正確性
	 * 通用，會忽略掉 Json Schema 找不到的錯誤，並且會重組錯誤訊息，讓它比較好讀。但不會發出 AMQP 訊息
	 *
	 * @throws URISyntaxException
	 */
	public void validate(final String schemaUriString, final JsonNode json) throws IOException, ExceptionBase, URISyntaxException
	{
		Set<ValidationMessage> errorSet;
		try
		{
			errorSet = _validate(schemaUriString, json);
		} catch (final JsonSchemaNotFoundException e)
		{
			return;
		}
		if (errorSet.size() == 0) return;

		final List<String> errorMessages = _getValidationErrorMessage(errorSet);
		throw new JsonSchemaValidationException(String.join(", ", errorMessages));
	}

	/**
	 * 在 AMQP 場合，驗證 JSON 資料正確性。會發出 AMQP 訊息通知
	 *
	 * @throws URISyntaxException
	 */
	private void _validateForAmqp(final JsonNode json, final String queueName, final MessageType messageType, final String schemaUriString) throws IOException, ExceptionBase, URISyntaxException
	{
		Set<ValidationMessage> errorSet;
		try
		{
			errorSet = _validate(schemaUriString, json);
		} catch (final JsonSchemaNotFoundException e)
		{
			return;
		}
		if (errorSet.size() == 0) return;

		// create jsonBean
		final MessageBean messageBean = new MessageBean(MessageType.InvalidJsonFormat);
		final ObjectNode content = JsonNodeFactory.instance.objectNode();
		final ArrayNode errors = (ArrayNode) this.objectMapper.valueToTree(errorSet);
		content.put("appName", appName);
		content.put("schemaFile", schemaUriString);
		content.set("json", json);
		content.set("errorMessage", errors);
		content.put("queue", queueName);
		content.put("messageType", (messageType == null) ? "" : messageType.name());
		messageBean.setContent(content);
		log.debug("\t InvalidJsonFormat messageBean={}", objectMapper.valueToTree(messageBean).toPrettyString());
		// send AMQP to LM
		this.amqpService.sendMsgToLm(messageBean);

		final List<String> errorMessages = _getValidationErrorMessage(errorSet);
		throw new JsonSchemaValidationException(String.join(", ", errorMessages));
	}

	/**
	 * 驗證 JSON 資料正確性
	 *
	 * @throws URISyntaxException
	 */
	private Set<ValidationMessage> _validate(final String schemaUriString, final JsonNode json) throws IOException, JsonSchemaNotFoundException, URISyntaxException
	{
		final JsonSchema jsonSchema = getJsonSchema(schemaUriString);
		return jsonSchema.validate(json);
	}

	private List<String> _getValidationErrorMessage(final Set<ValidationMessage> errorSet) throws IOException
	{
		// log.debug("errorSet=\n{}", objectMapper.valueToTree(errorSet).toPrettyString());
		final List<String> errorMessages = new ArrayList<>();
		Integer count = 1;
		for (final ValidationMessage error : errorSet)
		{
			errorMessages.add("(" + count + ") " + error.getMessage());
			count++;
		}
		return errorMessages;
	}

	private JsonSchema getJsonSchema(final String schemaUriString) throws IOException, JsonSchemaNotFoundException, URISyntaxException
	{
		JsonSchema jsonSchema = jsonSchemaPool.get(schemaUriString);
		if (jsonSchema != null) return jsonSchema;

		final URI uri = new URI("classpath:" + baseDir + "/" + schemaUriString);

		JsonSchemaFactory factory = null;
		if (schemaUriString.contains(".json"))
		{
			factory = schemaFactoryForJson;
		} else if (schemaUriString.contains(".yaml"))
		{
			factory = schemaFactoryForYaml;
		} else
		{
			throw new JsonSchemaNotFoundException(schemaUriString);
		}

		try
		{
			jsonSchema = factory.getSchema(uri);
			jsonSchema.initializeValidators();
		} catch (final Exception e)
		{
			log.error("\t [JSON Validation] Schema file ({}) does not exist !!", schemaUriString);
			throw new JsonSchemaNotFoundException(schemaUriString);
		}

		// 將 jsonSchema 放到暫存區
		if (jsonSchema != null) jsonSchemaPool.put(schemaUriString, jsonSchema);

		return jsonSchema;
	}

	// ===[ JsonSchema ]=============================================================================================== //

	public JsonSchema getJsonSchemaFromClasspath(final String fileNname)
	{
		final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileNname);
		return factory.getSchema(is);
	}

	public JsonSchema getJsonSchemaFromStringContent(final String schemaContent)
	{
		final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		return factory.getSchema(schemaContent);
	}

	public JsonSchema getJsonSchemaFromUrl(final String uri) throws URISyntaxException
	{
		final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		return factory.getSchema(new URI(uri));
	}

	public JsonSchema getJsonSchemaFromJsonNode(final JsonNode jsonNode)
	{
		final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		return factory.getSchema(jsonNode);
	}

	// Automatically detect version for given JsonNode
	public JsonSchema getJsonSchemaFromJsonNodeAutomaticVersion(final JsonNode jsonNode)
	{
		final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(jsonNode));
		return factory.getSchema(jsonNode);
	}

	public String getJsonSchemaFileFromClasspath(final String schemafileName) throws IOException
	{
		final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(baseDir + "/" + schemafileName);
		return readFromInputStream(inputStream);
	}

	private String readFromInputStream(final InputStream inputStream) throws IOException
	{
		final StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

	public Boolean getJsonValidEnabled()
	{
		return jsonValidEnabled;
	}

	public void setJsonValidEnabled(final Boolean jsonValidEnabled)
	{
		this.jsonValidEnabled = jsonValidEnabled;
	}
}