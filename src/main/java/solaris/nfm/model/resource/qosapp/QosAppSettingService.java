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
public class QosAppSettingService {
	private RestTemplate restTemplate = new RestTemplate();
	
	public void setQosAppRoute(QosAppRouteRepository qosAppRouteRepository, JsonNode body){
		JsonNode qosAppInfoArray = body.get("qos_app_info");
		if(qosAppInfoArray.isArray()) {
			for(JsonNode data : qosAppInfoArray) {
//				System.out.println("ip"+data.get("ip")+" port"+data.get("port"));
				qosAppRouteRepository.save(new QosAppSetting(data.get("ip").textValue(),
						data.get("port").asInt()));
			}
		}
	}
	
	public JsonNode getAllMasterNodeInfo(JsonNode body) {
		String nodeUrl = "http://" + body.get("ip").asText().replace("\"", "") + ":" + body.get("port") + "/connInfo";
		ResponseEntity<JsonNode> obj = restTemplate.exchange(nodeUrl, HttpMethod.GET,null, JsonNode.class);
		return obj.getBody();
	}
	public JsonNode getAllMasterNodeInfo(String ip, int port) {
		String nodeUrl = "http://" + ip + ":" + String.valueOf(port) + "/connInfo";
//		ResponseEntity<JsonNode> obj = restTemplate.exchange(nodeUrl, HttpMethod.GET,null, JsonNode.class);
//		return obj.getBody().toPrettyString();
		ResponseEntity<JsonNode> obj = restTemplate.exchange(nodeUrl, HttpMethod.GET,null, JsonNode.class);
		return obj.getBody();
	}
	
	public void updateMasterNode(String ip, int port, JsonNode body) {
		String nodeUrl = "http://" + ip + ":" + String.valueOf(port) + "/connInfo";
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.postForObject(nodeUrl, body, String.class);
	}
	
//	public JsonNode getAll(QosAppRouteRepository qosAppRouteRepository) {
//		String nodeUrl = "";
//		Iterator<QosAppSetting> iter = qosAppRouteRepository.findAll().iterator();
//		while(iter.hasNext()) {
//			QosAppSetting tmp = iter.next();
//			nodeUrl = "http://" + tmp.getIp() + ":" + tmp.getPort() + "/connInfo";
//		}
//		ResponseEntity<JsonNode> obj = restTemplate.exchange(apirul, HttpMethod.GET, JsonNode.class);
//	}

}
