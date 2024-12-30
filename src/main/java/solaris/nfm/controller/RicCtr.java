package solaris.nfm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.service.RicService;

@RestController
@RequestMapping("/v1/ric")
public class RicCtr
{
	@Autowired
	private RicService ricService;

	@GetMapping(value = "/fields")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllFields() throws Exception
	{
		return this.ricService.getFields();
	}

	@GetMapping(value = "/fields/{fieldId}/gnbs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchAllGnbs(@PathVariable("fieldId") final String fieldId) throws Exception
	{
		return this.ricService.getGnbs(fieldId);
	}

	@GetMapping(value = "/cm/configs")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchConfig() throws Exception
	{
		return this.ricService.doMethodGet("/cm/config");
	}

	@PutMapping(value = "/cm/configs")
	@ResponseStatus(HttpStatus.CREATED)
	@OperationLog
	public void replaceConfig(@RequestBody final JsonNode json) throws Exception
	{
		this.ricService.doMethodPut("/cm/config", json);
	}
}
