package solaris.nfm.model.resource.alarm.fault;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.querydsl.core.types.Predicate;

import solaris.nfm.controller.util.ControllerUtil;
import solaris.nfm.model.resource.alarm.fault.fgc.FaultAlarm;
import solaris.nfm.model.resource.alarm.fault.fgc.FaultAlarmDao;

/**
 * @author Holisun Wu
 */
@RestController
@RequestMapping("/v1/fm/alarms")
@Validated
public class FaultAlarmCtr
{
	@Autowired
	private ObjectMapper	objectMapper;
	@Autowired
	private FaultAlarmDao	faultAlarmDao;

	@GetMapping(value = "")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllAlarms(@QuerydslPredicate(root = FaultAlarm.class) final Predicate predicate, @RequestParam final MultiValueMap<String, String> parameters) throws Exception
	{
		final List<FaultAlarm> fgcAlarmList = (List<FaultAlarm>) this.faultAlarmDao.findAll(predicate);
		final ArrayNode fgcAlarms = (ArrayNode) this.objectMapper.valueToTree(fgcAlarmList);
		// final List<MecFaultAlarm> mecAlarmList = (List<MecFaultAlarm>) this.mecFaultAlarmDao.findAll(predicate);
		// final ArrayNode mecAlarms = (ArrayNode) this.objectMapper.valueToTree(mecAlarmList);
		// final List<RicFaultAlarm> ricAlarmList = (List<RicFaultAlarm>) this.ricFaultAlarmDao.findAll(predicate);
		// final ArrayNode ricAlarms = (ArrayNode) this.objectMapper.valueToTree(ricAlarmList);
		// final List<FaultAlarmPhysical> physicalAlarmList = (List<FaultAlarmPhysical>) this.faultAlarmPhysicalDao.findAll(predicate);
		// final ArrayNode physicalAlarms = (ArrayNode) this.objectMapper.valueToTree(physicalAlarmList);
		//
		// fgcAlarms.addAll(mecAlarms);
		//
		// final Map<String, String> fieldMapping = new HashMap<>();
		// ArrayNode fields = null;
		// try
		// {
		// fields = (ArrayNode) this.ricService.getJsonNode("/fields");
		// } catch (final ExceptionBase e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// if (fields == null)
		// {
		// fgcAlarms.addAll(ricAlarms);
		// } else
		// {
		// for (final JsonNode field : fields)
		// {
		// fieldMapping.put(field.path("fieldId").asText(), field.path("fieldName").asText());
		// }
		// for (final JsonNode ricAlarm : ricAlarms)
		// {
		// final String fieldId = ricAlarm.path("fieldId").asText();
		// final ObjectNode newRicAlarm = ricAlarm.deepCopy();
		// newRicAlarm.put("fieldName", fieldMapping.get(fieldId));
		//
		// fgcAlarms.add(newRicAlarm);
		// }
		// }
		//
		// fgcAlarms.addAll(physicalAlarms);

		// 建立並回傳分頁資料集合
		return ControllerUtil.createResponseJson(fgcAlarms);
	}
}