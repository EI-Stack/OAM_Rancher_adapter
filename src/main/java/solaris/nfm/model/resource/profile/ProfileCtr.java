package solaris.nfm.model.resource.profile;

import java.time.ZonedDateTime;

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

@RestController
@RequestMapping("/v1/profiles")
public class ProfileCtr extends ControllerBase<Profile, ProfileVo, ProfileDto, ProfileDto, ProfileDmo>
{
	@Autowired
	private ProfileDao dao;

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ProfileVo fetchProfile(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<ProfileVo> fetchAllProfiles(@QuerydslPredicate(root = Profile.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createProfile(@Validated(ProfileDto.Create.class) @RequestBody final ProfileDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 name 是否為唯一值
		if (this.dao.countByNameAndType(dto.getName(), dto.getType()) > 0) throw new EntityHasExistedException("Profile  (name=" + dto.getName() + ", type=" + dto.getType() + ") has existed.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Profile detach = new Profile();
		BeanUtils.copyProperties(dto, detach);
		detach.setChangeTime(ZonedDateTime.now());
		final Profile entity = this.dmo.createOne(detach);

		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyProfile(@PathVariable("id") final Long id, @Validated(ProfileDto.Update.class) @RequestBody final ProfileDto dto) throws Exception, ExceptionBase
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		dto.setChangeTime(ZonedDateTime.now());
		updateEntity(id, dto);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removeProfile(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		deleteEntity(id);
	}
}