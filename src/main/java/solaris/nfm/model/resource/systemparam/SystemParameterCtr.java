package solaris.nfm.model.resource.systemparam;

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.exception.DeleteNotAllowedException;
import solaris.nfm.exception.EntityFieldInvalidException;
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.EntityIdInvalidException;
import solaris.nfm.exception.EntityIsNullException;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.model.base.domain.FaultAlarmBase.PerceivedSeverity;

@RestController
@RequestMapping("/v1")
public class SystemParameterCtr
{
	@Autowired
	private SystemParameterDao	dao;
	@Autowired
	private SystemParameterDmo	dmo;

	@GetMapping(value = "/sm/dtm/alarm/analyzeMap")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchDtmAlarmAnalyzeMap() throws Exception
	{
		return getDtmAlarmAnalyzeMap("dtmAlarmAnalyzeMap");
	}

	@PutMapping(value = "/sm/dtm/alarm/analyzeMap")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceDtmAlarmAnalyzeMap(@RequestBody final JsonNode json) throws Exception
	{
		setDtmAlarmAnalyzeMap("dtmAlarmAnalyzeMap", json);
	}

	@GetMapping(value = "/faultAlarms/mailAddressSetting")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchFmAlarmMailSetting() throws Exception
	{
		return getMailAddressSetting("fmAlarmMailAddressSetting");
	}

	@PutMapping(value = "/faultAlarms/mailAddressSetting")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replaceFmAlarmMailSetting(@RequestBody final JsonNode json) throws SecurityException, Exception
	{
		modifyMailAddressSetting("fmAlarmMailAddressSetting", json);
	}

	@GetMapping(value = "/performanceAlarms/mailAddressSetting")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchPmAlarmMailSetting() throws Exception
	{
		return getMailAddressSetting("pmAlarmMailAddressSetting");
	}

	@PutMapping(value = "/performanceAlarms/mailAddressSetting")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@OperationLog
	public void replacePmAlarmMailSetting(@RequestBody final JsonNode json) throws SecurityException, Exception
	{
		modifyMailAddressSetting("pmAlarmMailAddressSetting", json);
	}

	private JsonNode createMailAddressSetting()
	{
		final ObjectNode mailAddress = JsonNodeFactory.instance.objectNode();
		final ObjectNode critical = mailAddress.putObject(PerceivedSeverity.CRITICAL.name());
		critical.put("mailDisabled", true).putArray("mailAddresses");
		final ObjectNode major = mailAddress.putObject(PerceivedSeverity.MAJOR.name());
		major.put("mailDisabled", true).putArray("mailAddresses");
		final ObjectNode minor = mailAddress.putObject(PerceivedSeverity.MINOR.name());
		minor.put("mailDisabled", true).putArray("mailAddresses");
		final ObjectNode warning = mailAddress.putObject(PerceivedSeverity.WARNING.name());
		warning.put("mailDisabled", true).putArray("mailAddresses");
		final ObjectNode cleared = mailAddress.putObject(PerceivedSeverity.CLEARED.name());
		cleared.put("mailDisabled", true).putArray("mailAddresses");
		final ObjectNode indeterminate = mailAddress.putObject(PerceivedSeverity.INDETERMINATE.name());
		indeterminate.put("mailDisabled", true).putArray("mailAddresses");

		return mailAddress;
	}

	private JsonNode getMailAddressSetting(final String parameterName) throws EntityHasExistedException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, EntityNotFoundException, EntityFieldInvalidException, EntityIsNullException, EntityIdInvalidException, DeleteNotAllowedException
	{
		final SystemParameter entity = this.dao.findTopByName(parameterName);
		if (entity == null)
		{
			final SystemParameter detach = new SystemParameter();
			detach.setName(parameterName);
			detach.setParameter(createMailAddressSetting());
			this.dmo.createOne(detach);
			return detach.getParameter();
		}

		return entity.getParameter();
	}

	private void modifyMailAddressSetting(final String parameterName, final JsonNode json) throws EntityHasExistedException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, EntityNotFoundException, EntityFieldInvalidException, EntityIsNullException, EntityIdInvalidException, DeleteNotAllowedException
	{
		SystemParameter entity = this.dao.findTopByName(parameterName);
		if (entity == null)
		{
			final SystemParameter detach = new SystemParameter();
			detach.setName(parameterName);
			detach.setParameter(createMailAddressSetting());
			entity = this.dmo.createOne(detach);
		}
		entity.setParameter(json);
		this.dmo.modifyOne(entity);
	}

	private JsonNode getDtmAlarmAnalyzeMap(final String parameterName) throws EntityHasExistedException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, EntityNotFoundException, EntityFieldInvalidException, EntityIsNullException, EntityIdInvalidException, DeleteNotAllowedException
	{
		final SystemParameter entity = this.dao.findTopByName(parameterName);
		if (entity == null)
		{
			final SystemParameter detach = new SystemParameter();
			detach.setName(parameterName);
			detach.setParameter(JsonNodeFactory.instance.objectNode());
			this.dmo.createOne(detach);
			return detach.getParameter();
		}

		return entity.getParameter();
	}

	private void setDtmAlarmAnalyzeMap(final String parameterName, final JsonNode json) throws EntityHasExistedException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, EntityNotFoundException, EntityFieldInvalidException, EntityIsNullException, EntityIdInvalidException, DeleteNotAllowedException
	{
		SystemParameter entity = this.dao.findTopByName(parameterName);
		if (entity == null)
		{
			final SystemParameter detach = new SystemParameter();
			detach.setName(parameterName);
			detach.setParameter(JsonNodeFactory.instance.objectNode());
			entity = this.dmo.createOne(detach);
		}
		entity.setParameter(json);
		this.dmo.modifyOne(entity);
	}
}
