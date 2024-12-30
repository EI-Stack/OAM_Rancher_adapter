package solaris.nfm.model.resource.appgroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.querydsl.core.types.Predicate;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.controller.base.ControllerBase;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.RegularExpressionException;

@RestController
@RequestMapping("/v1")
@Slf4j
public class AppGroupCtr extends ControllerBase<AppGroup, AppGroupVo, AppGroup, AppGroup, AppGroupDmo>
{
	@Autowired
	private YamlParserService	yamlParserService;
	@Autowired
	private AppGroupDao			dao;
	@Autowired
	private AppGroupService		service;
	
	@GetMapping(value = "/version")
	@ResponseStatus(HttpStatus.OK)
	public String fetchRancherVersion() throws Exception
	{
		return service.getRancherVersion();
	}
	
//	@GetMapping(value = "/appGroups/{id}")
//	@ResponseStatus(HttpStatus.OK)
//	public AppGroupVo fetchAppGroup(@PathVariable("id") final Long id) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		this.dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final AppGroup appGroup = this.dao.getReferenceById(id);
//		final AppGroupVo vo = this.service.fetchAppGroup(appGroup.getAppInfos(), appGroup.getUuid());
//		BeanUtils.copyProperties(appGroup, vo);
//
//		return vo;
//	}

//	@GetMapping(value = "/appGroups")
//	@ResponseStatus(HttpStatus.OK)
//	public RestResultDto<AppGroupsVo> fetchAllAppGroups(@QuerydslPredicate(root = AppGroup.class) final Predicate predicate, final Pageable pageable) throws Exception
//	{
//		final ArrayNode sourceAppGroups = this.service.fetchAppGroups();
//		final Page<AppGroup> entityPage = this.dmo.findAll(predicate, pageable);
//		final List<AppGroupsVo> voList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
//		for (final AppGroup entity : entityPage)
//		{
//			final String appGroupName = entity.getName();
//			AppGroupsVo vo = new AppGroupsVo();
//			BeanUtils.copyProperties(entity, vo);
//			vo = this.service.editData(vo, sourceAppGroups);
//			log.info("appGroupName:"+appGroupName);
//			final JsonNode sourceAppGroup = this.service.findFromArrayNode(sourceAppGroups, "name", appGroupName);
//			vo.setStatus(sourceAppGroup.path("status").asText());
//			voList.add(vo);
//		}
//		
//		// 建立並回傳分頁資料集合
//		return new RestResultDto<>(voList, new PaginationDto(entityPage));
//	}

//	@PostMapping(value = "/appGroups")
//	@ResponseStatus(HttpStatus.CREATED)
//	public JsonNode createAppGroup(@Validated(AppGroupDto.Create.class) @RequestPart(name = "jsonData") final AppGroupDto dto,
//			@RequestParam(name = "file", required = false) final MultipartFile multipartFile) throws SecurityException, Exception
//	{
//		// ---[正規表達式檢查]-------------
//		if (!dto.getName().matches("^[0-9a-z.-]*"))
//		{
//			throw new RegularExpressionException(dto.getName());
//		}
//		// ---[正規表達式檢查]-------------
//
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		// 檢查 name 是否為唯一值
//		if (this.dao.countByName(dto.getName()) > 0) throw new EntityHasExistedException("App Group (" + dto.getName() + ") has existed.");
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//		log.info("Name:" + dto.getName() + " Priority:" + dto.getPriority());
//		final AppGroup detach = new AppGroup();
//		BeanUtils.copyProperties(dto, detach);
//
//		// Get app info by parsing zipped yaml file
//		final ArrayNode appInfos = (ArrayNode) this.yamlParserService.convertYamlToJson(multipartFile);
//		detach.setAppInfos(appInfos);
//		log.debug("appInfos=\n{}", appInfos.toPrettyString());
//		// Get yaml content by parsing a zipped yaml file
//		final Map<String, Object> yamlMap = this.yamlParserService.getYaml(multipartFile);
//		final Map<String, String> yamlContentMap = (Map<String, String>) yamlMap.get("yamlContent");
//		final HashMap<String, String> yamlFileMap = this.yamlParserService.getYamlFileListString(multipartFile);
//		log.debug("yamlContentMap=\n{}", (new ObjectMapper()).valueToTree(yamlContentMap).toPrettyString());
//
//		// Create AppGroup
//		final String appGroupUuid = this.service.createAppGroup(dto);
//		detach.setUuid(appGroupUuid);
//		//存入對應表
//		this.service.saveToCorrespondTable(appGroupUuid,detach.getName() , "", "");
//
//		// Create deployments
//		for (final JsonNode appInfo : appInfos)
//		{
//			final String appName = appInfo.path("appName").asText();
//			// Create a new deployment
//			log.debug("{}=\n{}", appName, yamlContentMap.get(appName));
//			JsonNode content = this.service.createDeployment
//					(dto.getName(), appGroupUuid, dto.getSiteName(), yamlFileMap.get(appName));
//			//存入對應表
//			this.service.saveToCorrespondTable(
//					appGroupUuid, detach.getName(), content.get("id").asText(), appName);
//			
//		}
//		final AppGroup entity = this.dmo.createOne(detach);
//		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
//	}
	
//	@DeleteMapping(value = "/appGroups/{id}")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	public void removeAppGroup(@PathVariable("id") final Long id) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final AppGroup entity = this.dao.getReferenceById(id);
//
//		// 刪掉所有的 Deployment(包含內部的task)
//		final ArrayNode appInfos = entity.getAppInfos();
//		ArrayList<String> taskIdList = new ArrayList<>();
//		for (final JsonNode appInfo : appInfos)
//		{
////			final String appName = appInfo.path("appName").asText();
////			//去資料庫找對應task 只會有一個所以直接取
////			log.info("Group name:" + entity.getName() + " appName:"+appName);
////			GroupTaskCorrespond correspond = this.service.selectByGroupNameAndTaskName(entity.getName(), appName).get(0);
////			//stop task
////			this.service.stopTask(correspond.getTaskId());
////			//remove task
////			this.service.removeTask(correspond.getTaskId());
////			//等等要做是否刪除成功的check
////			taskIdList.add(correspond.getTaskId());
////			//remove deployment  //現在不用deployment
//////			this.service.removeDeployment(entity.getName(), appName);
//		}
//		
//		Thread.sleep(3000);  //給K8S時間刪除上面的task
//		this.service.checkAllTaskDelete(taskIdList);  //檢查是否所有的task都delete ok
//		this.service.removeAppGroup(entity.getUuid());
//		this.service.deleteFromCorrespondTableByGroupId(entity.getUuid());
//
//		this.dmo.removeOne(id);
//	}

//	@PutMapping(value = "/{id}")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	public void modifyAppGroup(@PathVariable("id") final Long id, @Validated(AppGroupDto.Update.class) @RequestBody final AppGroupDto dto) throws Exception
//	{
//		System.out.println("@PutMapping(value = \"/{id}\")");
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final AppGroup entity = this.dao.getReferenceById(id);
//		BeanUtils.copyProperties(dto, entity);
//
//		this.dmo.modifyOne(entity);
//	}

//	@PatchMapping(value = "/appGroups/{id}/description")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	public void modifyDescriptionInAppGroup(@PathVariable("id") final Long id, @RequestBody final JsonNode json) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		this.dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final AppGroup entity = this.dao.getReferenceById(id);
//		entity.setDescription(json.path("description").asText());
//		this.dmo.modifyOne(entity);
//	}

//	@PatchMapping(value = "/appGroups/{id}/paasRequest")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	public void modifyPaasRequestInAppGroup(@PathVariable("id") final Long id, @RequestBody final JsonNode paasRequest) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		this.dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final AppGroup entity = this.dao.getReferenceById(id);
//		entity.setPaasRequest(paasRequest);
//
//		this.dmo.modifyOne(entity);
//	}

	// ---[ Resource ]-------------------------------------------------------------------------------------------------[START]
	/**
	 * 取得系統資源資料
	 */
//	@GetMapping(value = "/resource")
//	@ResponseStatus(HttpStatus.OK)
//	public JsonNode fetchResource() throws Exception
//	{
//		return this.service.fetchResource();
//	}

	/**
	 * 取得 AppGroup 資源資料
	 */
//	@GetMapping(value = "/appGroups/{id}/resource")
//	@ResponseStatus(HttpStatus.OK)
//	public JsonNode fetchResourceFromAppGroup(@PathVariable("id") final Long id) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final AppGroup entity = this.dao.getReferenceById(id);
//		return this.service.fetchResourceFromAppGroup(entity);
//	}

	// ---[ Resource ]-------------------------------------------------------------------------------------------------[END]

	// ---[ Site ]-----------------------------------------------------------------------------------------------------[START]
	/**
	 * Fetch all site names
	 */
//	@GetMapping(value = "/sites")
//	@ResponseStatus(HttpStatus.OK)
//	public JsonNode fetchSites() throws Exception
//	{
//		return ControllerUtil.createResponseJson(this.service.fetchSites());
//	}
	// ---[ Site ]-----------------------------------------------------------------------------------------------------[END]

//	@PostMapping("/yaml/contents/{appName}")
//	@ResponseStatus(HttpStatus.OK)
//	public String parseYamlZipFile(@PathVariable("appName") final String appName, @RequestPart(value = "yamlZipFile") final MultipartFile yamlZipFile) throws Exception
//	{
//		final Map<String, String> contentMap = (Map<String, String>) this.yamlParserService.getYaml(yamlZipFile).get("yamlContent");
//		return contentMap.get(appName);
//	}
//
//	@PostMapping("/yaml/appInfos")
//	@ResponseStatus(HttpStatus.OK)
//	public JsonNode fetchAppInfoByParseYamlZipFile(@RequestPart(value = "yamlZipFile") final MultipartFile yamlZipFile) throws Exception
//	{
//		final ArrayNode appInfos = (ArrayNode) this.yamlParserService.getYaml(yamlZipFile).get("appInfos");
//		log.debug("={}", appInfos);
//
//		return appInfos;
//	}
}