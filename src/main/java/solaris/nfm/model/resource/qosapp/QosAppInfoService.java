package solaris.nfm.model.resource.qosapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class QosAppInfoService {
	
	@Autowired
	private QosAppInfoRepository qosAppInfoRepository;//資料表動作
	
	public JsonNode getAllQosInfo(String ip, int port) {
		System.out.println("/getAllQosInfo");
		String nodeUrl = "http://" + ip + ":" + port + "/qosInfoAll";
		System.out.println("nodeUrl:"+nodeUrl);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<JsonNode> obj = restTemplate.exchange(nodeUrl, HttpMethod.GET,null, JsonNode.class);
		log.info("getAllQosInfo return:" + obj.getBody());
		return obj.getBody();
	}
	
	public JsonNode getQosInfo(String ip, int port, String namespace) {
		System.out.println("/getQosInfo namespace:" + namespace);
		String nodeUrl = "http://" + ip + ":" + port + "/qosInfo/" + namespace;
		System.out.println("nodeUrl:"+nodeUrl);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<JsonNode> obj = restTemplate.exchange(nodeUrl, HttpMethod.GET,null, JsonNode.class);
		log.info("getQosInfo return:" + obj.getBody());
		return obj.getBody();
	}
	
	public void createQosInfo(String ip, int port, JsonNode body) {
		System.out.println("/createQosInfo namespace:" + body.get("namespace"));
		log.info("createQosInfo body:" + body.toPrettyString());
		String nodeUrl = "http://" + ip + ":" + port + "/qosInfo/" + body.get("namespace").asText();
		System.out.println("nodeUrl:"+nodeUrl);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.postForObject(nodeUrl, body, String.class);
	}
	
	public void deleteQosInfo(String ip, int port, String namespace) {
		String nodeUrl = "http://" + ip + ":" + port + "/qosInfo/" + namespace;
		System.out.println("nodeUrl:"+nodeUrl);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.delete(nodeUrl);
	}

}
