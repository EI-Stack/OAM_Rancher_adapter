package solaris.nfm.capability.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Set;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.util.SslUtils;

@Slf4j
public class RestServiceBase
{
	@Getter
	@Setter
	private static Boolean	logEnabled	= false;
	@Getter
	@Setter
	protected RestTemplate	restTemplate;
	@Getter
	@Setter
	protected String		networkUrl	= null;
	@Getter
	@Setter
	private HttpHeaders		httpHeaders	= null;
	@Getter
	@Setter
	private HttpHeaders		multipartFileHttpHeaders	= null;

	@PostConstruct
	public void init()
	{}

	protected void initService(final RestTemplateBuilder restTemplateBuilder, final SslBundles sslBundles, final RestTemplate restTemplate, String url, String token, Boolean sslEnabled,
			String sslBundleName)
	{
		// Configure restTemplate
		RestTemplate specificRestTemplate = null;
		if (Boolean.TRUE.equals(sslEnabled))
		{
			specificRestTemplate = restTemplateBuilder.setSslBundle(sslBundles.getBundle(sslBundleName)).build();
			specificRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		} else
		{
			specificRestTemplate = restTemplate;
		}
		setRestTemplate(specificRestTemplate);
		// Configure URL
//		if (Boolean.TRUE.equals(sslEnabled))
//		{
//			setNetworkUrl("https://" + url);
//		} else
//		{
//			setNetworkUrl("http://" + url);
//		}
		setNetworkUrl(url);

		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.setOrigin(getNetworkUrl());
		httpHeaders.setBearerAuth(token);
		// httpHeaders.setBasicAuth(this.configBean.getUsername(), this.configBean.getPassword());
		setHttpHeaders(httpHeaders);

		final HttpHeaders httpHeaders2 = new HttpHeaders();
		httpHeaders2.setContentType(MediaType.MULTIPART_FORM_DATA);
		httpHeaders2.setOrigin(getNetworkUrl());
		httpHeaders2.setBearerAuth(token);
		setMultipartFileHttpHeaders(httpHeaders2);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodGet(final String path) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.ACCEPTED);
		return doMethodGet(path, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodGetString(final String path) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.ACCEPTED);
		return doMethodGetString(path, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodGet(final String path, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		JsonNode returnedBody = null;
		try
		{
			returnedBody = _doMehtodGet(path, allowedStatusCodes, JsonNode.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody.toPrettyString();
			log.error("\t [REST] GET body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodGetString(final String path, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodGet(path, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody;
			log.error("\t [REST] GET body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodGetForBodyText(final String path) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.ACCEPTED);
		return doMethodGetForBodyText(path, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodGetForBodyText(final String path, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodGet(path, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "" : returnedBody.toString();
			log.error("\t [REST] GET body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	private <T> T _doMehtodGet(final String path, final Set<HttpStatus> allowedStatusCodes, final Class<T> responseType) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		if (logEnabled)
		{
			log.debug("\t [REST] GET URL=[{}]", url);
		}
		SslUtils.ignoreSsl();
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(this.httpHeaders);
		ResponseEntity<T> response = null;
		response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, responseType);
		if (logEnabled)
		{
			log.debug("\t [REST] GET code={}", response.getStatusCode());
		}
		if (allowedStatusCodes.contains(response.getStatusCode()) == false)
		{
			throw new ExceptionBase(400, "Fetching resource is failed.");
		}

		return response.getBody();
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPost(final String path, final JsonNode json) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPost(path, json, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPostString(final String path, final String body) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPostString(path, body, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPostFile(final String path, final MultipartFile multipartFile) throws ExceptionBase
	{
		try
		{
			final String url = this.networkUrl + path;
			byte[] fileBytes = multipartFile.getBytes();
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new ByteArrayResource(fileBytes) {
				@Override
				public String getFilename() {
					return multipartFile.getOriginalFilename();
				}
			});
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, this.multipartFileHttpHeaders);
			final ResponseEntity<JsonNode> responseEntity = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
			return responseEntity.getBody();
		}
		catch (final HttpStatusCodeException e)
		{
			throw new ExceptionBase(400, e.getResponseBodyAsString());
		}
		catch (final IOException e)
		{
			throw new ExceptionBase(400, "Upload file has occured IO Exception.");
		}
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPost(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		JsonNode returnedBody = null;
		try
		{
			returnedBody = _doMehtodPost(path, json, allowedStatusCodes, JsonNode.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody.toPrettyString();
			log.error("\t [REST] POST body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPostString(final String path, final String body, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodPostString(path, body, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody;
			log.error("\t [REST] POST body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPostForBodyText(final String path, final JsonNode json) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPostForBodyText(path, json, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPostForBodyText(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodPost(path, json, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "" : returnedBody.toString();
			log.error("\t [REST] POST body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	private <T> T _doMehtodPost(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes, final Class<T> responseType) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		if (logEnabled)
		{
			log.debug("\t [REST] POST URL=[{}]", url);
		}
		SslUtils.ignoreSsl();
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(json, this.httpHeaders);
		final ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);
		if (logEnabled)
		{
			log.debug("\t [REST] POST Code =[{}]", response.getStatusCode().toString());
		}
		if (allowedStatusCodes.contains(response.getStatusCode()) == false)
		{
			throw new ExceptionBase(400, "Creating resource is failed.");
		}

		return response.getBody();
	}

	private <T> T _doMehtodPostString(final String path, final String body, final Set<HttpStatus> allowedStatusCodes, final Class<T> responseType) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		if (logEnabled)
		{
			log.debug("\t [REST] POST URL=[{}]", url);
		}
		SslUtils.ignoreSsl();
		final HttpEntity<String> requestEntity = new HttpEntity<>(body, this.httpHeaders);
		final ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);
		if (logEnabled)
		{
			log.debug("\t [REST] POST Code =[{}]", response.getStatusCode().toString());
		}
		if (allowedStatusCodes.contains(response.getStatusCode()) == false)
		{
			throw new ExceptionBase(400, "Creating resource is failed.");
		}

		return response.getBody();
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPut(final String path, final JsonNode json) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPut(path, json, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPut(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		JsonNode returnedBody = null;
		try
		{
			returnedBody = _doMehtodPut(path, json, allowedStatusCodes, JsonNode.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody.toPrettyString();
			log.error("\t [REST] PUT body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPutForBodyText(final String path, final JsonNode json) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPutForBodyText(path, json, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPutForBodyText(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodPut(path, json, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "" : returnedBody.toString();
			log.error("\t [REST] PUT body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	private <T> T _doMehtodPut(final String path, final JsonNode requestJson, final Set<HttpStatus> allowedStatusCodes, final Class<T> responseType) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		if (logEnabled)
		{
			log.debug("\t [REST] PUT URL=[{}]", url);
		}
		SslUtils.ignoreSsl();
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestJson, this.httpHeaders);
		final ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.PUT, requestEntity, responseType);
		if (logEnabled)
		{
			log.debug("\t [REST] PUT Code =[{}]", response.getStatusCode().toString());
		}
		if (allowedStatusCodes.contains(response.getStatusCode()) == false)
		{
			throw new ExceptionBase(400, "Modifing resource is failed.");
		}

		return response.getBody();
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public void doMethodDetele(final String path) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		doMethodDetele(path, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public void doMethodDetele(final String path, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		JsonNode returnedBody = null;
		try
		{
			returnedBody = _doMehtodDelete(path, allowedStatusCodes, JsonNode.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody.toPrettyString();
			log.error("\t [REST] DELETE body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public void doMethodDeteleForBodyText(final String path) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		doMethodDeteleForBodyText(path, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public void doMethodDeteleForBodyText(final String path, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodDelete(path, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "" : returnedBody.toString();
			log.error("\t [REST] PATCH body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
	}

	private <T> T _doMehtodDelete(final String path, final Set<HttpStatus> allowedStatusCodes, final Class<T> responseType) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		if (logEnabled)
		{
			log.debug("\t [REST] DELETE URL=[{}]", url);
		}
		SslUtils.ignoreSsl();
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(this.httpHeaders);
		final ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, responseType);
		if (logEnabled)
		{
			log.debug("\t [REST] DELETE Code =[{}]", response.getStatusCode().toString());
		}
		if (allowedStatusCodes.contains(response.getStatusCode()) == false)
		{
			throw new ExceptionBase(400, "Deleting resource is failed.");
		}

		return response.getBody();
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPatch(final String path, final JsonNode json) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPatch(path, json, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public JsonNode doMethodPatch(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		JsonNode returnedBody = null;
		try
		{
			returnedBody = _doMehtodPatch(path, json, allowedStatusCodes, JsonNode.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "{}" : returnedBody.toPrettyString();
			log.error("\t [REST] PATCH body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPatchForBodyText(final String path, final JsonNode json) throws ExceptionBase
	{
		final Set<HttpStatus> allowedStatusCodes = Set.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT);
		return doMethodPatchForBodyText(path, json, allowedStatusCodes);
	}

	@Retryable(org.springframework.web.client.ResourceAccessException.class)
	public String doMethodPatchForBodyText(final String path, final JsonNode json, final Set<HttpStatus> allowedStatusCodes) throws ExceptionBase
	{
		String returnedBody = null;
		try
		{
			returnedBody = _doMehtodPatch(path, json, allowedStatusCodes, String.class);
		} catch (final ExceptionBase ex)
		{
			final String errorMessage = (returnedBody == null) ? "" : returnedBody.toString();
			log.error("\t [REST] PATCH body=\n{}", errorMessage);
			ex.setErrorMessage(ex.getErrorMessage() + " Message: " + errorMessage);
			throw ex;
		}
		return returnedBody;
	}

	private <T> T _doMehtodPatch(final String path, final JsonNode requestJson, final Set<HttpStatus> allowedStatusCodes, final Class<T> responseType) throws ExceptionBase
	{
		final String url = this.networkUrl + path;
		if (logEnabled)
		{
			log.debug("\t [REST] PATCH URL=[{}]", url);
		}
		SslUtils.ignoreSsl();
		final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestJson, this.httpHeaders);
		ResponseEntity<T> response = null;
		response = this.restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, responseType);
		if (logEnabled)
		{
			log.debug("\t [REST] PATCH Code =[{}]", response.getStatusCode().toString());
		}
		if (allowedStatusCodes.contains(response.getStatusCode()) == false)
		{
			throw new ExceptionBase(400, "Patching resource is failed.");
		}

		return response.getBody();
	}

	protected void handleErrorReturnJson(final ExceptionBase e, final ResponseEntity<JsonNode> response) throws ExceptionBase
	{
		final String errorMessage = MessageFormat.format("Connecting to external server is failed. Message：{0}", e.getMessage());
		log.error("\t [REST] Status Code=[{}]", response.getStatusCode());
		log.error("\t [REST] response.getBody() =\n{}", response.getBody().toPrettyString());
		log.error("\t [REST] Error Message: {}", errorMessage);

		throw new ExceptionBase(400, errorMessage);
	}
}