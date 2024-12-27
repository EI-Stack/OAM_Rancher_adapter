package solaris.nfm.service;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import solaris.nfm.controller.dto.OdlResponseDto;
import solaris.nfm.util.OdlInfo;

import java.util.List;

@Service
@Slf4j
public class OdlReqService {
    @Autowired
    private OdlInfo odlInfo;
    @Autowired
    private RestTemplate restTemplate;

    public OdlResponseDto get(String path) {
        final String url = odlInfo.getUrl() + path;
        final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(getHeaders());
        final OdlResponseDto odlResponseDto = new OdlResponseDto();
        try {
            final ResponseEntity<JsonNode> responseEntity = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, JsonNode.class);
            odlResponseDto.setSuccess(true);
            odlResponseDto.setContent(responseEntity.getBody());
        } catch (final HttpStatusCodeException e) {
            odlResponseDto.setErrorCode(e.getStatusCode().value());
            odlResponseDto.setSuccess(false);
            odlResponseDto.setErrorMessage(e.getResponseBodyAsString());
            log.error("ODL GET error {}", e.getResponseBodyAsString());
        }
        return odlResponseDto;
    }

    public OdlResponseDto put(String path, JsonNode input) {
        final String url = odlInfo.getUrl() + path;
        final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(input, getHeaders());
        final OdlResponseDto odlResponseDto = new OdlResponseDto();
        try {
            final ResponseEntity<JsonNode> responseEntity = this.restTemplate.exchange(url, HttpMethod.PUT, requestEntity, JsonNode.class);
            odlResponseDto.setSuccess(true);
            odlResponseDto.setContent(responseEntity.getBody());
        } catch (final HttpStatusCodeException e) {
            odlResponseDto.setErrorCode(e.getStatusCode().value());
            odlResponseDto.setSuccess(false);
            odlResponseDto.setErrorMessage(e.getResponseBodyAsString());
            log.error("ODL PUT error {}", e.getResponseBodyAsString());
        }
        return odlResponseDto;
    }

    public OdlResponseDto post(String path, JsonNode input) {
        final String url = odlInfo.getUrl() + path;
        final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(input, getHeaders());
        final OdlResponseDto odlResponseDto = new OdlResponseDto();
        try {
            final ResponseEntity<JsonNode> responseEntity = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
            odlResponseDto.setSuccess(true);
            odlResponseDto.setContent(responseEntity.getBody());
        } catch (final HttpStatusCodeException e) {
            odlResponseDto.setErrorCode(e.getStatusCode().value());
            odlResponseDto.setSuccess(false);
            odlResponseDto.setErrorMessage(e.getResponseBodyAsString());
            log.error("ODL POST error {}", e.getResponseBodyAsString());
        }
        return odlResponseDto;
    }

    public OdlResponseDto delete(String path) {
        final String url = odlInfo.getUrl() + path;
        final HttpEntity<JsonNode> requestEntity = new HttpEntity<>(getHeaders());
        final OdlResponseDto odlResponseDto = new OdlResponseDto();
        try {
            final ResponseEntity<JsonNode> responseEntity = this.restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, JsonNode.class);
            odlResponseDto.setSuccess(true);
            odlResponseDto.setContent(responseEntity.getBody());
        } catch (final HttpStatusCodeException e) {
            odlResponseDto.setErrorCode(e.getStatusCode().value());
            odlResponseDto.setSuccess(false);
            odlResponseDto.setErrorMessage(e.getResponseBodyAsString());
            log.error("ODL DELETE error {}", e.getResponseBodyAsString());
        }
        return odlResponseDto;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        httpHeaders.setBasicAuth(odlInfo.getUsername(), odlInfo.getPassword());
        return httpHeaders;
    }
}
