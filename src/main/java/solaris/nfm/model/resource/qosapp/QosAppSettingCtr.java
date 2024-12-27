package solaris.nfm.model.resource.qosapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/v1/qos")
@Slf4j
public class QosAppSettingCtr {
	
	@Autowired
	private QosAppSettingService qosAppSettingService;
	
	private final QosAppRouteRepository qosAppRouteRepository;//資料表動作
	
	public QosAppSettingCtr(QosAppRouteRepository qosAppRouteRepository) {
		this.qosAppRouteRepository = qosAppRouteRepository;
	}
	
	@PutMapping("/setQosRoute")
	@ResponseStatus(HttpStatus.OK)
	private void setQosAppInfo(@RequestBody JsonNode body) {
		qosAppSettingService.setQosAppRoute(qosAppRouteRepository, body);
	}
	
	@GetMapping("/connInfo")
	private ResponseEntity<JsonNode> getMasterNodeInfo(
			@RequestParam(name = "ip") String ip,
			@RequestParam(name = "port") int port){
//		String resp = qosAppSettingService.getAllMasterNodeInfo(ip, port);
//		return resp;
		
		JsonNode resp = qosAppSettingService.getAllMasterNodeInfo(ip, port);
		if(resp != null) {
			return new ResponseEntity<JsonNode>(resp, HttpStatus.OK);
		}else {
			return new ResponseEntity<JsonNode>(resp, HttpStatus.BAD_REQUEST);
		}
		
	}
	
	@PostMapping("/connInfo")
	@ResponseStatus(HttpStatus.CREATED)
	private void updateMasterNode(@RequestParam(name = "ip") String ip,
								  @RequestParam(name = "port") int port,
								  @RequestBody JsonNode body) {
		qosAppSettingService.updateMasterNode(ip, port, body);
	}
	
}
