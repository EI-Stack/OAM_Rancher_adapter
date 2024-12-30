package solaris.nfm.model.resource.appgroup;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.rest.RestServiceBase;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.appgroup.AppGroupVo.App;
import solaris.nfm.util.DateTimeUtil;

@Service
@Slf4j
public class AppGroupService extends RestServiceBase
{
	@Value("${solaris.server.k8s.http.url}")
	private String url;
	
	@Autowired
	private ObjectMapper objectmapper;
	
	public AppGroupService(final RestTemplateBuilder restTemplateBuilder, final SslBundles sslBundles, final RestTemplate restTemplate, @Value("${solaris.server.adapter.core.http.url}") String url,
			@Value("${solaris.server.adapter.core.ssl.enabled:false}") Boolean sslEnabled, @Value("${solaris.server.adapter.core.ssl.bundle:}") String sslBundleName,
			@Value("${solaris.server.adapter.core.http.token:}") String token) 
	{
		super.initService(restTemplateBuilder, sslBundles, restTemplate, url, token, sslEnabled, sslBundleName);
	}

	@PostConstruct
	public void init()
	{
//		final HttpHeaders httpHeaders = new HttpHeaders();
//		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//		httpHeaders.setOrigin(this.url);
//		super.httpHeaders = httpHeaders;
//
//		super.networkUrl = this.url;
	}

//	public String createAppGroup(final AppGroupDto dto) throws UnsupportedEncodingException, ExceptionBase
//	{
//		final String url = "/api/v1/groups?&name=" + dto.getName() + "&priority=" + dto.getPriority() + "&preempted=" + (dto.getPreempted() == true ? "y" : "n") + "&role=saas&create_ns=y";
//		final JsonNode responseConetent = create(url, null);
//		final String appGroupUuid = responseConetent.path("id").asText();
//
//		return appGroupUuid;
//	}

//	public void removeAppGroup(final String appGroupUuid) throws UnsupportedEncodingException, JsonMappingException, JsonProcessingException, InterruptedException
//	{
//		final String url = "/api/v1/groups/" + appGroupUuid + "?delete_ns=y";
//		try {
//			Thread.sleep(5000);  //給k8s時間
//			delete(url);
//		}catch(ExceptionBase e) {
//			e.printStackTrace();
//			final String errorMessage = MessageFormat.format("Connecting to outer server is failed. Message：{0}", e.getMessage());
//			log.error("\t[ErrorHandler] Status Code=[{}]", e.getErrorCode());
//			log.error("\t[ErrorHandler] response.getBody() ={}", e.getErrorMessage());
//			log.error("\t[ErrorHandler] Error Message: {}", errorMessage);
//		}
//	}

//	public ArrayNode fetchAppGroups() throws UnsupportedEncodingException, ExceptionBase
//	{
//		final String url = "/api/v1/groups?is_deep=y";
//		final ObjectNode content = getJsonNode(url);
//		final ArrayNode appGroups = (ArrayNode) content.path("items");
//
//		return appGroups;
//	}

//	public AppGroupVo fetchAppGroup(final ArrayNode appInfos, final String appGroupUuid) throws UnsupportedEncodingException, ExceptionBase
//	{
//		final String url = "/api/v1/groups/" + appGroupUuid + "?is_deep=y";
//		final ObjectNode content = getJsonNode(url);
//
//		log.debug("={}", content.toPrettyString());
//		final AppGroupVo vo = new AppGroupVo();
//		vo.setSiteName(content.path("site").asText());
//		vo.setUuid(appGroupUuid);
//		vo.setName(content.path("name").asText());
//		vo.setPriority(content.path("priority").asInt());
//		vo.setPreempted(content.path("preempted").asBoolean());
//
//		final ArrayNode tasks = (ArrayNode) content.path("tasks");
//		for (final JsonNode task : tasks)
//		{
//			final String appName = task.path("name").asText();
//			System.out.println("appName:"+appName);
//			final JsonNode appInfo = findAppInfo(appInfos, appName);
//
//			final App app = new App();
//			app.setName(appName);
//
//			app.setResource(appInfo.path("resource"));
//			app.setService(appInfo.path("service"));
//
//			app.setCreationTime(DateTimeUtil.castIsoToUtcZonedDateTime(task.path("create_time").asText()));
//			app.setStartTime(DateTimeUtil.castIsoToUtcZonedDateTime(task.path("start_time").asText()));
//			app.setScheduelTime(task.path("sch_time").asText());
//			app.setReplica(task.path("replica_set").asInt());
//
//			app.setStatus(task.path("status").asText());
//
//			vo.getApps().add(app);
//		}
//
//		return vo;
//	}

	public ObjectNode fetchResourceFromAppGroup(final AppGroup appGroup)
	{
		Integer cpuSum = 0;
		Integer gpuSum = 0;
		Long memorySum = 0L;
		Long ssdSum = 0L;
		Long hddSum = 0L;
		Long pmemSum = 0L;
		final ArrayNode appInfos = appGroup.getAppInfos();
		for (final JsonNode appInfo : appInfos)
		{
			cpuSum += castCpuValue(appInfo.path("resource").path("cpu").asText());
			gpuSum += appInfo.path("resource").path("gpu").asInt();
			memorySum += castMemoryValue(appInfo.path("resource").path("memory").asText());

			final ArrayNode storages = (ArrayNode) appInfo.path("resource").path("storages");
			for (final JsonNode storage : storages)
			{
				final String type = storage.path("type").asText();

				switch (type)
				{
					case "ssd" :
						ssdSum += castMemoryValue(storage.path("value").asText());
						break;
					case "hdd" :
						hddSum += castMemoryValue(storage.path("value").asText());
						break;
					case "pmem" :
						pmemSum += castMemoryValue(storage.path("value").asText());
						break;
					default :
						log.error("Unknown storage type: " + type);
				}
			}
		}

		final ObjectNode resourceSum = JsonNodeFactory.instance.objectNode();
		resourceSum.put("cpu", cpuSum);
		resourceSum.put("gpu", gpuSum);
		resourceSum.put("memory", memorySum);
		final ObjectNode storage = resourceSum.putObject("storage");
		storage.put("ssd", ssdSum);
		storage.put("hdd", hddSum);
		storage.put("pmem", pmemSum);

		return resourceSum;
	}

	public JsonNode findAppInfo(final ArrayNode appInfos, final String appName)
	{
		JsonNode returnAppInfo = null;
		for (final JsonNode appInfo : appInfos)
		{
			if (appInfo.path("appName").asText().equals(appName)) returnAppInfo = appInfo;
		}
		return returnAppInfo;
	}

	public JsonNode findFromArrayNode(final ArrayNode arrayNode, final String key, final String value)
	{
		JsonNode returnNode = null;
		for (final JsonNode node : arrayNode)
		{
			if (node.path(key).asText().equals(value)) {log.info("key:"+node.path(key).asText());returnNode = node;}
		}
		return returnNode;
	}

//	public String createTask(final String appGroupUuid, final String scheduleTime, final String yamlContent) throws UnsupportedEncodingException, ExceptionBase
//	{
//
//		final String url = "/api/v1/namespaces/default/tasks?group_id=" + appGroupUuid + "&sch_time=" + scheduleTime + "&owner=itri";
//		final JsonNode responseJson = create(url, null);
//		final Integer errorCode = responseJson.path("error_code").asInt();
//		final String errorMessage = responseJson.path("error_string").asText();
//
//		if (errorCode != 0 || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(errorCode, errorMessage);
//
//		return responseJson.path("id").asText();
//	}
//
//	public void removeTask(final String taskUuid) throws UnsupportedEncodingException, JsonMappingException, JsonProcessingException, InterruptedException
//	{
//		final String url = "/api/v1/tasks/" + taskUuid;
//		try {
//			log.info("Remove task id:" + taskUuid);
//			delete(url);
//			Thread.sleep(3000);  //休息三秒 給K8S時間
//		}catch(ExceptionBase e) {
//			e.printStackTrace();
//			final String errorMessage = MessageFormat.format("Connecting to outer server is failed. Message：{0}", e.getMessage());
//			log.error("\t[ErrorHandler] Status Code=[{}]", e.getErrorCode());
//			log.error("\t[ErrorHandler] response.getBody() ={}", e.getErrorMessage());
//			log.error("\t[ErrorHandler] Error Message: {}", errorMessage);
//		}
//		
//	}
//
//	public ArrayNode fetchTasks(final String appGroupId) throws ExceptionBase
//	{
//		final String namespaces = appGroupId;
//		final String urlString = "/api/v1/namespaces/" + namespaces + "/tasks";
//		final JsonNode response = getJsonNode(urlString);
//
//		return (ArrayNode) response.path("items");
//	}
//	
//	public boolean taskExist(final String taskId) throws ExceptionBase
//	{
//		final String urlString = this.url + "/api/v1/tasks/" + taskId;
//
//		log.debug("\t GET URL=[{}]", urlString);
//		final HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(this.httpHeaders);
//		ResponseEntity<JsonNode> response = null;
//		try
//		{
//			response = this.restTemplate.exchange(urlString, HttpMethod.GET, requestEntity, JsonNode.class);
//			log.debug("\t GET StatusCode={}", response.getStatusCode());
//			if (!(response.getStatusCode().equals(HttpStatus.OK))) throw new ExceptionBase(400, "Fetching resource is failed.");
//
//			final JsonNode bodyJson = response.getBody();
//			final String errorCode = bodyJson.path("error_code").asText();
//			final String errorMessage = bodyJson.path("error_string").asText();
//			if (errorCode.equalsIgnoreCase("20101") &&
//				errorMessage.contains("not found") && 
//				errorMessage.contains(taskId)) {
//				return false;
//			}else if(errorCode.equalsIgnoreCase("00000") &&
//					 errorMessage.equalsIgnoreCase("success")){
//				return true;
//			}else {
//				return true;
//			}
//		} catch (final HttpClientErrorException ex)
//		{
//			handleErrorMessage(ex);
//		}
//		return true;
//	}
//	
//	public boolean checkAllTaskDelete(ArrayList<String> taskIdList) throws ExceptionBase, InterruptedException {
//		for(int i = 0; i < taskIdList.size(); i++) {
//			while(taskExist(taskIdList.get(i))) {  //一個檢查完了才會看下一個
//				log.info("TaskId:" + taskIdList.get(i) + " deleting");
//				Thread.sleep(6000);
//			}
//		}
//		return true;
//	}
//
//	public ArrayNode fetchSites() throws ExceptionBase
//	{
//		final ArrayNode sites = JsonNodeFactory.instance.arrayNode();
//
//		final String urlString = "/api/v1/sites";
//		final JsonNode content = getJsonNode(urlString);
//		final ArrayNode sourceSites = (ArrayNode) content.path("items");
//		for (final JsonNode site : sourceSites)
//		{
//			sites.add(site.path("name").asText());
//		}
//		log.info("sites:" + sites);
//		return sites;
//	}
//
//	public ArrayNode fetchResource() throws ExceptionBase
//	{
//		final String urlString = "/api/v1/sites";
//		final JsonNode content = getJsonNode(urlString);
//		final ArrayNode sourceSites = (ArrayNode) content.path("items");
//
//		final ArrayNode sites = JsonNodeFactory.instance.arrayNode();
//		// final ObjectNode resource = JsonNodeFactory.instance.objectNode();
//
//		for (final JsonNode sourceSite : sourceSites)
//		{
//			final ObjectNode site = JsonNodeFactory.instance.objectNode();
//			site.put("name", sourceSite.path("name").asText());
//			final ObjectNode resource = site.putObject("resource");
//			final ObjectNode cpu = resource.putObject("cpu");
//			final ObjectNode gpu = resource.putObject("gpu");
//			final ObjectNode memory = resource.putObject("memory");
//
//			cpu.put("total", castCpuValue(sourceSite.path("total_allocatable").path("cpu").asText()));
//			gpu.put("total", sourceSite.path("total_allocatable").path("gpu").asInt(0));
//			memory.put("total", castMemoryValue(sourceSite.path("total_allocatable").path("mem").asText()));
//
//			cpu.put("usage", castCpuValue(sourceSite.path("total_allocated").path("cpu").asText()));
//			gpu.put("usage", sourceSite.path("total_allocated").path("gpu").asInt(0));
//			memory.put("usage", castMemoryValue(sourceSite.path("total_allocated").path("mem").asText()));
//
//			sites.add(site);
//		}
//		log.info("sites:" + sites);
//
//		return sites;
//	}

	private Long castMemoryValue(final String value)
	{
		Long castedValue = 0l;

		log.debug("value={}", value);

		if (StringUtils.hasText(value) == false)
			castedValue = 0l;
		else if (value.contains("K") || value.contains("Ki"))
			castedValue = Long.valueOf(value.replaceAll("Ki?", "")) * 1_000;
		else if (value.contains("M") || value.contains("Mi"))
			castedValue = Long.valueOf(value.replaceAll("Mi?", "")) * 1_000_000;
		else if (value.contains("G") || value.contains("Gi"))
			castedValue = Long.valueOf(value.replaceAll("Gi?", "")) * 1_000_000_000;
		else if (value.contains("T") || value.contains("Ti"))
			castedValue = Long.valueOf(value.replaceAll("Ti?", "")) * 1_000_000_000_000l;
		else if (value.contains("P") || value.contains("Pi"))
			castedValue = Long.valueOf(value.replaceAll("Pi?", "")) * 1_000_000_000_000_000l;
		else if (value.contains("E") || value.contains("Ei"))
			castedValue = Long.valueOf(value.replaceAll("Ei?", "")) * 1_000_000_000_000_000_000l;
		else
			castedValue = Long.valueOf(value);

		log.debug("castedValue={}", castedValue);
		return castedValue;
	}

	private Integer castCpuValue(final String value)
	{
		Integer castedValue = 0;

		if (StringUtils.hasText(value) == false)
			castedValue = 0;
		else if (value.contains("m"))
			castedValue = Integer.valueOf(value.replace("m", ""));
		else
			castedValue = Integer.valueOf(value) * 1_000;

		return castedValue;
	}

	// ---[ Deployment ]------------------------------------------------------------------------------------------------------[START]

	/**
	 * 須注意 body 傳送的資料是 text，不是 json
	 */
//	public JsonNode createDeployment(final String appGroupName, final String siteName, final String yamlContent) throws UnsupportedEncodingException, ExceptionBase
//	{
//		final String url = this.networkUrl + "/api/v1/namespaces/" + appGroupName + "/deployments?site=" + siteName;
//		final HttpHeaders httpHeaders = new HttpHeaders();
//		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
//		httpHeaders.setOrigin(this.url);
//
//		log.debug("\t POST URL=[{}]", url);
//		JsonNode responseJson = null;
//		final HttpEntity<String> requestEntity = new HttpEntity<>(yamlContent, this.httpHeaders);
//		try
//		{
//			final ResponseEntity<JsonNode> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
//			log.debug("POST StatusCode =[{}]", response.getStatusCode().toString());
//			if (!response.getStatusCode().equals(HttpStatus.OK)) throw new ExceptionBase(400, "Creating resource is failed.");
//
//			final JsonNode bodyJson = response.getBody();
//			final String errorCode = bodyJson.path("error_code").asText();
//			final String errorMessage = bodyJson.path("error_string").asText();
//			if (!errorCode.equalsIgnoreCase("00000") || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(Integer.parseInt(errorCode), errorMessage);
//
//			responseJson = bodyJson.path("content");
//		} catch (final HttpClientErrorException ex)
//		{
//			handleErrorMessage(ex);
//		}
//
//		return responseJson;
//	}
	
	/**
	 * 須注意 body 傳送的資料是 text，不是 json
	 * 目前預設在create group的同時，建立一個與group同名的namespace
	 */
//	public JsonNode createDeployment(final String appGroupName, final String groupId,
//									 final String siteName,     final String yamlContent) throws UnsupportedEncodingException, ExceptionBase{
//		System.out.println("appGroupName:" + appGroupName);
//		System.out.println("groupId:" + groupId);
//		System.out.println("siteName:" + siteName);
//		System.out.println("yamlContent:\n" + yamlContent);
//		final String url = this.networkUrl + "/api/v1/namespaces/"+appGroupName+"/tasks?site="+siteName+"&group_id="+groupId+"&owner=itri";
//		final HttpHeaders httpHeaders = new HttpHeaders();
//		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
//		httpHeaders.setOrigin(this.url);
//
//		log.debug("\t POST URL=[{}]", url);
//		JsonNode responseJson = null;
//		final HttpEntity<String> requestEntity = new HttpEntity<>(yamlContent, this.httpHeaders);
//		try
//		{
//			final ResponseEntity<JsonNode> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
//			log.debug("POST StatusCode =[{}]", response.getStatusCode().toString());
//			if (!response.getStatusCode().equals(HttpStatus.OK)) throw new ExceptionBase(400, "Creating resource is failed.");
//
//			final JsonNode bodyJson = response.getBody();
//			final String errorCode = bodyJson.path("error_code").asText();
//			final String errorMessage = bodyJson.path("error_string").asText();
//			if (!errorCode.equalsIgnoreCase("00000") || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(Integer.parseInt(errorCode), errorMessage);
//
//			responseJson = bodyJson.path("content");
//		} catch (final HttpClientErrorException ex)
//		{
//			handleErrorMessage(ex);
//		}
//
//		return responseJson;
//	}
	
//	public void stopTask(final String taskId) throws ExceptionBase {
//		final String url = "/api/v1/task-stop/" + taskId;
//		final JsonNode response = getJsonInformation(url);
//		log.info("Stop task taskId:" + taskId);
//		log.info(response.toPrettyString());
//	}
	
//	public void removeDeployment(final String appGroupName, final String appName) throws UnsupportedEncodingException, ExceptionBase, JsonMappingException, JsonProcessingException
//	{
//		final String url = "/api/v1/namespaces/" + appGroupName + "/deployments/" + appName;
//		delete(url);
//	}
	
	// ---[ Edit vo ]-----------------------------------
	public AppGroupsVo editData(AppGroupsVo saas,  ArrayNode sourceAppGroups) {
		String name = saas.getName();
		for(JsonNode source : sourceAppGroups) {
			if(source.get("name").asText().equals(name)) {
				saas.setSiteName(source.get("name").asText());
				saas.setPriority(source.get("priority").asInt());
				saas.setPreempted(source.get("preempted").asBoolean());
				saas.setSiteName(source.get("site").asText());
			}
		}
		return saas;
	}
	
}