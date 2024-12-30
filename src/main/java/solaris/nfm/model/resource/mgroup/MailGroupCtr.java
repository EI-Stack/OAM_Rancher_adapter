package solaris.nfm.model.resource.mgroup;

import java.util.ArrayList;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.querydsl.core.types.Predicate;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.JsonValid;
import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.controller.base.ControllerBase;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.util.RegexUtil;

@RestController
@RequestMapping("/v1/mailGroups")
@Slf4j
public class MailGroupCtr extends ControllerBase<MailGroup, MailGroupVo, MailGroupDto, MailGroupDto, MailGroupDmo>
{
	@Autowired
	private ObjectMapper	objectMapper;
	@Autowired
	private MailGroupDao	dao;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public MailGroupVo fetchMailGroup(@PathVariable("id") final Long id) throws Exception
	{
		this.dmo.getOne(id);

		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<MailGroupVo> fetchAllMailGroups(@QuerydslPredicate(root = MailGroup.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@JsonValid("api.yaml#/createMailGroup")
	@OperationLog
	public JsonNode createMailGroup(@Validated(MailGroupDto.Create.class) @RequestBody final JsonNode requestJson) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		final MailGroupDto dto = objectMapper.treeToValue(requestJson, MailGroupDto.class);
		// 檢查 name 是否為唯一值
		if (this.dao.countByName(dto.getName()) > 0) throw new EntityHasExistedException("Mail Group (" + dto.getName() + ") has existed.");
		for (final String mailAddress : dto.getMailAddresses())
			RegexUtil.checkMailAddress(mailAddress);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final MailGroup detach = new MailGroup();
		BeanUtils.copyProperties(dto, detach);
		detach.setMailAddresses(new ArrayList<>(dto.getMailAddresses()));

		final MailGroup entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyMailGroup(@PathVariable("id") final Long id, @Validated(MailGroupDto.Update.class) @RequestBody final MailGroupDto dto) throws Exception, ExceptionBase
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		dmo.checkOneById(id);
		for (final String mailAddress : dto.getMailAddresses())
			RegexUtil.checkMailAddress(mailAddress);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final MailGroup mailGroup = this.dao.getReferenceById(id);
		BeanUtils.copyProperties(dto, mailGroup, getNullPropertyNames(dto));
		mailGroup.setMailAddresses(new ArrayList<>(dto.getMailAddresses()));

		this.dmo.modifyOne(mailGroup);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeMailGroup(@PathVariable("id") final Long id) throws Exception
	{
		deleteEntity(id);
	}
}
