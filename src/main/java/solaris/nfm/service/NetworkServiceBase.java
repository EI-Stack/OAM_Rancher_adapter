package solaris.nfm.service;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.base.ExceptionBase;

@Slf4j
public class NetworkServiceBase
{
	@Autowired
	public RestTemplate		restTemplate;
	@Autowired
	private ObjectMapper	objectMapper;

	public HttpHeaders		httpHeaders		= null;
	public HttpHeaders		textHttpHeaders	= null;
	public String			networkUrl		= null;

	public RestTemplate getRestTemplate()
	{
		return this.restTemplate;
	}

	public ObjectNode getJsonNode(final String path) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		log.debug("\t GET URL=[{}]", url);
		final HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(this.httpHeaders);
		ResponseEntity<JsonNode> response = null;
		JsonNode responseJson = null;
		try
		{
			response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, JsonNode.class);
			log.debug("\t GET StatusCode={}", response.getStatusCode());
			if (!(response.getStatusCode().equals(HttpStatus.OK))) throw new ExceptionBase(400, "Fetching resource is failed.");

			final JsonNode bodyJson = response.getBody();
			final String errorCode = bodyJson.path("error_code").asText();
			final String errorMessage = bodyJson.path("error_string").asText();
			if (!errorCode.equalsIgnoreCase("00000") || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(Integer.parseInt(errorCode), errorMessage);

			responseJson = bodyJson.path("content");
		} catch (final HttpClientErrorException ex)
		{
			handleErrorMessage(ex);
		}

		return (ObjectNode) responseJson;
	}
	
	public JsonNode getJsonInformation(String path) throws ExceptionBase {
		final String url = this.networkUrl + path;
		log.debug("\t GET URL=[{}]", url);
		final HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(this.httpHeaders);
		ResponseEntity<JsonNode> response = null;
		JsonNode responseJson = null;
		try
		{
			response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, JsonNode.class);
			log.debug("\t GET StatusCode={}", response.getStatusCode());
			if (!(response.getStatusCode().equals(HttpStatus.OK))) throw new ExceptionBase(400, "Fetching resource is failed.");

			final JsonNode bodyJson = response.getBody();
			final String errorCode = bodyJson.path("error_code").asText();
			final String errorMessage = bodyJson.path("error_string").asText();

			responseJson = bodyJson.path("content");
		} catch (final HttpClientErrorException ex)
		{
			handleErrorMessage(ex);
		}

		return (ObjectNode) responseJson;
	}

	public JsonNode postJsonNode(final String path, final JsonNode requestNode) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestNode, this.httpHeaders);
		JsonNode node = null;
		try
		{
			final ResponseEntity<JsonNode> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
			// log.debug("\t[ODL] [getOnlineDeviceNames] code={}", response.getStatusCode());
			// if (!(response.getStatusCode().equals(HttpStatus.CREATED) || response.getStatusCode().equals(HttpStatus.NO_CONTENT))) throw new ExceptionBase(400, "Creating resource is failed.");
			node = response.getBody();
		} catch (final HttpClientErrorException ex)
		{
			handleErrorMessage(ex);
		}

		return node;
	}

	public JsonNode create(final String path, final JsonNode requestNode) throws ExceptionBase, UnsupportedEncodingException
	{
		final String url = this.networkUrl + path;
		log.debug("\t POST URL=[{}]", url);
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestNode, this.httpHeaders);
		JsonNode responseJson = null;
		try
		{
			final ResponseEntity<JsonNode> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
			log.debug("\t POST StatusCode={}", response.getStatusCode());
			if (!(response.getStatusCode().equals(HttpStatus.OK))) throw new ExceptionBase(400, "Creating resource is failed.");

			final JsonNode bodyJson = response.getBody();
			final String errorCode = bodyJson.path("error_code").asText();
			final String errorMessage = bodyJson.path("error_string").asText();
			if (!errorCode.equalsIgnoreCase("00000") || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(Integer.parseInt(errorCode), errorMessage);

			responseJson = bodyJson.path("content");
		} catch (final HttpClientErrorException ex)
		{
			handleErrorMessage(ex);
		}

		return responseJson;
	}

	public void modify(final String path, final JsonNode requestNode) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		log.debug("\t PUT URL=[{}]", url);
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestNode, this.httpHeaders);
		try
		{
			final ResponseEntity<JsonNode> response = this.restTemplate.exchange(url, HttpMethod.PUT, requestEntity, JsonNode.class);
			log.debug("PUT StatusCode =[{}]", response.getStatusCode().toString());
			if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) throw new ExceptionBase(400, "Modifing resource is failed.");

			final JsonNode bodyJson = response.getBody();
			final String errorCode = bodyJson.path("error_code").asText();
			final String errorMessage = bodyJson.path("error_string").asText();
			if (!errorCode.equalsIgnoreCase("00000") || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(Integer.parseInt(errorCode), errorMessage);
		} catch (final HttpClientErrorException ex)
		{
			handleErrorMessage(ex);
		}
	}

	public void delete(final String path) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		final String url = this.networkUrl + path;
		log.debug("\t DELETE URL=[{}]", url);
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(this.httpHeaders);
		try
		{
			final ResponseEntity<JsonNode> response = this.restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, JsonNode.class);
			log.debug("\t DELETE StatusCode={}", response.getStatusCode());
			if (!(response.getStatusCode().equals(HttpStatus.OK))) throw new ExceptionBase(400, "Creating resource is failed.");

			final JsonNode bodyJson = response.getBody();
			final String errorCode = bodyJson.path("error_code").asText();
			final String errorMessage = bodyJson.path("error_string").asText();
			if (!errorCode.equalsIgnoreCase("00000") || !errorMessage.equalsIgnoreCase("success")) throw new ExceptionBase(Integer.parseInt(errorCode), errorMessage);
		} catch (final HttpClientErrorException ex)
		{
			handleErrorMessage(ex);
		}
	}

	protected void handleErrorMessage(final HttpClientErrorException e) throws ExceptionBase
	{
		e.printStackTrace();

		final String errorMessage = MessageFormat.format("Connecting to outer server is failed. Message：{0}", e.getMessage());
		log.error("\t[ErrorHandler] Status Code=[{}]", e.getStatusCode());
		log.error("\t[ErrorHandler] response.getBody() ={}", e.getResponseBodyAsString());
		log.error("\t[ErrorHandler] Error Message: {}", errorMessage);

		JsonNode emNode = null;
		try
		{
			emNode = this.objectMapper.readTree(e.getResponseBodyAsString()).path("message");
		} catch (final Exception e2)
		{
			throw new ExceptionBase(400, errorMessage);
		}

		throw new ExceptionBase(e.getStatusCode().value(), emNode.asText());
	}
}