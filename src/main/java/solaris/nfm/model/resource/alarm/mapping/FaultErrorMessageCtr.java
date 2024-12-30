package solaris.nfm.model.resource.alarm.mapping;

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
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.util.RegexUtil;

@RestController
@RequestMapping("/v1/faultErrorMessages")
public class FaultErrorMessageCtr extends ControllerBase<FaultErrorMessage, FaultErrorMessageVo, FaultErrorMessageDto, FaultErrorMessageDto, FaultErrorMessageDmo>
{
	@Autowired
	private FaultErrorMessageDao dao;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public FaultErrorMessageVo fetchFaultErrorMessage(@PathVariable("id") final Long id) throws Exception
	{
		this.dmo.getOne(id);

		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<FaultErrorMessageVo> fetchAllFaultErrorMessages(@QuerydslPredicate(root = FaultErrorMessage.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createFaultErrorMessage(@Validated(FaultErrorMessageDto.Create.class) @RequestBody final FaultErrorMessageDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 name 是否為唯一值
		if (this.dao.countByNetworkTypeAndCode(dto.getNetworkType(), dto.getCode()) > 0)
			throw new EntityHasExistedException("Error message (" + dto.getNetworkType() + ", " + dto.getCode() + ") has existed.");
		checkMailAddress(dto);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final FaultErrorMessage detach = new FaultErrorMessage();
		BeanUtils.copyProperties(dto, detach);
		detach.setMailAddresses(new ArrayList<>(dto.getMailAddresses()));

		final FaultErrorMessage entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyFaultErrorMessage(@PathVariable("id") final Long id, @Validated(FaultErrorMessageDto.Update.class) @RequestBody final FaultErrorMessageDto dto) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		dmo.checkOneById(id);
		checkMailAddress(dto);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final FaultErrorMessage fem = this.dao.getReferenceById(id);
		BeanUtils.copyProperties(dto, fem, getNullPropertyNames(dto));
		if (dto.getMailAddresses() != null) fem.setMailAddresses(new ArrayList<>(dto.getMailAddresses()));

		this.dmo.modifyOne(fem);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeFaultErrorMessage(@PathVariable("id") final Long id) throws Exception
	{
		deleteEntity(id);
	}

	private void checkMailAddress(final FaultErrorMessageDto dto) throws Exception, ExceptionBase
	{
		final Set<String> mailAddresses = dto.getMailAddresses();
		if (mailAddresses == null) return;

		for (final String mailAddress : mailAddresses)
			RegexUtil.checkMailAddress(mailAddress);
	}
}