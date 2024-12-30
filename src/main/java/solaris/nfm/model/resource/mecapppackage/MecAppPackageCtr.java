package solaris.nfm.model.resource.mecapppackage;

import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.controller.base.ControllerBase;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.exception.EntityHasExistedException;

@RestController
@RequestMapping("/v1/mecAppPackages")
@Slf4j
public class MecAppPackageCtr extends ControllerBase<MecAppPackage, MecAppPackageVo, MecAppPackageDto, MecAppPackageDuo, MecAppPackageDmo>
{
	@Autowired
	private MecAppPackageDao dao;
	// @Autowired
	// private MecAppPackageDmo dmo;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public MecAppPackageVo fetchMecAppPackage(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<MecAppPackageVo> fetchAllMecAppPackages(final Pageable pageable) throws Exception
	{
		return findAllEntity(pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	public JsonNode createMecAppPackage(@Validated(MecAppPackageDto.Create.class) @RequestBody final MecAppPackageDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 name 是否為唯一值
		if (this.dao.countByName(dto.getName()) > 0) throw new EntityHasExistedException("MEC app package (name=" + dto.getName() + ") has existed.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final MecAppPackage bean = new MecAppPackage();
		BeanUtils.copyProperties(dto, bean);
		bean.setCreationTime(LocalDateTime.now());
		final MecAppPackage entity = this.dmo.createOne(bean);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void modifyMecAppPackage(@PathVariable("id") final Long id, @Validated(MecAppPackageDto.Create.class) @RequestBody final MecAppPackageDuo dto) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// 檢查 name 是否為唯一值
		if (dto.getName() != null && this.dao.countByName(dto.getName()) > 0) throw new EntityHasExistedException("MEC app package (name=" + dto.getName() + ") has existed.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		updateEntity(id, dto);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMecAppPackage(@PathVariable("id") final Long id) throws Exception
	{
		deleteEntity(id);
	}
}