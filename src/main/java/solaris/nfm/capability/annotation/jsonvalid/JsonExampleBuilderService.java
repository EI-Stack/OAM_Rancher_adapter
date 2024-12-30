package solaris.nfm.capability.annotation.jsonvalid;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.swagger.oas.inflector.Constants;
import io.swagger.oas.inflector.examples.models.ArrayExample;
import io.swagger.oas.inflector.examples.models.BooleanExample;
import io.swagger.oas.inflector.examples.models.DecimalExample;
import io.swagger.oas.inflector.examples.models.DoubleExample;
import io.swagger.oas.inflector.examples.models.Example;
import io.swagger.oas.inflector.examples.models.FloatExample;
import io.swagger.oas.inflector.examples.models.IntegerExample;
import io.swagger.oas.inflector.examples.models.LongExample;
import io.swagger.oas.inflector.examples.models.NullExample;
import io.swagger.oas.inflector.examples.models.ObjectExample;
import io.swagger.oas.inflector.examples.models.StringExample;
import io.swagger.oas.inflector.processors.JsonNodeExampleSerializer;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JsonExampleBuilderService
{
	private static String						baseDir			= "json-schema";
	// 將 Example 放入暫存 Map<yamlFileName-schemaName, Example>
	final private Map<String, JsonNode>			examplePool		= new HashMap<>();
	// 從 OpenApi/YAML 格式內容的檔案讀取所有的 schema，將 schema 放入暫存 Map<yamlFileName, Map<schemaName, Schema>>
	private Map<String, Map<String, Schema>>	schemaPool		= new HashMap<>();
	private SimpleModule						simpleModule	= null;

	@Autowired
	private final ObjectMapper					objectMapper	= new ObjectMapper();
	@Autowired
	private JsonService							jsonSrv;

	public enum RequestType
	{
		READ,
		WRITE
	}

	public static final String	SAMPLE_EMAIL_PROPERTY_VALUE			= "apiteam@swagger.io";
	public static final String	SAMPLE_UUID_PROPERTY_VALUE			= "3fa85f64-5717-4562-b3fc-2c963f66afa6";
	public static final String	SAMPLE_STRING_PROPERTY_VALUE		= "string";
	public static final int		SAMPLE_INT_PROPERTY_VALUE			= 0;
	public static final int		SAMPLE_LONG_PROPERTY_VALUE			= 0;
	public static final int		SAMPLE_BASE_INTEGER_PROPERTY_VALUE	= 0;
	public static final float	SAMPLE_FLOAT_PROPERTY_VALUE			= 1.1f;
	public static final double	SAMPLE_DOUBLE_PROPERTY_VALUE		= 1.1f;
	public static final boolean	SAMPLE_BOOLEAN_PROPERTY_VALUE		= true;
	public static final String	SAMPLE_DATE_PROPERTY_VALUE			= "2015-07-20";
	public static final String	SAMPLE_DATETIME_PROPERTY_VALUE		= "2015-07-20T15:49:04-07:00";
	public static final double	SAMPLE_DECIMAL_PROPERTY_VALUE		= 1.5;

	@PostConstruct
	public void init()
	{
		simpleModule = new SimpleModule().addSerializer(new JsonNodeExampleSerializer());
	}

	/**
	 * 依據 OpenApi/YAML 格式內容的檔案，組成 JSON 範例
	 *
	 * @param yamlFileName:
	 *        ex, "R16/TS28623_GenericNrm.yaml"
	 */
	public JsonNode getJsonExampleFromSchema(final String yamlFileName, final String schemaName) throws IOException
	{
		final String poolKey = yamlFileName + "-" + schemaName;
		if (examplePool.containsKey(poolKey))
		{
			log.debug("使用 Example 暫存 {}", poolKey);
			return examplePool.get(poolKey);
		}

		final Map<String, Schema> schemas = getSchemas(yamlFileName);
		if (schemas == null) return null;
		final Example example = fromSchema(schemaName, schemas, false);
		// final Schema propertySchema = definitions.get(propertyName);
		// final Example example = ExampleBuilder.fromSchema(propertySchema, definitions);
		Json.mapper().registerModule(simpleModule);

		final JsonNode result = objectMapper.readTree(Json.pretty(example));
		if (result != null) examplePool.put(poolKey, result);
		return result;
	}

	/**
	 * 從 OpenApi/YAML 格式內容的檔案讀取所有的 schema
	 */
	public Map<String, Schema> getSchemas(final String yamlFileName)
	{
		JsonNode schemaNode = null;

		final String poolKey = yamlFileName;
		if (schemaPool.containsKey(poolKey))
		{
			log.debug("使用 schema 暫存 {}", poolKey);
			return schemaPool.get(poolKey);
		}

		try
		{
			schemaNode = jsonSrv.getJsonNodeFromClasspathForYaml(JsonExampleBuilderService.baseDir + "/" + yamlFileName);
		} catch (final IOException e)
		{
			e.printStackTrace();
			log.debug("\t [JSON] Reading file (" + yamlFileName + ") is failed !!");
			return null;
		}
		final OpenAPI openAPI = ((new OpenAPIV3Parser()).parseJsonNode(null, schemaNode)).getOpenAPI();

		final Map<String, Schema> result = openAPI.getComponents().getSchemas();
		if (result != null) schemaPool.put(poolKey, result);
		return result;
	}

	public Example fromSchema(final String propertyName, final Map<String, Schema> definitions, final boolean onlyRequired)
	{
		final Schema propertySchema = definitions.get(propertyName);
		return fromProperty(null, propertySchema, definitions, new HashMap<>(), null, false, false, onlyRequired);
	}

	public Example fromProperty(String name, final Schema property, final Map<String, Schema> definitions, final Map<String, Example> processedModels, final RequestType requestType,
			final boolean nullExample, final boolean processNullExampleExtension, final boolean onlyRequired)
	{

		if (property == null)
		{
			return null;
		}
		if (property.getReadOnly() != null && property.getReadOnly() && requestType == RequestType.WRITE)
		{
			return null;
		}
		if (property.getWriteOnly() != null && property.getWriteOnly() && requestType == RequestType.READ)
		{
			return null;
		}

		// name = null;
		String namespace = null;
		String prefix = null;
		Boolean attribute = false;
		Boolean wrapped = false;

		if (property.getXml() != null)
		{
			final XML xml = property.getXml();
			name = xml.getName();
			namespace = xml.getNamespace();
			prefix = xml.getPrefix();
			attribute = xml.getAttribute();
			wrapped = xml.getWrapped() != null ? xml.getWrapped() : false;
		}

		Example output = null;

		final Object example = property.getExample();

		if (example == null && property.get$ref() == null)
		{
			if (nullExample)
			{
				return new NullExample();
			}
			if (processNullExampleExtension)
			{
				if (property.getExtensions() != null && property.getExtensions().get(Constants.X_INFLECTOR_NULL_EXAMPLE) != null)
				{
					return new NullExample();
				}
			} else if (property.getExampleSetFlag())
			{
				return new NullExample();
			}
		}

		log.debug("property.getName()={}", property.getPropertyNames());
		log.debug("property.get$ref()={}", property.get$ref());
		if (property.get$ref() != null)
		{
			String ref = property.get$ref();

			final String referencedFileName = getLinkedFileName(ref);
			log.debug("yamlFileName={}", referencedFileName);

			ref = ref.substring(ref.lastIndexOf("/") + 1);
			log.debug("ref={}", ref);
			if (processedModels.containsKey(ref))
			{
				// 此處是回傳一個空 example，但是帶有資料類型
				// return some sort of example
				return alreadyProcessedRefExample(ref, definitions, processedModels);
			}
			processedModels.put(ref, null);
			if (definitions != null)
			{
				// Holisun: 此行只能取得同份文件下的 schema
				if (referencedFileName.equals(""))
				{
					final Schema model = definitions.get(ref);
					if (model != null)
					{
						output = fromProperty(ref, model, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						processedModels.put(ref, output);
						return output;
					}
				} else
				{
					final Map<String, Schema> subDefinitions = getSchemas("R16/" + referencedFileName);
					final Schema model2 = subDefinitions.get(ref);
					if (model2 != null)
					{
						output = fromProperty(ref, model2, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						processedModels.put(ref, output);
						return output;
					}
				}
			}
		} else if (property instanceof EmailSchema)
		{
			if (example != null)
			{
				output = new StringExample(example.toString());
			} else
			{
				String defaultValue = ((EmailSchema) property).getDefault();

				if (defaultValue == null)
				{
					final List<String> enums = ((EmailSchema) property).getEnum();
					if (enums != null && !enums.isEmpty())
					{
						defaultValue = enums.get(0);
					}
				}

				output = new StringExample(defaultValue == null ? SAMPLE_EMAIL_PROPERTY_VALUE : defaultValue);
			}
		} else if (property instanceof UUIDSchema)
		{
			if (example != null)
			{
				output = new StringExample(example.toString());
			} else
			{
				UUID defaultValue = ((UUIDSchema) property).getDefault();

				if (defaultValue == null)
				{
					final List<UUID> enums = ((UUIDSchema) property).getEnum();
					if (enums != null && !enums.isEmpty())
					{
						defaultValue = enums.get(0);
					}
				}

				output = new StringExample(defaultValue == null ? SAMPLE_UUID_PROPERTY_VALUE : defaultValue.toString());
			}
		} else if (property instanceof StringSchema)
		{
			if (example != null)
			{
				output = new StringExample(example.toString());
			} else
			{
				String defaultValue = ((StringSchema) property).getDefault();

				if (defaultValue == null)
				{
					final List<String> enums = ((StringSchema) property).getEnum();
					if (enums != null && !enums.isEmpty())
					{
						defaultValue = enums.get(0);
					}
				}

				output = new StringExample(defaultValue == null ? SAMPLE_STRING_PROPERTY_VALUE : defaultValue);
			}
		} else if (property instanceof PasswordSchema)
		{
			if (example != null)
			{
				output = new StringExample(example.toString());
			} else
			{
				String defaultValue = ((PasswordSchema) property).getDefault();

				if (defaultValue == null)
				{
					final List<String> enums = ((PasswordSchema) property).getEnum();
					if (enums != null && !enums.isEmpty())
					{
						defaultValue = enums.get(0);
					}
				}

				output = new StringExample(defaultValue == null ? SAMPLE_STRING_PROPERTY_VALUE : defaultValue);
			}
		} else if (property instanceof IntegerSchema)
		{
			if (example != null)
			{
				try
				{
					if (property.getFormat() != null)
					{
						if (property.getFormat().equals("int32"))
						{
							output = new IntegerExample(Integer.parseInt(example.toString()));
						} else if (property.getFormat().equals("int64"))
						{
							output = new LongExample(Long.parseLong(example.toString()));
						}
					} else
					{
						output = new IntegerExample(Integer.parseInt(example.toString()));
					}
				} catch (final NumberFormatException e)
				{}
			}

			if (output == null)
			{
				Number defaultValue = ((IntegerSchema) property).getDefault();

				if (defaultValue == null)
				{
					final List<Number> enums = ((IntegerSchema) property).getEnum();
					if (enums != null && !enums.isEmpty())
					{
						defaultValue = enums.get(0);
					}
				}
				if (property.getFormat() != null)
				{
					if (property.getFormat().equals("int32"))
					{
						output = new IntegerExample(defaultValue == null ? SAMPLE_INT_PROPERTY_VALUE : defaultValue.intValue());
					} else if (property.getFormat().equals("int64"))
					{
						output = new LongExample(defaultValue == null ? SAMPLE_LONG_PROPERTY_VALUE : defaultValue.longValue());
					}
				} else
				{
					output = new IntegerExample(SAMPLE_BASE_INTEGER_PROPERTY_VALUE);
				}
			}
		} else if (property instanceof NumberSchema)
		{

			if (example != null)
			{
				try
				{
					if (property.getFormat() != null)
					{
						if (property.getFormat().equals("double"))
						{
							output = new DoubleExample(Double.parseDouble(example.toString()));
						} else if (property.getFormat().equals("float"))
						{
							output = new FloatExample(Float.parseFloat(example.toString()));
						}
					} else
					{
						output = new DecimalExample(new BigDecimal(example.toString()));
					}
				} catch (final NumberFormatException e)
				{}
			}

			if (output == null)
			{
				BigDecimal defaultValue = ((NumberSchema) property).getDefault();

				if (defaultValue == null)
				{
					final List<BigDecimal> enums = ((NumberSchema) property).getEnum();
					if (enums != null && !enums.isEmpty())
					{
						defaultValue = enums.get(0);
					}
				}
				if (property.getFormat() != null)
				{
					if (property.getFormat().equals("double"))
					{
						output = new DoubleExample(defaultValue == null ? SAMPLE_DOUBLE_PROPERTY_VALUE : defaultValue.doubleValue());
					}
					if (property.getFormat().equals("float"))
					{
						output = new FloatExample(defaultValue == null ? SAMPLE_FLOAT_PROPERTY_VALUE : defaultValue.floatValue());
					}
				} else
				{
					output = new DecimalExample(new BigDecimal(SAMPLE_DECIMAL_PROPERTY_VALUE));
				}
			}

		} else if (property instanceof BooleanSchema)
		{
			if (example != null)
			{
				output = new BooleanExample(Boolean.valueOf(example.toString()));
			} else
			{
				final Boolean defaultValue = (Boolean) property.getDefault();
				output = new BooleanExample(defaultValue == null ? SAMPLE_BOOLEAN_PROPERTY_VALUE : defaultValue.booleanValue());
			}
		} else if (property instanceof DateSchema)
		{
			final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			if (example != null)
			{
				final String exampleAsString = format.format(example);
				output = new StringExample(exampleAsString);
			} else
			{

				final List<Date> enums = ((DateSchema) property).getEnum();
				if (enums != null && !enums.isEmpty())
				{
					output = new StringExample(format.format(enums.get(0)));
				} else
				{
					output = new StringExample(SAMPLE_DATE_PROPERTY_VALUE);
				}
			}
		} else if (property instanceof DateTimeSchema)
		{
			if (example != null)
			{
				final String exampleAsString = example.toString();
				output = new StringExample(exampleAsString);
			} else
			{
				final List<OffsetDateTime> enums = ((DateTimeSchema) property).getEnum();
				if (enums != null && !enums.isEmpty())
				{
					output = new StringExample(enums.get(0).toString());
				} else
				{
					output = new StringExample(SAMPLE_DATETIME_PROPERTY_VALUE);
				}
			}
		} else if (property instanceof ObjectSchema)
		{
			if (example != null)
			{
				try
				{
					output = Json.mapper().readValue(example.toString(), ObjectExample.class);
				} catch (final IOException e)
				{
					log.error("unable to convert `" + example + "` to JsonNode");
					output = new ObjectExample();
				}
			} else
			{
				final ObjectExample outputExample = new ObjectExample();
				outputExample.setName(property.getName());
				// Holisun
				final List<String> requiredList = property.getRequired();
				log.debug("onlyRequired={}", onlyRequired);
				log.debug("requiredList={}", requiredList);
				final ObjectSchema op = (ObjectSchema) property;
				if (op.getProperties() != null)
				{
					for (final String propertyname : op.getProperties().keySet())
					{
						// Holisun
						if (onlyRequired == true && requiredList != null && requiredList.contains(propertyname) == false) continue;

						final Schema inner = op.getProperties().get(propertyname);
						final Example innerExample = fromProperty(null, inner, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						outputExample.put(propertyname, innerExample);
					}
					output = outputExample;
				}

			}
		} else if (property instanceof ArraySchema)
		{
			if (example != null)
			{
				try
				{
					output = Json.mapper().readValue(example.toString(), ArrayExample.class);
				} catch (final IOException e)
				{
					log.error("unable to convert `" + example + "` to JsonNode");
					output = new ArrayExample();
				}
			} else
			{
				final ArraySchema ap = (ArraySchema) property;
				final Schema inner = ap.getItems();
				// Holisun
				final Integer minItems = ap.getMinItems();
				log.debug("minItems={}", minItems);
				// Holisun
				if (inner != null && minItems != null && minItems > 0)
				{
					final Object innerExample = fromProperty(null, inner, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
					log.debug("innerExample={}", innerExample);
					if (innerExample != null)
					{
						if (innerExample instanceof Example)
						{
							final ArrayExample an = new ArrayExample();
							// Holisun
							if (minItems != null)
							{
								for (int i = 0; i < minItems; i++)
									an.add((Example) innerExample);
								an.setName(property.getName());
								output = an;
							}

						}
					}
				}
			}
		} else if (property instanceof ComposedSchema)
		{
			// validate resolved validators if true send back the first property if false the actual code
			final ComposedSchema composedSchema = (ComposedSchema) property;
			if (composedSchema.getAllOf() != null)
			{

				final List<Schema> models = composedSchema.getAllOf();
				final ObjectExample ex = new ObjectExample();

				final List<Example> innerExamples = new ArrayList<>();
				if (models != null)
				{
					for (final Schema im : models)
					{
						final Example innerExample = fromProperty(null, im, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						if (innerExample != null)
						{
							innerExamples.add(innerExample);
						}
					}
				}
				mergeTo(ex, innerExamples);
				output = ex;
			}
			if (composedSchema.getAnyOf() != null)
			{

				final List<Schema> models = composedSchema.getAnyOf();
				if (models != null)
				{
					for (final Schema im : models)
					{
						final Example innerExample = fromProperty(null, im, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						if (innerExample != null)
						{
							output = innerExample;
							break;
						}
					}
				}
			}
			if (composedSchema.getOneOf() != null)
			{
				final List<Schema> models = composedSchema.getOneOf();

				if (models != null)
				{
					for (final Schema im : models)
					{
						final Example innerExample = fromProperty(null, im, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						if (innerExample != null)
						{
							output = innerExample;
							break;
						}
					}
				}
			}
		} else if (property.getProperties() != null && output == null)
		{
			if (example != null)
			{
				try
				{
					output = Json.mapper().readValue(example.toString(), ObjectExample.class);
				} catch (final IOException e)
				{
					log.error("unable to convert `" + example + "` to JsonNode");
					output = new ObjectExample();
				}
			} else
			{
				final ObjectExample ex = new ObjectExample();

				if (property.getProperties() != null)
				{
					final Map<String, Schema> properties = property.getProperties();
					for (final String propertyKey : properties.keySet())
					{
						final Schema inner = properties.get(propertyKey);
						final Example propExample = fromProperty(null, inner, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
						ex.put(propertyKey, propExample);
					}
				}

				output = ex;
			}

		}

		if (property.getAdditionalProperties() instanceof Schema)
		{
			final Schema inner = (Schema) property.getAdditionalProperties();
			if (inner != null)
			{
				for (int i = 1; i <= 3; i++)
				{
					final Example innerExample = fromProperty(null, inner, definitions, processedModels, requestType, nullExample, processNullExampleExtension, onlyRequired);
					if (innerExample != null)
					{
						if (output == null)
						{
							output = new ObjectExample();
						}
						final ObjectExample on = (ObjectExample) output;
						final String key = "additionalProp" + i;
						if (innerExample.getName() == null)
						{
							innerExample.setName(key);
						}

						if (!on.keySet().contains(key))
						{
							on.put(key, innerExample);
						}
					}
				}
			}
		} else if (property.getAdditionalProperties() instanceof Boolean && output == null)
		{
			output = new ObjectExample();
		}

		if (output != null)
		{
			if (attribute != null)
			{
				output.setAttribute(attribute);
			}
			if (wrapped != null && wrapped)
			{
				if (name != null)
				{
					output.setWrappedName(name);
				}
			}
			if (name != null)
			{
				output.setName(name);
			}
			output.setNamespace(namespace);
			output.setPrefix(prefix);
			output.setWrapped(wrapped);
		}
		return output;
	}

	public static Example alreadyProcessedRefExample(final String name, final Map<String, Schema> definitions, final Map<String, Example> processedModels)
	{
		if (processedModels.get(name) != null)
		{
			return processedModels.get(name);
		}
		final Schema model = definitions.get(name);
		if (model == null)
		{
			return null;
		}
		final Example output = null;

		// look at type
		if (model.getType() != null)
		{
			if ("object".equals(model.getType()))
			{
				return new ObjectExample();
			}
			if ("string".equals(model.getType()))
			{
				return new StringExample("");
			}
			if ("integer".equals(model.getType()))
			{
				return new IntegerExample(0);
			}
			if ("long".equals(model.getType()))
			{
				return new LongExample(0);
			}
			if ("float".equals(model.getType()))
			{
				return new FloatExample(0);
			}
			if ("double".equals(model.getType()))
			{
				return new DoubleExample(0);
			}
		}

		return output;
	}

	public static void mergeTo(final ObjectExample output, final List<Example> examples)
	{
		for (final Example ex : examples)
		{
			if (ex instanceof ObjectExample)
			{
				final ObjectExample objectExample = (ObjectExample) ex;
				final Map<String, Example> values = objectExample.getValues();
				if (values != null)
				{
					output.putAll(values);
				}
			}
		}
	}

	private String getLinkedFileName(final String ref)
	{
		String fileName = "";
		final String tmpfileName = ref.split("#")[0];
		if (StringUtils.hasText(tmpfileName))
		{
			fileName = tmpfileName.substring(2);
		}

		return fileName;
	}
}
