package solaris.nfm.model.resource.alarm.performance.rule;

import java.util.ArrayList;
import java.util.List;
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
import solaris.nfm.model.base.domain.FaultAlarmBase.NetworkType;
import solaris.nfm.util.RegexUtil;

/**
 * @author Holisun Wu
 */
@RestController
@RequestMapping("/v1/performanceRules")
public class PerformanceRuleCtr extends ControllerBase<PerformanceRule, PerformanceRuleVo, PerformanceRuleDto, PerformanceRuleDto, PerformanceRuleDmo>
{
	@Autowired
	private PerformanceRuleDao			dao;
	@Autowired
	private PerformanceRuleNameDefBean	pmRuleNameDefBean;

	@GetMapping(value = "/ruleNameList/{networkType}")
	@ResponseStatus(HttpStatus.OK)
	public List<String> fetchPerformanceRule(@PathVariable("networkType") final NetworkType networkType) throws Exception
	{
		return this.pmRuleNameDefBean.getNameList().get(networkType.toString());
	}

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public PerformanceRuleVo fetchPerformanceRule(@PathVariable("id") final Long id) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		return findEntity(id);
	}

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<PerformanceRuleVo> fetchAllPerformanceRules(@QuerydslPredicate(root = PerformanceRule.class) final Predicate predicate, final Pageable pageable) throws Exception
	{
		return findAllEntity(predicate, pageable);
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createPerformanceRule(@Validated(PerformanceRuleDto.Create.class) @RequestBody final PerformanceRuleDto dto) throws SecurityException, Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// 檢查 rule name 是否在網元的白名單內
		checkRuleName(dto);
		checkMailAddress(dto);
		// 檢查是否為唯一值
		if (this.dao.countByNetworkTypeAndName(dto.getNetworkType(), dto.getName()) > 0)
			throw new EntityHasExistedException("Performance rule (" + dto.getNetworkType() + ", " + dto.getName() + ") has existed.");
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final PerformanceRule detach = new PerformanceRule();
		BeanUtils.copyProperties(dto, detach);
		detach.setMailAddresses(new ArrayList<>(dto.getMailAddresses()));

		final PerformanceRule entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void modifyPerformanceRule(@PathVariable("id") final Long id, @Validated(PerformanceRuleDto.Update.class) @RequestBody final PerformanceRuleDto dto) throws Exception
	{
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		this.dmo.checkOneById(id);
		checkMailAddress(dto);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final PerformanceRule rule = this.dao.getReferenceById(id);
		BeanUtils.copyProperties(dto, rule, getNullPropertyNames(dto));
		if (dto.getMailAddresses() != null) rule.setMailAddresses(new ArrayList<>(dto.getMailAddresses()));

		this.dmo.modifyOne(rule);
	}

	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void removePerformanceRule(@PathVariable("id") final Long id) throws Exception
	{
		deleteEntity(id);
	}

	private void checkMailAddress(final PerformanceRuleDto dto) throws Exception, ExceptionBase
	{
		final Set<String> mailAddresses = dto.getMailAddresses();
		if (mailAddresses == null) return;

		for (final String mailAddress : mailAddresses)
			RegexUtil.checkMailAddress(mailAddress);
	}

	/**
	 * 檢查 rule name 是否在網元的白名單內
	 */
	private void checkRuleName(final PerformanceRuleDto dto) throws ExceptionBase
	{
		final NetworkType networkType = NetworkType.valueOf(dto.getNetworkType().toString());

		switch (networkType)
		{
			case fgc :
				if (this.pmRuleNameDefBean.getNameList().get(dto.getNetworkType().toString()).contains(dto.getName()) == false)
					throw new ExceptionBase(400, "5GC Performance rule name (" + dto.getName() + ") is not allowed.");
				break;
			case ric :
				final String measurementObject = dto.getName().substring(dto.getName().indexOf("-") + 1);
				if (this.pmRuleNameDefBean.getNameList().get("ric").contains(measurementObject) == false)
					throw new ExceptionBase(400, "RIC Performance rule name (" + dto.getName() + ") is not allowed.");
				break;
			default :
				break;
		}
	}
}