package solaris.nfm.controller;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.JsonValid;
import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.service.FgcService;
import solaris.nfm.service.RicService;

@RestController
@RequestMapping("/v1")
@Validated
@Slf4j
public class SliceCtr
{
	@Autowired
	protected ObjectMapper	objectMapper;
	@Autowired
	private RicService		ricSrv;
	@Autowired
	private FgcService service;

	// ================================================================================================================
	// Cell
	// ================================================================================================================
	@GetMapping(value = "/cell/ncis")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllCells(final Pageable pageable) throws Exception
	{
		final JsonNode source = this.ricSrv.doMethodGet("/v1/privateAPI/getCellList");
		final ArrayNode ncis = JsonNodeFactory.instance.arrayNode();
		ncis.addAll((ArrayNode) source.path("NCI"));
		return ControllerUtil.createResponseJson(ncis);
	}

	@GetMapping(value = "/cells/{nci}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchCell(@PathVariable("nci") final String nci) throws Exception
	{
		// 檢查 nci 是否存在
		final JsonNode source = this.ricSrv.doMethodGet("/v1/privateAPI/getCellList");
		final ArrayNode sourceNcis = (ArrayNode) source.path("NCI");
		final Set<String> sourceNciSet = this.objectMapper.readValue(sourceNcis.traverse(), new TypeReference<LinkedHashSet<String>>()
		{});
		if (sourceNciSet.contains(nci) == false) throw new ExceptionBase(404, "NCI (" + nci + ") is invalid, it should be one of set " + sourceNciSet);

		final ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
		final ArrayNode ncis = requestJson.putArray("NCI");
		ncis.add(nci);

		final ArrayNode cells = (ArrayNode) this.ricSrv.doMethodPost("/v1/privateAPI/getCellInfo", requestJson);
		return cells.path(0);
	}

	// ================================================================================================================
	// RAN Slice Resource Monitor
	// ================================================================================================================
	@GetMapping(value = "/ricSlices/{ricSliceId}/resourceMonitor")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchResourceMonitorInfo(@PathVariable("ricSliceId") final Integer ricSliceId) throws Exception
	{
		ricSrv.checkRicSliceId(ricSliceId);
		return ricSrv.doMethodGet("/v1/networkSliceInstances/" + ricSliceId + "/getMonitorInfo");
	}

	@PutMapping(value = "/ricSlices/{ricSliceId}/resourceMonitor")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@JsonValid("ric-slice.yaml#/ResourceMonitor")
	@OperationLog
	public void activeResourceMonitor(@PathVariable("ricSliceId") final Integer ricSliceId, @RequestBody final JsonNode requestJson) throws Exception
	{
		ricSrv.checkRicSliceId(ricSliceId);
		ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/activeResourceMonitor", requestJson);
	}

	@DeleteMapping(value = "/ricSlices/{ricSliceId}/resourceMonitor")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void terminateResourceMonitor(@PathVariable("ricSliceId") final Integer ricSliceId) throws Exception
	{
		ricSrv.checkRicSliceId(ricSliceId);
		ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/terminateResourceMonitor", null);
	}

	// ================================================================================================================
	// RAN Slice
	// ================================================================================================================
	@GetMapping(value = "/ricSlices")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllRicSlices() throws Exception
	{
		final ArrayNode ricSlices = this.ricSrv.getRicSlices();
		return ControllerUtil.createResponseJson(ricSlices);
	}

	@GetMapping(value = "/ricSlices/{ricSliceId}")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRicSlice(@PathVariable("ricSliceId") final Integer ricSliceId) throws Exception
	{
		ricSrv.checkRicSliceId(ricSliceId);
		return this.ricSrv.getRicSlice(ricSliceId);
	}

	@PostMapping(value = "/ricSlices")
	@ResponseStatus(HttpStatus.CREATED)
//	@JsonValid("ric-slice.yaml#/RicSliceCreation")
	@OperationLog
	public JsonNode createRicSlice(@RequestBody final JsonNode requestJson) throws Exception
	{
		// 驗證 ricSliceId 是否是可用的
		final Integer ricSliceId = requestJson.path("ricSliceId").asInt();
		final Set<Integer> ricSliceIds = ricSrv.getRicSliceIds();
		if (ricSliceIds.contains(ricSliceId)) throw new ExceptionBase("RicSliceId (" + ricSliceId + ") has existed.");

		// 檢查 cell 是否合法與存在
		final ArrayNode cells = (ArrayNode) requestJson.path("cells");
		if (cells.size() == 0) throw new ExceptionBase("The number of cells must be greater than 0");
		ricSrv.checkNci(cells);

		// 執行創建 RIC slice
		final JsonNode sliceContent = requestJson.path("sliceContent");
		log.debug("執行創建 RIC slice \n{}", sliceContent.toPrettyString());
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindRicSlice", sliceContent);

		// 執行綁定 cell
		log.debug("執行綁定 cell {}", cells.toPrettyString());
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindSliceTarget", cells);

		// 執行將 cell 的套用狀態改為 是
		log.debug("Apply to DU");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/activateRicSlice", null);

		return JsonNodeFactory.instance.objectNode().put("id", ricSliceId);
	}

	@DeleteMapping(value = "/ricSlices/{ricSliceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeRicSlice(@PathVariable("ricSliceId") final Integer ricSliceId) throws Exception
	{
		this.ricSrv.checkRicSliceId(ricSliceId);

		final JsonNode ricSlice = ricSrv.getRicSlice(ricSliceId);
		if (ricSlice.path("activestate").asText().equals("Active"))
		{
			// 執行將 cell 的套用狀態改為 否
			log.debug("執行將 cell 的套用狀態改為 否");
			this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/terminateRicSlice", null);
		}

		// 執行刪除 cell
		log.debug("執行刪除已綁定的 cell");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindSliceTarget", JsonNodeFactory.instance.arrayNode());

		// 執行刪除 RIC slice
		log.debug("執行刪除 RIC slice");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/unbindRicSlice", null);
	}

	/**
	 * 執行重新綁定 cell set
	 */
	@PutMapping(value = "/ricSlices/{ricSliceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@JsonValid("ric-slice.yaml#/RicSliceModification")
	@OperationLog
	public JsonNode modifyRicSlice(@PathVariable("ricSliceId") final Integer ricSliceId, @RequestBody final JsonNode requestJson) throws Exception
	{
		this.ricSrv.checkRicSliceId(ricSliceId);
		// 檢查 cell 是否合法與存在
		final ArrayNode cells = (ArrayNode) requestJson.path("cells");
		if (cells.size() == 0) throw new ExceptionBase("The number of cells must be greater than 0");
		this.ricSrv.checkNci(cells);

		final JsonNode ricSlice = ricSrv.getRicSlice(ricSliceId);
		if (ricSlice.path("activestate").asText().equals("Active"))
		{
			// 執行將 cell 的套用狀態改為 否
			log.debug("執行將 cell 的套用狀態改為 否");
			this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/terminateRicSlice", null);
		}

		// 執行重新綁定 cell set
		log.debug("執行重新綁定 cell {}", cells.toPrettyString());
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindSliceTarget", cells);

		// 執行將 cell 的套用狀態改為 是
		log.debug("Apply to DU");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/activateRicSlice", null);

		return null;
	}

	private JsonNode createServiceProfile(final String ricSliceId, final Integer sst, final String sd)
	{
		final ObjectNode serviceProfile = JsonNodeFactory.instance.objectNode();
		serviceProfile.put("serviceProfileId", "cn=" + ricSliceId);
		serviceProfile.put("description", "");  // #1
		serviceProfile.put("maxNumberofUEs", 100);
		serviceProfile.put("coverageArea", 0);
		serviceProfile.put("latency", 30);
		serviceProfile.put("uEMobilityLevel", "stationary");
		serviceProfile.put("resourceSharingLevel", "not_shared");
		serviceProfile.put("resourceSharingGroup", 1);
		serviceProfile.put("sST", sst);
		serviceProfile.put("availability", 60.1);
		serviceProfile.put("activityFactor", 60.1);

		// serviceProfile.put("uESpeed", 3);
		// serviceProfile.put("jitter", 10);
		// serviceProfile.put("survivalTime", "2021-10-22T02:37:50.302Z");
		// serviceProfile.put("reliability", "string");
		// serviceProfile.put("maxDLDataVolume", "string");
		// serviceProfile.put("maxULDataVolume", "string");

		// final ArrayNode snssaiList = serviceProfile.putArray("sNSSAIList");
		// final ObjectNode snssai = JsonNodeFactory.instance.objectNode();
		// snssai.put("idx", 1);
		// snssai.put("sst", sst);
		// snssai.put("sd", sd);
		// snssaiList.add(snssai);

		// final ArrayNode plmnIdList = serviceProfile.putArray("pLMNIdList");
		// final ObjectNode plmnId = JsonNodeFactory.instance.objectNode();
		// plmnId.put("mcc", "466");
		// plmnId.put("mnc", "93");
		// plmnIdList.add(plmnId);

		// final ObjectNode delayTolerance = serviceProfile.putObject("delayTolerance");
		// delayTolerance.put("support", "NOT_SUPPORTED");
		// ObjectNode servAttrCom = delayTolerance.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		// final ObjectNode deterministicComm = serviceProfile.putObject("deterministicComm");
		// deterministicComm.put("availability", "NOT_SUPPORTED");
		// servAttrCom = deterministicComm.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");
		// final ArrayNode periodicityList = deterministicComm.putArray("periodicityList");
		// periodicityList.add(0);

		final ObjectNode dLThptPerSlice = serviceProfile.putObject("dLThptPerSlice");
		dLThptPerSlice.put("guaThpt", 500000);
		dLThptPerSlice.put("maxThpt", 1000000);
		// servAttrCom = dLThptPerSlice.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		final ObjectNode dLThptPerUE = serviceProfile.putObject("dLThptPerUE");
		dLThptPerUE.put("guaThpt", 20000);
		dLThptPerUE.put("maxThpt", 50000);
		// servAttrCom = dLThptPerUE.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		final ObjectNode uLThptPerSlice = serviceProfile.putObject("uLThptPerSlice");
		uLThptPerSlice.put("guaThpt", 200000);
		uLThptPerSlice.put("maxThpt", 500000);
		// servAttrCom = uLThptPerSlice.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		final ObjectNode uLThptPerUE = serviceProfile.putObject("uLThptPerUE");
		uLThptPerUE.put("guaThpt", 5000);
		uLThptPerUE.put("maxThpt", 10000);
		// servAttrCom = uLThptPerUE.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		// final ObjectNode maxPktSize = serviceProfile.putObject("maxPktSize");
		// maxPktSize.put("maxSize", 512);
		// servAttrCom = maxPktSize.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		final ObjectNode maxNumberofPDUSessions = serviceProfile.putObject("maxNumberofPDUSessions");
		maxNumberofPDUSessions.put("nOofPDUSessions", 512);
		// servAttrCom = maxNumberofPDUSessions.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		// final ObjectNode kPIMonitoring = serviceProfile.putObject("kPIMonitoring");
		// servAttrCom = kPIMonitoring.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");
		// final ArrayNode kPIList = kPIMonitoring.putArray("kPIList");
		// kPIList.add("string");

		// final ObjectNode userMgmtOpen = serviceProfile.putObject("userMgmtOpen");
		// userMgmtOpen.put("support", "NOT_SUPPORTED");
		// servAttrCom = userMgmtOpen.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		// final ObjectNode v2XCommModels = serviceProfile.putObject("v2XCommModels");
		// v2XCommModels.put("v2XMode", "NOT_SUPPORTED");
		// servAttrCom = v2XCommModels.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		// final ObjectNode termDensity = serviceProfile.putObject("termDensity");
		// termDensity.put("density", 0);
		// servAttrCom = termDensity.putObject("servAttrCom");
		// servAttrCom.put("category", "character");
		// servAttrCom.put("tagging", "performance");
		// servAttrCom.put("exposure", "KPI");

		return serviceProfile;
	}


	//-----------------------combine affirmed slice and smo slice-------------------------

	@PostMapping("/combineSlices")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createRicSlices(@RequestBody final JsonNode requestJson) throws Exception {

		//affirmed
		try {
			this.service.doMethodPut("/v1/snssais/" + requestJson.path("name").asText(), requestJson);
		} catch (Exception e){
			throw new ExceptionBase("create affirmed slice failed.");
		}


		//smo
		// 驗證 ricSliceId 是否是可用的
		final Integer ricSliceId = requestJson.path("ricSliceId").asInt();
		final Set<Integer> ricSliceIds = ricSrv.getRicSliceIds();
		if (ricSliceIds.contains(ricSliceId)) throw new ExceptionBase("RicSliceId (" + ricSliceId + ") has existed.");

		// 檢查 cell 是否合法與存在
		final ArrayNode cells = (ArrayNode) requestJson.path("cells");
		if (cells.size() == 0) throw new ExceptionBase("The number of cells must be greater than 0");
		ricSrv.checkNci(cells);

		// 執行創建 RIC slice
		final JsonNode sliceContent = requestJson.path("sliceContent");
		log.debug("執行創建 RIC slice \n{}", sliceContent.toPrettyString());
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindRicSlice", sliceContent);

		// 執行綁定 cell
		log.debug("執行綁定 cell {}", cells.toPrettyString());
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindSliceTarget", cells);

		// 執行將 cell 的套用狀態改為 是
		log.debug("Apply to DU");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/activateRicSlice", null);

		return JsonNodeFactory.instance.objectNode().put("id", ricSliceId);
	}


	@DeleteMapping(value = "/combineSlices/sliceName/{snssaiName}/sliceId/{ricSliceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeSlices(@PathVariable("snssaiName") final String snssaiName,
							 @PathVariable("ricSliceId") final Integer ricSliceId) throws Exception
	{
		//affirmed
		try {
			this.service.doMethodDetele("/v1/snssais/" + snssaiName);
		} catch (Exception e){
			throw new ExceptionBase("delete affirmed slice failed.");
		}


		//smo
		this.ricSrv.checkRicSliceId(ricSliceId);

		final JsonNode ricSlice = ricSrv.getRicSlice(ricSliceId);
		if (ricSlice.path("activestate").asText().equals("Active"))
		{
			// 執行將 cell 的套用狀態改為 否
			log.debug("執行將 cell 的套用狀態改為 否");
			this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/terminateRicSlice", null);
		}

		// 執行刪除 cell
		log.debug("執行刪除已綁定的 cell");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/bindSliceTarget", JsonNodeFactory.instance.arrayNode());

		// 執行刪除 RIC slice
		log.debug("執行刪除 RIC slice");
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/unbindRicSlice", null);
	}

}
