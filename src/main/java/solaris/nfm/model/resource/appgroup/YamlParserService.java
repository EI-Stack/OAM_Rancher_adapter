package solaris.nfm.model.resource.appgroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class YamlParserService
{
	@Autowired
	private ObjectMapper objectMapper;

	public JsonNode convertYamlToJson(final MultipartFile file) throws Exception
	{
		// 取得unzip後的資料夾.
		final List<File> unZipFiles = ZipUtil.unzipForYaml(file);

		// 讀取yaml.
		final Yaml yaml = new Yaml();
		Map<String, Object> maps = new LinkedHashMap<>();
		final List<Map<String, Object>> lists = new LinkedList<>();
		for (int i = 0; i < unZipFiles.size(); i++)
		{
			final InputStream inputStream = new FileInputStream(unZipFiles.get(i).getName());
			maps = yaml.load(inputStream);
			lists.add(maps);
		}

		// 轉成json格式.
		final String jsonMaps = objectMapper.writeValueAsString(lists);
		final JsonNode jsonNode = objectMapper.readValue(jsonMaps, JsonNode.class);
		// log.info("jsonNode result : {}", jsonNode);

		// json schema檢查
		// JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
		// JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		// JsonSchema schema = factory.getJsonSchema(jsonNode);

		final ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();

		// if (filter.containsKey("resources")) {
		for (int i = 0; i < jsonNode.size(); i++)
		{
			final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
			final ObjectNode objectNodeResources = JsonNodeFactory.instance.objectNode();
			for (int j = 0; j < jsonNode.path(i).path("items").size(); j++)
			{
				// 抓storage.
				final ObjectNode objectNodeStorage = JsonNodeFactory.instance.objectNode();
				final ArrayNode arrayNodeStorage = JsonNodeFactory.instance.arrayNode();
				if (!jsonNode.path(i).path("items").path(j).path("spec").path("resources").isEmpty())
				{
					String key = jsonNode.path(i).path("items").path(j).path("spec").path("resources").path("storageClassName").asText();
					key = key.substring(key.indexOf("-") + 1);
					final String value = jsonNode.path(i).path("items").path(j).path("spec").path("resources").path("requests").path("storage").asText();
					objectNodeStorage.put("type", key);
					objectNodeStorage.put("value", value);

					arrayNodeStorage.add(objectNodeStorage);
					// 塞storage.
					objectNodeResources.set("storages", arrayNodeStorage);
				}

				if ("apps/v1".equals(jsonNode.path(i).path("items").path(j).path("apiVersion").asText()) ||
					"v1".equals(jsonNode.path(i).path("items").path(j).path("apiVersion").asText()))
					//兩個都認
				{
					// 抓appName.
					String appName = jsonNode.path(i).path("items").path(j).path("metadata").path("name").asText();
					objectNode.put("appName", appName);
					log.info("\t Yaml paser appName:" + appName);

					// 抓cpu.
					objectNodeResources.put("cpu",
							jsonNode.path(i).path("items").path(j).path("spec").path("template").path("spec").path("containers").path(0).path("resources").path("limits").path("cpu").asText());

					// 抓memory.
					objectNodeResources.put("memory",
							jsonNode.path(i).path("items").path(j).path("spec").path("template").path("spec").path("containers").path(0).path("resources").path("limits").path("memory").asText());

					// 抓gpu.
					String gpuValue = jsonNode.path(i).path("items").path(j).path("spec").path("template").path("spec").path("containers").path(0).path("resources").path("limits").path("amd.com/gpu").asText();
					if(gpuValue.equals("")) {
						gpuValue = jsonNode.path(i).path("items").path(j).path("spec").path("template").path("spec").path("containers").path(0).path("resources").path("limits").path("nvidia.com/gpu").asText();
					}
					objectNodeResources.put("gpu", gpuValue);

					// 抓empty storages
					if (objectNodeResources.path("storages").isEmpty())
					{
						objectNodeStorage.put("type", "");
						objectNodeStorage.put("value", "");
						objectNodeStorage.put("mountPath", "");

						arrayNodeStorage.add(objectNodeStorage);
						// 塞storage.
						objectNodeResources.put("storages", arrayNodeStorage);
					} else
					{
						// 抓mounthPath 有資料.
						final ObjectNode mouthPath = (ObjectNode) objectNodeResources.path("storages").path(0);
						mouthPath.put("mountPath",
								jsonNode.path(i).path("items").path(j).path("spec").path("template").path("spec").path("containers").path(0).path("volumeMounts").path(0).path("mountPath").asText());
					}

					objectNode.put("resource", objectNodeResources);
				}

				// 抓全部的service內容
				if ("Service".equals(jsonNode.path(i).path("items").path(j).path("kind").asText()))
				{
					objectNode.put("service", jsonNode.path(i).path("items").path(j));
				}

			}
			arrayNode.add(objectNode);
		}
		// } else {
		// //都不篩選的話直接回傳jsonNode.
		// arrayNode = (ArrayNode) jsonNode;
		// }
		return arrayNode;
		// 把所有的yaml檔全部放在一起返回 用app name 分類.
	}

	public Map<String, Object> getYaml(final MultipartFile file) throws Exception
	{

		// 取得unzip後的資料夾.
		final List<File> unZipFiles = ZipUtil.unzipForYaml(file);

		// 讀取yaml.
		final Yaml yaml = new Yaml();

		Map<String, String> maps = new LinkedHashMap<>();
		final List<String> lists = new LinkedList<>();
		final Map<String, String> yamlMap = new LinkedHashMap<>();
		final Map<String, Object> resultMap = new LinkedHashMap<>();
		for (int i = 0; i < unZipFiles.size(); i++)
		{
			final InputStream inputStream = new FileInputStream(unZipFiles.get(i).getName());
			maps = yaml.load(inputStream);
			// 輸出string 格式.
			final String output = yaml.dump(maps);
			lists.add(output);

			// call convertYamlToJson.
			final JsonNode yamlInfo = convertYamlToJson(file);

			yamlMap.put(yamlInfo.path(i).path("appName").asText(), lists.get(i).toString());

			resultMap.put("appInfos", yamlInfo);
			resultMap.put("yamlContent", yamlMap);
		}
		return resultMap;
	}
	
	public HashMap<String, String> getYamlFileListString(final MultipartFile file) throws Exception{
		HashMap<String, String> maps = new HashMap<>();
		// 取得unzip後的資料夾.
		final List<File> unZipFiles = ZipUtil.unzipForYaml(file);
		//讀取所有yaml檔案
		for (int i = 0; i < unZipFiles.size(); i++)
		{
			final InputStream inputStream = new FileInputStream(unZipFiles.get(i).getName());
			String result = "";
		    try (BufferedReader br
		      = new BufferedReader(new InputStreamReader(inputStream))) {
		        String line;
		        int tmp = 0;
		        while ((line = br.readLine()) != null) {
		        	tmp++;
		        	if(line.trim().startsWith("#")) {
		        		continue;
		        	}
		        	if(tmp > 3) {  //前三行不要
		        		result += line.replaceFirst("  ", "") + "\n";//最前面的兩個縮排拿掉
		        	}
		        }
		    }
		    result = result.replace("- apiVersion", "apiVersion");
		    result = result.replace("apiVersion", "---\napiVersion");//做分隔線
		    result = result.replaceFirst("---\\napiVersion", "apiVersion");//第一個不用 要換回來
		    
		    
		    
		    // call convertYamlToJson.
 			final JsonNode yamlInfo = convertYamlToJson(file);
 			String appName = yamlInfo.path(i).path("appName").asText();
		    maps.put(appName, result);
		}
		return maps;
	}
}