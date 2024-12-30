package solaris.nfm.controller;

import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.JsonValid;
import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.capability.system.json.util.JsonNodeUtil;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.profile.ProfileDao;
import solaris.nfm.service.DataFormationCheckService;
import solaris.nfm.service.FgcService;
import solaris.nfm.service.RicService;
import solaris.nfm.service.SliceService.PlmnId;
import solaris.nfm.util.DateTimeUtil;

@RestController
@RequestMapping("/v1/fgc")
@Slf4j
public class FgcCtr
{
	@Autowired
	private ObjectMapper				objectMapper;
	@Autowired
	private FgcService					service;
	@Autowired
	private RicService					ricSrv;
	@Autowired
	private ProfileDao					profileDao;

	@Autowired
	private DataFormationCheckService	dataFormationCheckSrv;

	@GetMapping(value = "/layout")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchLayout() throws Exception
	{
		final ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("sliceNames", service.getSliceNames());
		result.set("serviceAreaListNames", service.getServiceAreaListNames());
		result.set("intersections", service.packageUpf());

		result.set("combineSlices", ricSrv.getCombineSlices());

		log.debug("\t result=\n{}", result.toPrettyString());
		return result;
	}

	@GetMapping(value = "/layout/location")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchLocationView() throws Exception
	{
		final ArrayNode result = JsonNodeFactory.instance.arrayNode();

		final ArrayNode serviceAreaLists = (ArrayNode) this.service.doMethodGet("/v1/serviceAreaLists");

		for (final JsonNode serviceAreaList : serviceAreaLists)
		{
			final ObjectNode rSal = JsonNodeFactory.instance.objectNode();
			rSal.put("name", serviceAreaList.path("name").asText());
			final ArrayNode rSas = rSal.putArray("serviceAreas");
			final ArrayNode rTas = rSal.putArray("trackingAreas");
			final ArrayNode rCells = rSal.putArray("cells");
			final ArrayNode rGnbs = rSal.putArray("gnbs");

			final ArrayNode serviceAreaNames = (ArrayNode) serviceAreaList.path("serviceArea");
			for (final JsonNode serviceAreaName : serviceAreaNames)
			{
				rSas.add(serviceAreaName.path("name").asText());
				final JsonNode serviceArea = this.service.doMethodGet("/v1/serviceAreas/" + serviceAreaName.path("name").asText());
				final ArrayNode taiListNames = (ArrayNode) serviceArea.path("taiList");

				for (final JsonNode taiListName : taiListNames)
				{
					final JsonNode taiList = this.service.doMethodGet("/v1/taiLists/" + taiListName.path("name").asText());
					final ArrayNode taiNames = (ArrayNode) taiList.path("tai");

					for (final JsonNode taiName : taiNames)
					{
						final String taiNameString = taiName.path("name").asText();
						rTas.add(taiName.path("name").asText());
						final JsonNode tai = this.service.doMethodGet("/v1/tais/" + taiNameString);
						rCells.addAll((ArrayNode) tai.path("cells"));
						rGnbs.addAll((ArrayNode) tai.path("gnbs"));
					}
				}
			}
			result.add(rSal);
		}

		log.debug("result=\n{}", result.toPrettyString());

		return JsonNodeUtil.sanitize(result);
	}

	// 單一 UE 視角，呈現相關 UE 屬性。顯示 UE 允許出現的場域與切片，並顯示登入後的場域與切片
	@GetMapping(value = "/layout/ues/{supi}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUe(@PathVariable("supi") final String supi) throws Exception
	{
		dataFormationCheckSrv.checkSupi(supi);
		return this.service.doMethodGet("/v1/5gc/layout/ues/" + supi);
	}

	// 單一 UE 視角，搬移或複製 UE 允許出現的場域與切片
	@PutMapping(value = "/layout/ues/{supi}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	@JsonValid("api.yaml#/A01")
	public void replaceUe(@PathVariable("supi") final String supi, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/5gc/layout/ues/" + supi, requestJson);
	}

	// UPF 視角
	@GetMapping(value = "/layout/upfs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchLayoutUpfs() throws Exception
	{
		final ArrayNode upfs = (ArrayNode) this.service.doMethodGet("/v1/5gc/layout/upfs");
		return ControllerUtil.createResponseJson(upfs);
	}

	@GetMapping(value = "/config")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchConfig() throws Exception
	{
		return this.service.doMethodGet("/config");
	}

	@PutMapping(value = "/config")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public void replaceConfig(@RequestBody final JsonNode requestJson) throws Exception
	{
		// this.service.modify("/config", requestJson);
		this.service.doMethodPost("/config", requestJson);
	}

	@GetMapping(value = "/sm/algorithms")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAlgorithms() throws Exception
	{
		return this.service.doMethodGet("/sm/algo");
	}

	@PutMapping(value = "/sm/algorithms")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceAlgorithms(@RequestBody final JsonNode requestJson) throws Exception
	{
		// this.service.modify("/sm/algo", requestJson);
		this.service.doMethodPost("/sm/algo", requestJson);
	}

	@GetMapping(value = "/em/status")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchEmStatuses(@RequestParam final String networkType) throws Exception
	{
		return this.service.doMethodGet("/em/status?nf=" + networkType);
	}

	@DeleteMapping(value = "/em/status/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeEmStatuses(@PathVariable("id") final Long id) throws Exception
	{
		this.service.doMethodDetele("/em/status/" + id);
	}

	@GetMapping(value = "/nfs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchNfsList() throws Exception
	{
		return this.service.doMethodGet("/nfs");
	}

	// ---[ UE Subscription ]------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/ue/subscriptions")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllUeSubscriptions(
			@RequestParam(name="page",required=false,defaultValue = "1") int page,
			@RequestParam(name="size",required=false,defaultValue ="10") int pageSize) throws Exception
	{
		//先取得所有 Subscription
		JsonNode subList = this.service.getSubscriptionList();
		ArrayNode subscriptions = null ;
		if(!subList.isNull() && subList != null) {
			subscriptions = (ArrayNode)subList;
		}else {
			subscriptions = objectMapper.createArrayNode();
		}
		return ControllerUtil.createResponseJson(subscriptions, page, pageSize);
	}

	@GetMapping(value = "/ue/subscription/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUeSubscriptionCount() throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/ue/subscription/count");
	}

	@GetMapping(value = "/ues/{supi}/subscription")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUeSubscription(@PathVariable("supi") final String supi) throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/ues/" + supi + "/subscription");
	}

	@PostMapping(value = "/ue/subscription")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public void createUeSubscription(@RequestBody final JsonNode requestJson) throws Exception
	{
		// final PlmnId plmnId = new PlmnId("001", "35");
		// ((ObjectNode) requestJson).set("plmnId", this.objectMapper.valueToTree(plmnId));

		// final Long profileId = requestJson.path("profileId").asLong();
		// final ObjectNode profileJson = this.profileDao.getReferenceById(profileId).getJson();
		// ((ObjectNode) requestJson).remove("profileId");
		// ((ObjectNode) requestJson).set("profile", profileJson);

		log.debug("\t requestJson=\n{}", requestJson.toPrettyString());

		this.service.doMethodPost("/v1/5gc/ue/subscription", requestJson);
		this.service.evictAllCache();  //clear cache
	}

	@PutMapping(value = "/ues/{supi}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyUeSubscription(@PathVariable("supi") final String supi, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/5gc/ues/" + supi + "/subscription", requestJson);
		this.service.evictAllCache();  //clear cache
	}

	@DeleteMapping(value = "/ues/{supi}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeUeSubscription(@PathVariable("supi") final String supi) throws Exception
	{
		this.service.doMethodDetele("/v1/5gc/ues/" + supi + "/subscription");
		this.service.evictAllCache();  //clear cache
	}

	private void modifyUeForAffirmed(final JsonNode requestJson) throws ExceptionBase
	{
		final PlmnId plmnId = new PlmnId("001", "35");
		final String imsi = "001010000040071";
		final String supi = "imsi-" + imsi;
		final JsonNode json = this.service.createUeProfileForAffirmed(plmnId, imsi);
		log.debug("json=\n{}", json.toPrettyString());
		this.service.doMethodPut("/v1/ues/" + supi, json);
	}

	private void removeUeForAffirmed(final String supi) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		this.service.doMethodDetele("/v1/ues/" + supi);
	}

	private void removeUeForFree5gc(final String supi) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		final String imsi = supi.replaceFirst("imsi-", "");
		this.service.doMethodDetele("/ncms-oam/v1/subscriber/" + imsi + "/20893");
	}

	private ArrayNode getUeFormAffirmed() throws ExceptionBase
	{
		final ArrayNode ues = JsonNodeFactory.instance.arrayNode();
		final ArrayNode sues = (ArrayNode) this.service.doMethodGet("/ues");
		for (final JsonNode sue : sues)
		{
			final ObjectNode ue = JsonNodeFactory.instance.objectNode();
			ue.set("supi", sue.path("supi"));
			ue.set("state", sue.path("ue-state"));
			final String isoString = sue.path("last-ue-activity-time").asText();
			ue.put("last-ue-activity-time", DateTimeUtil.castIsoToUtcString(fixIsoString(isoString)));
			ue.set("nci", sue.path("conn-info").path("user-location-info").path("nr-location-info").path("nr-cgi").path("cell-id"));
			ue.put("gnbId", sue.path("conn-info").path("global-ran-node-id").asText().split(",")[3]);
			ue.set("subscribedUeAmbr", sue.path("amData").path("subscribedUeAmbr"));
			ue.set("nssai", sue.path("amData").path("nssai").path("singleNssais"));

			ues.add(ue);
		}
		return ues;
	}

	private ArrayNode getUeFormFree5gc() throws ExceptionBase
	{
		final ArrayNode ues = JsonNodeFactory.instance.arrayNode();
		final ArrayNode sues = (ArrayNode) this.service.doMethodGet("/ncms-oam/v1/ue/");

		// final ArrayNode sues = JsonNodeFactory.instance.arrayNode();
		// final ObjectNode sue1 = JsonNodeFactory.instance.objectNode();
		// sue1.put("Supi", "supi123").put("CmState", "CONNECTED").putObject("RandId").putObject("GNBId").put("gNBValue", "00000001");
		// final ObjectNode sue2 = JsonNodeFactory.instance.objectNode();
		// sue2.put("Supi", "supi456").put("CmState", "CONNECTED").putObject("RandId").putObject("GNBId").put("gNBValue", "00000002");
		// final ObjectNode sue3 = JsonNodeFactory.instance.objectNode();
		// sue3.put("Supi", "supi789").put("CmState", "CONNECTED").putObject("RandId").putObject("GNBId").put("gNBValue", "00000003");
		// sues.add(sue1).add(sue2).add(sue3);
		// log.debug("sues={}", sues.toPrettyString());

		for (final JsonNode sue : sues)
		{
			final ObjectNode ue = JsonNodeFactory.instance.objectNode();
			final String supi = sue.path("Supi").asText();

			ue.put("supi", supi);
			ue.put("state", sue.path("CmState").asText());
			ue.put("last-ue-activity-time", "");
			ue.put("nci", "");
			ue.put("gnbId", sue.path("RandId").path("GNBId").path("gNBValue").asText());

			try
			{
				final ArrayNode subscription = (ArrayNode) this.service.doMethodGet("/ncms-oam/v1/subscriber/" + supi + "/20893");
				// final ObjectNode subscription = this.service.createUeSubscription(supi, "k", "op");

				ue.set("subscribedUeAmbr", subscription.path("AccessAndMobilitySubscriptionData").path("subscribedUeAmbr"));
				ue.set("nssai", subscription.path("SessionManagementSubscriptionData").path("singleNssai"));

			} catch (final Exception e)
			{
				log.error("Fetching subscription data is failed. (supi =" + supi + ")");
			}

			ues.add(ue);
		}
		return ues;
	}

	private ObjectNode getSingleUeFromFree5gc(final String supi) throws ExceptionBase
	{
		final ObjectNode ue = JsonNodeFactory.instance.objectNode();
		final ObjectNode subscription = (ObjectNode) this.service.doMethodGet("/ncms-oam/v1/subscriber/" + supi + "/20893");

		ue.put("supi", supi);
		ue.put("state", "");
		ue.put("last-ue-activity-time", "");
		ue.put("nci", "");
		ue.put("gnbId", "");
		ue.set("subscribedUeAmbr", subscription.path("AccessAndMobilitySubscriptionData").path("subscribedUeAmbr"));
		ue.set("nssai", subscription.path("SessionManagementSubscriptionData").path(0).path("singleNssai"));

		return ue;
	}

	private void createUeForAffirmed(final JsonNode requestJson) throws ExceptionBase
	{
		final String supi = requestJson.path("supi").asText();
		final PlmnId plmnId = new PlmnId("001", "35");
		((ObjectNode) requestJson).set("plmnId", this.objectMapper.valueToTree(plmnId));
		final Long profileId = requestJson.path("profileId").asLong();
		final ObjectNode profileJson = this.profileDao.getReferenceById(profileId).getJson();
		((ObjectNode) requestJson).remove("profileId");
		((ObjectNode) requestJson).set("profile", profileJson);

		log.debug("json=\n{}", requestJson.toPrettyString());
		this.service.doMethodPut("/v1/ues/" + supi, requestJson);
	}

	private void createUeForFree5gc(final JsonNode requestJson) throws ExceptionBase
	{
		final String imsi = requestJson.path("imsi").asText();
		final String k = requestJson.path("key").path("k").asText();
		final String op = requestJson.path("key").path("op").asText();

		final ObjectNode json = this.service.createUeSubscription(imsi, k, op);
		log.debug("json=\n{}", json.toPrettyString());
		this.service.doMethodPut("/ncms-oam/v1/subscriber/" + imsi + "/20893", json);
	}

	// ---[ UE Subscription ]------------------------------------------------------------------------------------------[END]

	// ---[ UE registration ]------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/ue/registrations")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllUeRegistrations(@RequestParam final Map<String, String> requestParams) throws Exception
	{
		// /v1/5gc/ue/registrations?cmState=CONNECTED | REGISTERED
		String url = "/v1/5gc/ue/registrations";
		if (requestParams.size() > 0)
		{
			url += "?";
			for (final String paramName : requestParams.keySet())
			{
				url += paramName + "=" + requestParams.get(paramName) + "&";
			}
			url = url.substring(0, url.length() - 1);
		}

		final ArrayNode registrations = (ArrayNode) this.service.doMethodGet(url);
		return ControllerUtil.createResponseJson(registrations);
	}

	@GetMapping(value = "/ue/registration/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUeRegistrationCount(@RequestParam final Map<String, String> requestParams) throws Exception
	{
		// /v1/5gc/ue/registration?cmState=CONNECTED | REGISTERED
		String url = "/v1/5gc/ue/registration/count";
		if (requestParams.size() > 0)
		{
			url += "?";
			for (final String paramName : requestParams.keySet())
			{
				url += paramName + "=" + requestParams.get(paramName) + "&";
			}
			url = url.substring(0, url.length() - 1);
		}

		return this.service.doMethodGet(url);
	}

	@GetMapping(value = "/ues/{supi}/registration")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUeRegistration(@PathVariable("supi") final String supi) throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/ues/" + supi + "/registration");
	}

	@GetMapping(value = "/ue/pduSession/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUePduSessionCount() throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/ue/pduSession/count");
	}

	@GetMapping(value = "/ues/{supi}/pduSession/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUePduSessionCount(@PathVariable("supi") final String supi) throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/ues/" + supi + "/pduSession/count");
	}

	// ---[ UE registration ]------------------------------------------------------------------------------------------[END]

	// ---[ gNB ]------------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/gnbs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllGnbs() throws Exception
	{
		final ArrayNode gnbs = (ArrayNode) this.service.doMethodGet("/v1/gnbs");
		return ControllerUtil.createResponseJson(gnbs);
	}

	@GetMapping(value = "/gnb/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchGnbCount(@PathVariable("id") final String id) throws Exception
	{
		return this.service.doMethodGet("/v1/gnb/count");
	}
	// ---[ gNB ]------------------------------------------------------------------------------------------------------[END]

	// ---[ Network Function ]-----------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/nf/statuses")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllNfStatuses() throws Exception
	{
		final ArrayNode statuses = (ArrayNode) this.service.doMethodGet("/v1/5gc/nf/statuses");
		return ControllerUtil.createResponseJson(statuses);
	}

	@GetMapping(value = "/nfs/{nfName}/config")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchNfStatus(@PathVariable("nfName") final String nfName) throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/nfs/" + nfName + "/config");
	}
	// ---[ Network Function ]-----------------------------------------------------------------------------------------[END]

	// ---[ UPF ]------------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/associatedUpfs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllAssociatedUpfs(@RequestParam(name="page",required=false,defaultValue = "1") int page,
			@RequestParam(name="size",required=false,defaultValue = "10") int pageSize) throws Exception
	{
		final ArrayNode associatedUpfs = (ArrayNode) this.service.doMethodGet("/v1/5gc/associatedUpfs");
		return ControllerUtil.createResponseJson(associatedUpfs, page, pageSize);
	}

	@GetMapping(value = "/associatedUpf/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAssociatedUpfCount() throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/associatedUpf/count");
	}

	@GetMapping(value = "/registeredUpfs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllRegisteredUpfs(@RequestParam(name="page",required=false,defaultValue = "1") int page,
			@RequestParam(name="size",required=false,defaultValue = "10") int pageSize) throws Exception
	{
		final ArrayNode registeredUpfs = (ArrayNode) this.service.doMethodGet("/v1/5gc/registeredUpfs");
		return ControllerUtil.createResponseJson(registeredUpfs, page, pageSize);
	}

	@GetMapping(value = "/registeredUpf/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRegisteredUpfCount() throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/registeredUpf/count");
	}

	@GetMapping(value = "/topology")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchTopology() throws Exception
	{
		return this.service.doMethodGet("/v1/5gc/topology");
	}

	@GetMapping(value = "/upfs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllUpf(@RequestParam(name="page",required=false,defaultValue = "1") int page,
			 @RequestParam(name="size",required=false,defaultValue = "10") int pageSize) throws ExceptionBase, JsonMappingException, JsonProcessingException 
	{
		ArrayNode upfsInfo = this.service.getUpfsInfo(); 
		return ControllerUtil.createResponseJson(upfsInfo, page, pageSize);
	}
	// ---[ UPF ]------------------------------------------------------------------------------------------------------[END]

	// ---[ Alarm ]----------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/alarms")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllAlarms(@RequestParam final Map<String, String> requestParams) throws Exception
	{
		// /v1/alarms?nf=amf | oam
		String url = "/v1/5gc/alarms";
		if (requestParams.size() > 0)
		{
			url += "?";
			for (final String paramName : requestParams.keySet())
			{
				url += paramName + "=" + requestParams.get(paramName) + "&";
			}
			url = url.substring(0, url.length() - 1);
		}

		final ArrayNode alarms = (ArrayNode) this.service.doMethodGet(url);
		return ControllerUtil.createResponseJson(alarms);
	}
	// ---[ Alarm ]----------------------------------------------------------------------------------------------------[END]

	// ---[ NSSAI ]----------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/nssais")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllNssais() throws Exception
	{
		final ArrayNode nssais = (ArrayNode) this.service.doMethodGet("/v1/nssais");
		return ControllerUtil.createResponseJson(nssais);
	}

	@GetMapping(value = "/nssais/{nssaiName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchNssai(@PathVariable("nssaiName") final String nssaiName) throws Exception
	{
		final JsonNode nssai = this.service.doMethodGet("/v1/nssais/" + Encode.forHtml(nssaiName));
		return JsonNodeUtil.sanitize(nssai);
	}

	@PutMapping(value = "/nssais/{nssaiName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceNssai(@PathVariable("nssaiName") final String nssaiName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/nssais/" + nssaiName, requestJson);
	}

	@DeleteMapping(value = "/nssais/{nssaiName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeNssai(@PathVariable("nssaiName") final String nssaiName) throws Exception
	{
		this.service.doMethodDetele("/v1/nssais/" + nssaiName);
	}
	// ---[ NSSAI ]----------------------------------------------------------------------------------------------------[END]

	// ---[ SNSSAI ]---------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/snssais")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllSnssais() throws Exception
	{
		final ArrayNode snssais = (ArrayNode) this.service.doMethodGet("/v1/snssais");
		return ControllerUtil.createResponseJson(snssais);
	}

	@GetMapping(value = "/snssais/{snssaiName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchSnssai(@PathVariable("snssaiName") final String snssaiName) throws Exception
	{
		final JsonNode snssai = this.service.doMethodGet("/v1/snssais/" + Encode.forHtml(snssaiName));
		return JsonNodeUtil.sanitize(snssai);
	}

	@PutMapping(value = "/snssais/{snssaiName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceSnssai(@PathVariable("snssaiName") final String snssaiName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/snssais/" + snssaiName, requestJson);
	}

	@DeleteMapping(value = "/snssais/{snssaiName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeSnssai(@PathVariable("snssaiName") final String snssaiName) throws Exception
	{
		this.service.doMethodDetele("/v1/snssais/" + snssaiName);
	}
	// ---[ SNSSAI ]---------------------------------------------------------------------------------------------------[END]

	// ---[ Service Area List ]----------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/serviceAreaLists")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllServiceAreaLists() throws Exception
	{
		final ArrayNode serviceAreaLists = (ArrayNode) this.service.doMethodGet("/v1/serviceAreaLists");
		return ControllerUtil.createResponseJson(serviceAreaLists);
	}

	@GetMapping(value = "/serviceAreaLists/{serviceAreaListName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchServiceAreaList(@PathVariable("serviceAreaListName") final String serviceAreaListName) throws Exception
	{
		final JsonNode serviceAreaList = this.service.doMethodGet("/v1/serviceAreaLists/" + Encode.forHtml(serviceAreaListName));
		return JsonNodeUtil.sanitize(serviceAreaList);
	}

	@PutMapping(value = "/serviceAreaLists/{serviceAreaListName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceServiceAreaList(@PathVariable("serviceAreaListName") final String serviceAreaListName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/serviceAreaLists/" + serviceAreaListName, requestJson);
	}

	@DeleteMapping(value = "/serviceAreaLists/{serviceAreaListName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeServiceAreaList(@PathVariable("serviceAreaListName") final String serviceAreaListName) throws Exception
	{
		this.service.doMethodDetele("/v1/serviceAreaLists/" + serviceAreaListName);
	}

	// ---[ Service Area List ]----------------------------------------------------------------------------------------[END]

	// ---[ Service Area ]---------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/serviceAreas")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllServiceAreas() throws Exception
	{
		final ArrayNode serviceAreas = (ArrayNode) this.service.doMethodGet("/v1/serviceAreas");
		return ControllerUtil.createResponseJson(serviceAreas);
	}

	@GetMapping(value = "/serviceAreas/{serviceAreaName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchServiceArea(@PathVariable("serviceAreaName") final String serviceAreaName) throws Exception
	{
		final JsonNode serviceArea = this.service.doMethodGet("/v1/serviceAreas/" + Encode.forHtml(serviceAreaName));
		return JsonNodeUtil.sanitize(serviceArea);
	}

	@PutMapping(value = "/serviceAreas/{serviceAreaName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceServiceArea(@PathVariable("serviceAreaName") final String serviceAreaName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/serviceAreas/" + serviceAreaName, requestJson);
	}

	@DeleteMapping(value = "/serviceAreas/{serviceAreaName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeServiceArea(@PathVariable("serviceAreaName") final String serviceAreaName) throws Exception
	{
		this.service.doMethodDetele("/v1/serviceAreas/" + serviceAreaName);
	}

	// ---[ Service Area ]---------------------------------------------------------------------------------------------[END]

	// ---[ TAI List ]-------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/taiLists")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllTaiLists() throws Exception
	{
		final ArrayNode taiLists = (ArrayNode) this.service.doMethodGet("/v1/taiLists");
		return ControllerUtil.createResponseJson(taiLists);
	}

	@GetMapping(value = "/taiLists/{taiListName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchTaiList(@PathVariable("taiListName") final String taiListName) throws Exception
	{
		final JsonNode taiList = this.service.doMethodGet("/v1/taiLists/" + Encode.forHtml(taiListName));
		return JsonNodeUtil.sanitize(taiList);
	}

	@PutMapping(value = "/taiLists/{taiListName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceTaiList(@PathVariable("taiListName") final String taiListName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/taiLists/" + taiListName, requestJson);
	}

	@DeleteMapping(value = "/taiLists/{taiListName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeTaiList(@PathVariable("taiListName") final String taiListName) throws Exception
	{
		this.service.doMethodDetele("/v1/taiLists/" + taiListName);
	}

	// ---[ TAI List ]-------------------------------------------------------------------------------------------------[END]

	// ---[ TAI ]------------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/tais")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllTais() throws Exception
	{
		final ArrayNode tais = (ArrayNode) this.service.doMethodGet("/v1/tais");
		return ControllerUtil.createResponseJson(tais);
	}

	@GetMapping(value = "/tais/{taiName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchTai(@PathVariable("taiName") final String taiName) throws Exception
	{
		final JsonNode tai = this.service.doMethodGet("/v1/tais/" + Encode.forHtml(taiName));
		return JsonNodeUtil.sanitize(tai);
	}

	@PutMapping(value = "/tais/{taiName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void ReplaceTai(@PathVariable("taiName") final String taiName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/tais/" + taiName, requestJson);
	}

	@DeleteMapping(value = "/tais/{taiName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeTai(@PathVariable("taiName") final String taiName) throws Exception
	{
		this.service.doMethodDetele("/v1/tais/" + taiName);
	}
	// ---[ TAI ]------------------------------------------------------------------------------------------------------[END]

	// ---[ DNN List ]-------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/dnnLists")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllDnnLists() throws Exception
	{
		final ArrayNode dnnLists = (ArrayNode) this.service.doMethodGet("/v1/dnnLists");
		return ControllerUtil.createResponseJson(dnnLists);
	}

	@GetMapping(value = "/dnnLists/{dnnListName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchDnnList(@PathVariable("dnnListName") final String dnnListName) throws Exception
	{
		final JsonNode dnnList = this.service.doMethodGet("/v1/dnnLists/" + Encode.forHtml(dnnListName));
		return JsonNodeUtil.sanitize(dnnList);
	}

	@PutMapping(value = "/dnnLists/{dnnListName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceDnnList(@PathVariable("dnnListName") final String dnnListName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/dnnLists/" + dnnListName, requestJson);
	}

	@DeleteMapping(value = "/dnnLists/{dnnListName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeDnnList(@PathVariable("dnnListName") final String dnnListName) throws Exception
	{
		this.service.doMethodDetele("/v1/dnnLists/" + dnnListName);
	}
	// ---[ DNN List ]-------------------------------------------------------------------------------------------------[END]

	// ---[ DNN ]------------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/dnns")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllDnns() throws Exception
	{
		final ArrayNode dnns = (ArrayNode) this.service.doMethodGet("/v1/dnns");
		return ControllerUtil.createResponseJson(dnns);
	}

	@GetMapping(value = "/dnns/{dnnName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchDnn(@PathVariable("dnnName") final String dnnName) throws Exception
	{
		final JsonNode dnn = this.service.doMethodGet("/v1/dnns/" + Encode.forHtml(dnnName));
		return JsonNodeUtil.sanitize(dnn);
	}

	@PutMapping(value = "/dnns/{dnnName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceDnn(@PathVariable("dnnName") final String dnnName, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.service.doMethodPut("/v1/dnns/" + dnnName, requestJson);
	}

	@DeleteMapping(value = "/dnns/{dnnName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeDnn(@PathVariable("dnnName") final String dnnName) throws Exception
	{
		this.service.doMethodDetele("/v1/dnns/" + dnnName);
	}
	// ---[ DNN ]------------------------------------------------------------------------------------------------------[END]

	private String fixIsoString(final String isoString)
	{
		final Integer index = isoString.indexOf("+");
		if (index == -1)
		{
			return isoString;
		}

		return isoString.substring(0, index + 3) + ":" + isoString.substring(index + 3);
	}

	// --- [For Saviah]------------------------------------------------------------------------------------------------[START]
	@GetMapping(value = "/ues")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchUes(@RequestParam(name="page",defaultValue = "1") int page,
			@RequestParam(name="size",defaultValue ="10") int pageSize) throws ExceptionBase
	{
		final ArrayNode ues = (ArrayNode) this.service.doMethodGet("/v1/5gc/ues"+"?page=" + page + "&pageSize=" + pageSize);

		ArrayNode filteredArray = objectMapper.createArrayNode();
		for (JsonNode element : ues) {
			if (!element.has("totalCount")) {
				filteredArray.add(element);
			}
		}
		//計算分頁
		int totalCount = ues.path(0).path("totalCount").asInt();
		final ObjectNode pagination = JsonNodeFactory.instance.objectNode();

		int totalPage = totalCount / pageSize;
		if (totalCount % pageSize > 0)
		{
			totalPage += 1;
		}
		pagination.put("pageNumber", page).put("pageSize", ues.size() - 1).put("totalPages", totalPage).put("totalElements", totalCount);


		final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
		final ObjectNode root = jsonNodeFactory.objectNode();
		root.set("content", filteredArray);
		root.set("pagination", pagination);
		return root;
	}

	@GetMapping(value = "/nf/pmMetric")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchNfPmMetric() throws ExceptionBase
	{
		return this.service.doMethodGet("/v1/5gc/nf/pmMetric");
	}

	@GetMapping(value = "/ue/{supi}/connection")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchConnection(@PathVariable("supi") final String supi) throws ExceptionBase
	{
		return this.service.doMethodGet("/v1/5gc/ue/" + supi + "/connection");
	}

	@PostMapping(value = "/ue/{supi}/connection/deregister")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void fetchConnectionDeregister(@PathVariable("supi") final String supi) throws ExceptionBase
	{
		this.service.doMethodPost("/v1/5gc/ue/" + supi + "/connection/deregister",null);
	}

	@PostMapping(value = "/ue/{supi}/connection/reConnect")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void fetchConnectionReConnect(@PathVariable("supi") final String supi) throws ExceptionBase
	{
		this.service.doMethodPost("/v1/5gc/ue/" + supi + "/connection/reConnect", null);
	}

	@GetMapping(value = "/rans")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRans(@RequestParam(name="page",required=false,defaultValue = "1") int page,
			@RequestParam(name="size",required=false,defaultValue = "10") int pageSize) throws ExceptionBase
	{
		final ArrayNode rans = (ArrayNode) this.service.doMethodGet("/v1/5gc/rans");
		return ControllerUtil.createResponseJson(rans, page, pageSize);
	}


	@PostMapping(value = "/ue/subscriptions/csv")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void importSubscriptionsCsv(@RequestBody String body) throws ExceptionBase
	{
		this.service.doMethodPostString("/v1/5gc/ue/subscriptions/csv", body);
	}

	@GetMapping(value = "/ue/subscriptions/csv")
	@ResponseStatus(HttpStatus.OK)
	public String exportSubscriptionsCsv() throws ExceptionBase
	{
		return this.service.doMethodGetString("/v1/5gc/ue/subscriptions/csv");
	}


	@GetMapping(value = "/qosRef")
	@ResponseStatus(HttpStatus.OK)
	public ArrayNode fetchQosList() throws ExceptionBase
	{
		return (ArrayNode) this.service.doMethodGet("/v1/5gc/qosRef");
	}

	@GetMapping(value = "/qosRef/{QoSReferenceName}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchQos(@PathVariable("QoSReferenceName") final String QoSReferenceName) throws ExceptionBase
	{
		return this.service.doMethodGet("/v1/5gc/qosRef/" + QoSReferenceName);
	}

	@PutMapping(value = "/qosRef/{QoSReferenceName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyQos(@PathVariable("QoSReferenceName") final String QoSReferenceName,
			@RequestBody final JsonNode requestJson) throws ExceptionBase
	{
		this.service.doMethodPut("/v1/5gc/qosRef/" + QoSReferenceName, requestJson);
	}

	@DeleteMapping(value = "/qosRef/{QoSReferenceName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void deleteQos(@PathVariable("QoSReferenceName") final String QoSReferenceName) throws Exception
	{
		this.service.doMethodDetele("/v1/5gc/qosRef/" + QoSReferenceName);
	}

	@GetMapping(value = "/ue/connectedUe/count")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getConnectUeCount() throws ExceptionBase
	{
		return this.service.doMethodGet("/v1/5gc/ue/connectedUe/count");
	}

	@PostMapping(value = "/cache/clear")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void evictCache() {
		this.service.evictAllCache();
	}

	@GetMapping(value = "/licenses/cms")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getCmsLicenseInfo() throws ExceptionBase
	{
		return this.service.doMethodGet("/v1/licenses/cms");
	}

	@PostMapping(value = "/licenses/cms")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void importCmsLicenseFile(@RequestParam(name = "file", required = true) final MultipartFile multipartFile) throws ExceptionBase
	{
		this.service.doMethodPostFile("/v1/licenses/cms", multipartFile);
	}

	@GetMapping(value = "/license/oam")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getOamLicenseInfo() throws ExceptionBase
	{
		return this.service.doMethodGet("/v1/licenses/oam");
	}

	@PostMapping(value = "/license/oam")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void importOamLicenseFile(@RequestParam(name = "file", required = true) final MultipartFile multipartFile) throws ExceptionBase
	{
		this.service.doMethodPostFile("/v1/licenses/oam", multipartFile);
	}
	// --- [For Saviah]------------------------------------------------------------------------------------------------[END]
}