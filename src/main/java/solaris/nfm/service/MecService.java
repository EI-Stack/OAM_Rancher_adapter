package solaris.nfm.service;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.rest.RestServiceBase;
import solaris.nfm.exception.base.ExceptionBase;

@Service
@Slf4j
public class MecService extends RestServiceBase
{
	private static String	authenticationKey	= "email=achiou@itri.org.tw&apiKey=c39968c850acdeac4c4aa63a07b93a5f";

	@Value("${solaris.server.mec.http.url}")
	private String			url;

	@Override
	@PostConstruct
	public void init()
	{
		super.setNetworkUrl(this.url);
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.setOrigin(this.url);
		super.setHttpHeaders(httpHeaders);
	}

	public String createRegion(final String path, final JsonNode json) throws ExceptionBase, UnsupportedEncodingException
	{
		final String result = super.doMethodPostForBodyText(path + "?" + authenticationKey, json);
		return (result == null) ? "" : result.strip();
	}

	public void deleteRegion(final String path) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		super.doMethodDeteleForBodyText(path + "?" + authenticationKey);
	}

	public void createRegionConfig(final String regionId, final JsonNode requestJson) throws ExceptionBase, UnsupportedEncodingException
	{
		super.doMethodPost("/region/deploy/configuration?regionId=" + regionId + "&" + authenticationKey, requestJson);
	}

	public void startDeployRegion(final String regionId, final String mecVersion) throws ExceptionBase, UnsupportedEncodingException
	{
		super.doMethodPost("/region/installation?regionId=" + regionId + "&mecVersion=" + mecVersion + "&" + authenticationKey, null);
	}

	public void stopDeployRegion(final String regionId) throws ExceptionBase, UnsupportedEncodingException
	{
		super.doMethodDetele("/region/installation/" + regionId + "?" + authenticationKey);
	}

	public void redoDeployRegion(final String regionId, final String mecVersion) throws ExceptionBase, UnsupportedEncodingException
	{
		super.doMethodPut("/region/installation/" + regionId + "?mecVersion=" + mecVersion + "&" + authenticationKey, null);
	}

	public JsonNode checkDeployRegion(final String regionId) throws ExceptionBase, UnsupportedEncodingException
	{
		return super.doMethodGet("/region/installation/" + regionId + "?" + authenticationKey);
	}

	public void updateComponentNetwork(final String path, final JsonNode json) throws ExceptionBase
	{
		super.doMethodPut(path + authenticationKey, json);
	}

	public void createUpfNetwork(final String regionId, final String componentId, final JsonNode requestJson) throws ExceptionBase, UnsupportedEncodingException
	{
		super.doMethodPost("/region/component/" + regionId + "?componentId=" + componentId + "&" + authenticationKey, requestJson);
	}

	public void deleteUpfNetwork(final String regionId, final String componentId) throws ExceptionBase, JsonMappingException, JsonProcessingException
	{
		super.doMethodDetele("/region/component/" + regionId + "?componentId=" + componentId + "&" + authenticationKey);
	}

	@Override
	public JsonNode doMethodGet(final String path) throws ExceptionBase
	{
		return super.doMethodGet(path + "?" + authenticationKey);
	}

	public void createApp(final String regionId, final JsonNode requestJson) throws ExceptionBase, UnsupportedEncodingException
	{
		super.doMethodPost("/region/app/" + regionId + "?" + authenticationKey, requestJson);
	}

	@Override
	public JsonNode doMethodPut(final String path, final JsonNode json) throws ExceptionBase
	{
		return super.doMethodPut(path + MecService.authenticationKey, json);
	}

	public void deleteApp(final String regionId, final String appId) throws ExceptionBase
	{
		super.doMethodDetele("/region/app/" + regionId + "?appId=" + appId + "&" + MecService.authenticationKey);
	}

	protected void handleErrorForBodyString(final ExceptionBase e, final ResponseEntity<String> response) throws ExceptionBase
	{
		final String errorMessage = MessageFormat.format("Connecting to external server is failed. Message：{0}", e.getMessage());
		log.error("\t [REST] Status Code=[{}]", response.getStatusCode());
		log.error("\t [REST] response.getBody() =\n{}", response.getBody());
		log.error("\t [REST] Error Message: {}", errorMessage);

		throw new ExceptionBase(400, errorMessage);
	}
}