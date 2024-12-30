package solaris.nfm.model.resource.statistic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.querydsl.core.types.Predicate;

import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.controller.dto.PaginationDto;
import solaris.nfm.controller.dto.RestResultDto;
import solaris.nfm.model.resource.alarm.fault.fgc.FaultAlarm;

@RestController
@RequestMapping("/v1/statistics")
public class StatisticCtr
{
	@Autowired
	private StatisticDao	dao;
	@Autowired
	private StatisticDmo	dmo;

	@GetMapping(value = "/{mgtType}/{dataType}")
	@ResponseStatus(HttpStatus.OK)
	public RestResultDto<StatisticVo> fetchAllStatistics(@PathVariable("mgtType") final String mgtType, @PathVariable("dataType") final String dataType,
			@QuerydslPredicate(root = FaultAlarm.class) Predicate predicate, final Pageable pageable, @RequestParam final MultiValueMap<String, String> parameters) throws Exception
	{
		if (parameters.get("date1") != null && parameters.get("date2").get(0) != null)
		{
			final LocalDate date1 = LocalDate.parse(parameters.get("date1").get(0));
			final LocalDate date2 = LocalDate.parse(parameters.get("date2").get(0));
			predicate = QStatistic.statistic.date.between(date1, date2).and(predicate);
		}

		final Page<Statistic> entityPage = this.dao.findAll(predicate, pageable);
		final List<StatisticVo> voList = new ArrayList<>(Math.toIntExact(entityPage.getTotalElements()));
		for (final Statistic entity : entityPage)
		{
			final StatisticVo vo = new StatisticVo();
			vo.setDate(entity.getDate());
			vo.setData(entity.getData().path(mgtType).get(dataType));
			voList.add(vo);
		}

		// 建立並回傳分頁資料集合
		return new RestResultDto<>(voList, new PaginationDto(entityPage));
	}

	@PostMapping(value = "")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public JsonNode createStatistic(@RequestBody final JsonNode json) throws SecurityException, Exception
	{
		// default, ISO_LOCAL_DATE 2016-08-16
		final LocalDate localDate = LocalDate.parse(json.path("date").asText());
		final JsonNode data = json.path("data");

		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
		// if (this.dao.countByDate(localDate) > 0) throw new ExceptionBase(400, "Statistic (date=" + localDate.toString() + ") already existed.");
		if (this.dao.countByDate(localDate) > 0) this.dmo.removeAllByDate(localDate);
		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]

		final Statistic detach = new Statistic();
		detach.setDate(localDate);
		detach.setData(data);

		final Statistic entity = this.dmo.createOne(detach);
		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
	}
}
