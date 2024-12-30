package solaris.nfm.controller;

import java.util.HashSet;
import java.util.Set;

import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.system.json.util.JsonNodeUtil;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.service.MecService;

@RestController
@RequestMapping("/v1/mec")
@Slf4j
public class MecCtr
{
	@Autowired
	private ObjectMapper	objectMapper;

	@Autowired
	private MecService		mecService;

	@GetMapping(value = "/versions")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchVersion() throws Exception
	{
		return ControllerUtil.createResponseJson((ArrayNode) this.mecService.doMethodGet("/mec-version"));
	}

	// ================================================================================================================
	// Region
	// ================================================================================================================
	@GetMapping(value = "/regions")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllRegions() throws Exception
	{
		final ArrayNode regions = (ArrayNode) this.mecService.doMethodGet("/region");
		for (final JsonNode node : regions)
		{
			final ObjectNode region = (ObjectNode) node;
			final String regionId = node.path("regionId").asText();

			final ArrayNode apps = (ArrayNode) this.mecService.doMethodGet("/region/app/" + regionId);
			region.put("installedAppCount", apps.size());
			final ObjectNode detail = (ObjectNode) this.mecService.doMethodGet("/region/status/" + regionId);
			region.put("regionStatus", detail.path("regionStatus").asText());
			final ArrayNode ips = (ArrayNode) detail.path("appIpPlan");
			Integer ipCount = 0;
			for (final JsonNode ip : ips)
				if (ip.path("used").asBoolean() == true) ipCount++;
			region.put("usedIpCount", ipCount);
			region.put("totalIpCount", ips.size());
		}
		return ControllerUtil.createResponseJson(regions);
	}

	@GetMapping(value = "/regions/{regionId}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRegion(@PathVariable("regionId") final String id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final ObjectNode regionData = (ObjectNode) this.mecService.doMethodGet("/region/" + Encode.forHtml(id));
		final ObjectNode regionDetail = (ObjectNode) this.mecService.doMethodGet("/region/status/" + Encode.forHtml(id));
		regionData.setAll(regionDetail);

		return JsonNodeUtil.sanitize(regionData);
	}

	@PostMapping(value = "/regions")
	@ResponseStatus(HttpStatus.CREATED)
	public JsonNode createRegion(@RequestBody final JsonNode json) throws Exception
	{
		final String regionId = this.mecService.createRegion("/region", json.path("info"));
		this.mecService.createRegionConfig(regionId, json.path("config"));

		return JsonNodeFactory.instance.objectNode().put("id", regionId);
	}

	@PutMapping(value = "/regions/{regionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void modifyRegion(@PathVariable("regionId") final String id, @RequestBody final JsonNode json) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.doMethodPut("/region/" + id + "?", json);
	}

	@DeleteMapping(value = "/regions/{regionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeRegion(@PathVariable("regionId") final String id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.deleteRegion("/region/" + id);
	}

	// ================================================================================================================
	// Region Configuration
	// ================================================================================================================
	@GetMapping(value = "/regions/{regionId}/config")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRegionConfig(@PathVariable("regionId") final String regionId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return this.mecService.doMethodGet("/region/deploy/configuration/" + regionId);
	}

	@PutMapping(value = "/regions/{regionId}/config")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void modifyRegionConfig(@PathVariable("regionId") final String regionId, @RequestBody final JsonNode json) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.doMethodPut("/region/deploy/configuration/" + regionId + "?", json);
	}

	private void checkRegionId(final String regionId) throws ExceptionBase
	{
		if (!getRegionIdSet().contains(regionId)) throw new EntityNotFoundException("There's no region ID (" + regionId + ") found.");
	}

	private Set<String> getRegionIdSet() throws ExceptionBase
	{
		final Set<String> regionIdSet = new HashSet<>();
		final ArrayNode regions = (ArrayNode) this.mecService.doMethodGet("/region");
		regions.forEach(region ->
		{
			regionIdSet.add(region.path("regionId").asText());
		});
		return regionIdSet;
	}

	// ================================================================================================================
	// Region Deploy
	// ================================================================================================================
	@PutMapping(value = "/regions/{regionId}/deploy/start")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void startDeployRegion(@PathVariable("regionId") final String regionId, @RequestBody final JsonNode json) throws Exception
	{
		final String mecVersion = json.path("mecVersion").asText();

		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		checkMecVersion(mecVersion);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.startDeployRegion(regionId, mecVersion);
	}

	@PutMapping(value = "/regions/{regionId}/deploy/stop")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void stopDeployRegion(@PathVariable("regionId") final String regionId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.stopDeployRegion(regionId);
	}

	@PutMapping(value = "/regions/{regionId}/deploy/redo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void redoDeployRegion(@PathVariable("regionId") final String regionId, @RequestBody final JsonNode json) throws Exception
	{
		final String mecVersion = json.path("mecVersion").asText();

		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		checkMecVersion(mecVersion);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.redoDeployRegion(regionId, mecVersion);
	}

	@PutMapping(value = "/regions/{regionId}/deploy/check")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode checkDeployRegion(@PathVariable("regionId") final String regionId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return this.mecService.checkDeployRegion(regionId);
	}

	private void checkMecVersion(final String mecVersion) throws ExceptionBase
	{
		final Set<String> mecVersionSet = getMecVersionSet();
		if (!mecVersionSet.contains(mecVersion)) throw new ExceptionBase(400, "MEC version (" + mecVersion + ") is invalid, it must be one of set " + mecVersionSet);
	}

	private Set<String> getMecVersionSet() throws ExceptionBase
	{
		final Set<String> mecVersionSet = new HashSet<>();
		final ArrayNode mecVersions = (ArrayNode) this.mecService.doMethodGet("/mec-version");
		mecVersions.forEach(mecVersion ->
		{
			mecVersionSet.add(mecVersion.path("mecVersion").asText());
		});
		return mecVersionSet;
	}

	// ================================================================================================================
	// Component
	// ================================================================================================================
	@GetMapping(value = "regions/{regionId}/components")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllComponents(@PathVariable("regionId") final String regionId) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return ControllerUtil.createResponseJson(getComponents(regionId));
	}

	@GetMapping(value = "regions/{regionId}/components/{componentName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchComponent(@PathVariable("regionId") final String regionId, @PathVariable("componentName") final String componentName) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		checkComponentName(regionId, componentName);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		for (final JsonNode component : getComponents(regionId))
		{
			if (component.path("componentName").asText().equalsIgnoreCase(componentName)) return JsonNodeUtil.sanitize(component);
		}
		return null;
	}

	@PutMapping(value = "regions/{regionId}/components/{componentName}/network")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateComponentNetwork(@PathVariable("regionId") final String regionId, @PathVariable("componentName") final String componentName, @RequestBody final JsonNode json) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		checkComponentName(regionId, componentName);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final String componentId = getComponentIdByName(regionId, componentName);
		this.mecService.updateComponentNetwork("/region/component/" + regionId + "?componentId=" + componentId + "&", json);
	}

	private ArrayNode getComponents(final String regionId) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		final ArrayNode components = (ArrayNode) this.mecService.doMethodGet("/region/component/" + regionId);
		final ArrayNode statuses = (ArrayNode) this.mecService.doMethodGet("/region/component/status/" + regionId);

		Integer index = 0;
		Integer upfIndex = -1;
		for (final JsonNode node : components)
		{
			final ObjectNode component = (ObjectNode) node;
			final String componentId = node.path("componentId").asText();
			final String componentName = node.path("componentName").asText();

			if (componentName.equalsIgnoreCase("upf"))
			{
				upfIndex = index;
				continue;
			}
			index++;

			for (final JsonNode status : statuses)
			{
				log.debug("={}", status.path("componentStatus").asText());
				if (status.path("componentId").asText().equals(componentId))
				{
					component.setAll((ObjectNode) status);
					final JsonNode statusValue = this.objectMapper.readTree(status.path("componentStatus").asText());
					component.replace("componentStatus", statusValue.path(componentName));
					break;
				}
			}
		}
		if (upfIndex != -1) components.remove(upfIndex);
		return components;
	}

	private String getComponentIdByName(final String regionId, final String componentName) throws ExceptionBase
	{
		final ArrayNode components = (ArrayNode) this.mecService.doMethodGet("/region/component/" + regionId);
		for (final JsonNode component : components)
		{
			if (component.path("componentName").asText().equalsIgnoreCase(componentName)) return component.path("componentId").asText();
		}

		return null;
	}

	private void checkComponentName(final String regionId, final String componentName) throws ExceptionBase
	{
		final Set<String> componentNameSet = getComponentNameSet(regionId);
		if (!componentNameSet.contains(componentName.toLowerCase())) throw new ExceptionBase(400, "Component name (" + componentName + ") is invalid, it must be one of set " + componentNameSet);
	}

	private Set<String> getComponentNameSet(final String regionId) throws ExceptionBase
	{
		final Set<String> componentNameSet = new HashSet<>();
		final ArrayNode components = (ArrayNode) this.mecService.doMethodGet("/region/component/" + regionId);
		components.forEach(component ->
		{
			componentNameSet.add(component.path("componentName").asText());
		});
		componentNameSet.remove("upf");
		return componentNameSet;
	}

	// ================================================================================================================
	// UPF
	// ================================================================================================================
	@GetMapping(value = "regions/{regionId}/upfs/{upfId}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUpf(@PathVariable("regionId") final String regionId, @PathVariable("upfId") final String upfId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return getUpfs(regionId).path(0);
	}

	@PostMapping(value = "regions/{regionId}/upfs")
	@ResponseStatus(HttpStatus.CREATED)
	public void createUpfNetwork(@PathVariable("regionId") final String regionId, @RequestBody final JsonNode json) throws Exception
	{
		final String componentId = getComponentIdByName(regionId, "upf");
		this.mecService.createUpfNetwork(regionId, componentId, json);
	}

	@DeleteMapping(value = "regions/{regionId}/upfs/{upfId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUpfNetwork(@PathVariable("regionId") final String regionId, @PathVariable("upfId") final String upfId) throws Exception
	{
		final String componentId = getComponentIdByName(regionId, "upf");
		this.mecService.deleteUpfNetwork(regionId, componentId);
	}

	private ArrayNode getUpfs(final String regionId) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		final ArrayNode upfs = JsonNodeFactory.instance.arrayNode();
		final ArrayNode components = (ArrayNode) this.mecService.doMethodGet("/region/component/" + regionId);
		final ArrayNode statuses = (ArrayNode) this.mecService.doMethodGet("/region/component/status/" + regionId);
		for (final JsonNode node : components)
		{
			final ObjectNode component = (ObjectNode) node;
			final String componentId = node.path("componentId").asText();
			final String componentName = node.path("componentName").asText();

			if (!componentName.equalsIgnoreCase("upf")) continue;

			for (final JsonNode status : statuses)
			{
				if (status.path("componentId").asText().equals(componentId))
				{
					component.setAll((ObjectNode) status);
					final JsonNode statusValue = this.objectMapper.readTree(status.path("componentStatus").asText());
					component.replace("componentStatus", statusValue);
					break;
				}
			}
			upfs.add(component);
		}
		return upfs;
	}

	// ================================================================================================================
	// App
	// ================================================================================================================
	@GetMapping(value = "regions/{regionId}/apps")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllApps(@PathVariable("regionId") final String regionId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return ControllerUtil.createResponseJson(getApps(regionId));
	}

	@GetMapping(value = "regions/{regionId}/apps/{appId}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchApp(@PathVariable("regionId") final String regionId, @PathVariable("appId") final String appId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return getApp(regionId, appId);
	}

	@PostMapping(value = "regions/{regionId}/apps")
	@ResponseStatus(HttpStatus.CREATED)
	public void createApp(@PathVariable("regionId") final String regionId, @RequestBody final JsonNode json) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.createApp(regionId, json);
	}

	@PutMapping(value = "regions/{regionId}/apps/{appId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void modifyApp(@PathVariable("regionId") final String regionId, @PathVariable("appId") final String appId, @RequestBody final JsonNode json) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.doMethodPut("/region/app/" + regionId + "?appId=" + appId + "&", json);
	}

	@DeleteMapping(value = "regions/{regionId}/apps/{appId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteApp(@PathVariable("regionId") final String regionId, @PathVariable("appId") final String appId) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		checkRegionId(regionId);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		this.mecService.deleteApp(regionId, appId);
	}

	private ArrayNode getApps(final String regionId) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		final ArrayNode apps = (ArrayNode) this.mecService.doMethodGet("/region/app/" + regionId);
		final ArrayNode statuses = (ArrayNode) this.mecService.doMethodGet("/region/app/status/" + regionId);

		for (final JsonNode node : apps)
		{
			final ObjectNode app = (ObjectNode) node;
			final String id = app.path("appId").asText();
			for (final JsonNode status : statuses)
			{
				final String id2 = status.path("appId").asText();
				if (id.endsWith(id2) == false) continue;

				app.setAll((ObjectNode) status);
				break;
			}
		}
		return apps;
	}

	private ObjectNode getApp(final String regionId, final String appId) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		final ObjectNode app = (ObjectNode) this.mecService.doMethodGet("/region/app/" + regionId + "/" + appId);
		final ObjectNode status = (ObjectNode) this.mecService.doMethodGet("/region/app/status/" + regionId + "/" + appId);
		app.setAll(status);
		return app;
	}

	@GetMapping(value = "regions/{regionId}/domains")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllDomains(@PathVariable("regionId") final String regionId) throws Exception
	{
		return this.mecService.doMethodGet("/region/domain/" + regionId);
	}

	@GetMapping(value = "regions/{regionId}/statuses")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllStatuses(@PathVariable("regionId") final String regionId) throws Exception
	{
		return this.mecService.doMethodGet("/region/status/" + regionId);
	}
}
