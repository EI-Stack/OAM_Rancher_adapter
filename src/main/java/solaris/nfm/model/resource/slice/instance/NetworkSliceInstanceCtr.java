package solaris.nfm.model.resource.slice.instance;

import java.util.ArrayList;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.querydsl.core.types.Predicate;

import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.controller.base.ControllerBase;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.exception.EntityIdInvalidException;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.slice.template.NetworkSliceDao;
import solaris.nfm.service.SliceService;

@RestController
@RequestMapping("/v1/sliceInstances")
public class NetworkSliceInstanceCtr extends ControllerBase<NetworkSliceInstance, NetworkSliceInstanceVo, NetworkSliceInstanceDto, NetworkSliceInstanceDto, NetworkSliceInstanceDmo>
{
	@Autowired
	private NetworkSliceDao	networkSliceDao;
	@Autowired
	private SliceService	sliceSrv;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public NetworkSliceInstanceVo fetchNetworkSliceInstance(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<NetworkSliceInstanceVo> fetchAllNetworkSliceInstances(@QuerydslPredicate(root = NetworkSliceInstance.class) final Predicate predicate, final Pageable pageable)
			throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createNetworkSliceInstance(@Validated(NetworkSliceInstanceDto.Create.class) @RequestBody final NetworkSliceInstanceDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 networkSliceId 是否存在
		if (this.networkSliceDao.existsById(dto.getNetworkSliceId()) == false) throw new EntityIdInvalidException("network slice", dto.getNetworkSliceId());
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final NetworkSliceInstance detach = new NetworkSliceInstance();
		BeanUtils.copyProperties(dto, detach);
		detach.setRicSliceList(new ArrayList<String>());
		// 自動配置 RIC Slice
		// 依據 (sst, sd)，(1) 先找 RAN slice 相符者 (2) 檢查目前所有的 instance 是否有使用該 RAN slice
		final Set<String> availableRanSliceSet = this.sliceSrv.getAvailableRanSliceSet(dto.getNetworkSliceId());

		if (availableRanSliceSet.size() == 0) throw new ExceptionBase(400, "There is no available RAN slice.");
		// 原本當沒有可用的 RAN slice 時，應該嘗試去自動生成。但 cell 與覆蓋地點有關，目前不知如何判斷選取，所以這部份保留為手動產生

		// 進行 RAN slice 綁定
		detach.getRicSliceList().add(new ArrayList<>(availableRanSliceSet).get(0));

		final NetworkSliceInstance entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyNetworkSliceInstance(@PathVariable("id") final Long id, @Validated(NetworkSliceInstanceDto.Update.class) @RequestBody final NetworkSliceInstanceDto dto)
			throws Exception, ExceptionBase
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		updateEntity(id, dto);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeNetworkSliceInstance(@PathVariable("id") final Long id) throws Exception
	{
		deleteEntity(id);
	}
}