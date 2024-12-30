package solaris.nfm.model.resource.slice.template;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.types.Predicate;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.controller.base.ControllerBase;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.slice.instance.NetworkSliceInstance;
import solaris.nfm.model.resource.slice.instance.NetworkSliceInstanceDao;
import solaris.nfm.model.resource.slice.instance.NetworkSliceInstanceVo;
import solaris.nfm.service.RicService;
import solaris.nfm.service.SliceService;

@RestController
@RequestMapping("/v1/slices")
@Slf4j
public class NetworkSliceCtr extends ControllerBase<NetworkSlice, NetworkSliceVo, NetworkSliceDto, NetworkSliceDto, NetworkSliceDmo>
{
	@Autowired
	private ObjectMapper			objectMapper;
	@Autowired
	private NetworkSliceDao			dao;
	@Autowired
	private NetworkSliceInstanceDao	networkSliceInstanceDao;
	@Autowired
	private SliceService			sliceSrv;
	@Autowired
	private RicService				ricSrv;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public NetworkSliceVo fetchNetworkSlice(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<NetworkSliceVo> fetchAllNetworkSlices(@QuerydslPredicate(root = NetworkSlice.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createNetworkSlice(@Validated(NetworkSliceDto.Create.class) @RequestBody final NetworkSliceDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 name 是否為唯一值
		if (this.dao.countByName(dto.getName()) > 0) throw new EntityHasExistedException("Network Slice name (" + dto.getName() + ") has existed.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		// Service profile 資料配置
		final JsonNode profile = dto.getProfile();
		final Integer sst = profile.path("sst").asInt();
		final String sd = profile.path("sd").asText();
		final String mcc = profile.path("mcc").asText();
		final String mnc = profile.path("mnc").asText();
		final ObjectNode serviceProfile = this.sliceSrv.createServiceProfile(sst, sd, mcc, mnc);
		serviceProfile.replace("maxNumberofUEs", profile.path("maxNumberofUEs"));
		serviceProfile.replace("latency", profile.path("latency"));
		((ObjectNode) serviceProfile.path("dLThptPerSlice")).setAll((ObjectNode) profile.path("dLThptPerSlice"));
		((ObjectNode) serviceProfile.path("uLThptPerSlice")).setAll((ObjectNode) profile.path("uLThptPerSlice"));

		log.debug("={}", serviceProfile.toPrettyString());
		final NetworkSlice detach = new NetworkSlice();
		BeanUtils.copyProperties(dto, detach);
		final ArrayNode serviceProfiles = JsonNodeFactory.instance.arrayNode();
		serviceProfiles.add(serviceProfile);
		detach.setServiceProfiles(serviceProfiles);

		final NetworkSlice entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyNetworkSlice(@PathVariable("id") final Long id, @Validated(NetworkSliceDto.Update.class) @RequestBody final NetworkSliceDto dto) throws Exception, ExceptionBase
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		updateEntity(id, dto);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeNetworkSlice(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查是否尚有 instance 存在，若有，則不能刪除
		if (this.networkSliceInstanceDao.countByNetworkSliceId(id) > 0) throw new ExceptionBase(400, "There are some instances existed, the slice is Not allowed to remove.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		deleteEntity(id);
	}

	/**
	 * 取得從屬於該 network slice 的 instance 清單
	 */
	@GetMapping(value = "/{id}/instances")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<NetworkSliceInstanceVo> fetchInstancesOfNetworkSlice(@PathVariable("id") final Long id, final Pageable pageable) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Page<NetworkSliceInstance> entityPage = this.networkSliceInstanceDao.findByNetworkSliceId(id, pageable);
		final List<NetworkSliceInstanceVo> voList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
		for (final NetworkSliceInstance entity : entityPage)
		{
			final NetworkSliceInstanceVo vo = new NetworkSliceInstanceVo();
			BeanUtils.copyProperties(entity, vo);
			voList.add(vo);
		}

		return new RestResultDto<>(voList, new PaginationDto(entityPage));
	}

	/**
	 * 取得從屬於該 network slice 的可用 RAN slice ID 清單
	 */
	@GetMapping(value = "/{id}/availableRanSliceIds")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRanSlicesOfNetworkSlice(@PathVariable("id") final Long id, final Pageable pageable) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Set<String> availableRanSliceSet = this.sliceSrv.getAvailableRanSliceSet(id);
		final ArrayNode availableRanSlices = this.objectMapper.valueToTree(availableRanSliceSet);
		return ControllerUtil.createResponseJson(availableRanSlices);
	}

	/**
	 * 依據 network slice，新增 RAN slice
	 */
	@PostMapping(value = "/{id}/ranSlices")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createRanSliceOfNetworkSlice(@PathVariable("id") final Long id, @Validated(NetworkSliceDto.Create.class) @RequestBody final JsonNode requestJson)
			throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final NetworkSlice networkSlice = this.dao.getReferenceById(id);
		final JsonNode serviceProfileS = networkSlice.getServiceProfiles().path(0);
		final JsonNode snssaiS = serviceProfileS.path("pLMNInfoList").path(0).path("snssai");
		final Integer sst = snssaiS.path("sst").asInt();
		final String sd = snssaiS.path("sd").asText();

		final String ranSliceId = UUID.randomUUID().toString();
		final ObjectNode sliceRoot = JsonNodeFactory.instance.objectNode();
		sliceRoot.put("networkSliceId", ranSliceId);
		final ArrayNode sliceprofiles = sliceRoot.putArray("serviceProfileList");
		sliceprofiles.add(this.sliceSrv.createRanSliceProfile(ranSliceId, sst, sd));

		final ObjectNode sliceProfile = (ObjectNode) sliceRoot.path("serviceProfileList").path(0);
		sliceProfile.set("maxNumberofUEs", serviceProfileS.path("maxNumberofUEs"));
		sliceProfile.set("latency", serviceProfileS.path("latency"));
		final ObjectNode dLThptPerSlice = (ObjectNode) sliceProfile.path("dLThptPerSlice");
		dLThptPerSlice.set("maxThpt", serviceProfileS.path("dLThptPerSlice").path("maxThpt"));
		dLThptPerSlice.set("guaThpt", serviceProfileS.path("dLThptPerSlice").path("guaThpt"));
		final ObjectNode uLThptPerSlice = (ObjectNode) sliceProfile.path("uLThptPerSlice");
		uLThptPerSlice.set("maxThpt", serviceProfileS.path("uLThptPerSlice").path("maxThpt"));
		uLThptPerSlice.set("guaThpt", serviceProfileS.path("uLThptPerSlice").path("guaThpt"));

		final ArrayNode cells = (ArrayNode) requestJson.path("cells");
		final Set<String> nciSet = new LinkedHashSet<>();
		// cell 的套用狀態為 是 的清單
		final Set<String> applingNciSet = new LinkedHashSet<>();
		cells.forEach(cell ->
		{
			nciSet.add(cell.path("nci").asText());
			if (cell.path("status").asText().equalsIgnoreCase("up")) applingNciSet.add(cell.path("nci").asText());
		});

		log.debug("={}", sliceProfile.toPrettyString());

		// 檢查 cell 是否合法與存在
		this.sliceSrv.checkNci(nciSet, null);

		// 執行創建 RIC slice
		log.debug("sliceRoot \n{}", sliceRoot.toPrettyString());
		this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ranSliceId + "/bindRicSlice", sliceRoot);

		// 執行新增 cell
		log.debug("執行新增 cell {}", nciSet);
		if (nciSet.size() > 0) this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ranSliceId + "/bindSliceTarget", this.sliceSrv.createArrayNodeForNci(nciSet));

		// 執行將 cell 的套用狀態改為 是
		log.debug("執行將 cell 的套用狀態改為 是 {}", applingNciSet);
		if (applingNciSet.size() > 0) this.ricSrv.doMethodPost("/v1/networkSliceInstances/" + ranSliceId + "/activateRicSlice", this.sliceSrv.createArrayNodeForNci(applingNciSet));

		return JsonNodeFactory.instance.objectNode().put("id", ranSliceId);
	}

}