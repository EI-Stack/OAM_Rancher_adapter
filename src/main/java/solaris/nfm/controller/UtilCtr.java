package solaris.nfm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import solaris.nfm.capability.annotation.jsonvalid.JsonValid;
import solaris.nfm.capability.annotation.jsonvalid.JsonValidationService;
import solaris.nfm.capability.annotation.log.OperationLog;
import solaris.nfm.capability.rest.RestServiceBase;

@RestController
@RequestMapping("/v1")
@Validated
public class UtilCtr
{
	@Autowired
	private JsonValidationService jsonValidationService;

	// ================================================================================================================
	// JSON Validation
	// ================================================================================================================
	/**
	 * 讀取 Json Schema 的內容，Json/YAML 格式都可以
	 */
	@GetMapping(value = "/jsonValidation/jsonSchemaFiles/{fileName}")
	@ResponseStatus(HttpStatus.OK)
	public String fetchJsonSchemaFile(@PathVariable("fileName") final String fileName) throws Exception
	{
		return jsonValidationService.getJsonSchemaFileFromClasspath(fileName);
	}

	/**
	 * 讀取 Json Schema 的啟用狀態
	 */
	@GetMapping(value = "/jsonValidation")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchJsonValidationStatus() throws Exception
	{
		return JsonNodeFactory.instance.objectNode().put("enabled", jsonValidationService.getJsonValidEnabled());
	}

	/**
	 * 設定 Json Schema 的啟用狀態
	 */
	@PutMapping(value = "/jsonValidation")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@JsonValid("api.yaml#/CapabilityEnabledStatus")
	@OperationLog
	public void modifyJsonValidationStatus(@RequestBody final JsonNode requestJson) throws Exception
	{
		jsonValidationService.setJsonValidEnabled(requestJson.path("enabled").asBoolean());
	}

	// ================================================================================================================
	// 調用外部 REST API 的日誌狀態
	// ================================================================================================================

	/**
	 * 讀取 REST API log 的狀態
	 */
	@GetMapping(value = "/restApiLogStatus")
	@ResponseStatus(HttpStatus.OK)
	public JsonNode fetchRestApiLogStatus() throws Exception
	{
		return JsonNodeFactory.instance.objectNode().put("enabled", RestServiceBase.getLogEnabled());
	}

	/**
	 * 設定 REST API log 的狀態
	 */
	@PutMapping(value = "/restApiLogStatus")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@JsonValid("api.yaml#/CapabilityEnabledStatus")
	@OperationLog
	public void modifyRestApiLogStatus(@RequestBody final JsonNode requestJson) throws Exception
	{
		RestServiceBase.setLogEnabled(requestJson.path("enabled").asBoolean());
	}
}
