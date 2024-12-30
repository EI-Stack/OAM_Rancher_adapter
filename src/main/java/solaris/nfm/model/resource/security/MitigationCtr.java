package solaris.nfm.model.resource.security;

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

@RestController
@RequestMapping("/v1/mitigations")
public class MitigationCtr extends ControllerBase<Mitigation, MitigationVo, MitigationDto, MitigationDto, MitigationDmo>
{
	@Autowired
	private MitigationDao dao;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public MitigationVo fetchMitigation(@PathVariable("id") final Long id) throws Exception
	{
		this.dmo.getOne(id);

		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<MitigationVo> fetchAllMitigations(@QuerydslPredicate(root = Mitigation.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createMitigation(@Validated(MitigationDto.Create.class) @RequestBody final MitigationDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 name 是否為唯一值
		if (this.dao.countByName(dto.getName()) > 0) throw new EntityHasExistedException("Mitigation (" + dto.getName() + ") has existed.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Mitigation detach = new Mitigation();
		BeanUtils.copyProperties(dto, detach);

		final Mitigation entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyMitigation(@PathVariable("id") final Long id, @Validated(MitigationDto.Update.class) @RequestBody final MitigationDto dto) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Mitigation entity = this.dao.getReferenceById(id);
		BeanUtils.copyProperties(dto, entity, getNullPropertyNames(dto));

		this.dmo.modifyOne(entity);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeMitigation(@PathVariable("id") final Long id) throws Exception
	{
		deleteEntity(id);
	}
}