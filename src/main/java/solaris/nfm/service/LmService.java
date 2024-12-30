package solaris.nfm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import solaris.nfm.capability.rest.RestServiceBase;

@Service
public class LmService extends RestServiceBase
{
	@Value("${solaris.server.lm.http.url}")
	private String	url;
	@Value("${solaris.server.lm.http.token}")
	private String	token;

	@Override
	@PostConstruct
	public void init()
	{
		super.setNetworkUrl(this.url);

		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.setOrigin(this.url);
		httpHeaders.setBearerAuth(token);
		super.setHttpHeaders(httpHeaders);
	}
}