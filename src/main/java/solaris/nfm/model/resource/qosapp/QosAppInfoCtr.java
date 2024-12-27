package solaris.nfm.model.resource.qosapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
public class QosAppInfoCtr{
	
	@Autowired
	private QosAppInfoService qosAppInfoService;
	
	@GetMapping("/qosInfoAll")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<JsonNode> getAllInfo(
			@RequestParam(name = "ip") String ip,
			@RequestParam(name = "port") int port) {
		log.info("Controller qosInfoAll ip:" + ip + " port:" + port);
		JsonNode resp = qosAppInfoService.getAllQosInfo(ip, port);
		if(resp != null) {
			return new ResponseEntity<JsonNode>(resp, HttpStatus.OK);
		}else {
			return new ResponseEntity<JsonNode>(resp, HttpStatus.BAD_REQUEST);
		}
	}
	
	@GetMapping("/qosInfo")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<JsonNode> getQosInfo(
			@RequestParam(name = "ip") String ip,
			@RequestParam(name = "port") int port,
			@RequestParam(name = "namespace") String namespace) {
		JsonNode resp = qosAppInfoService.getQosInfo(ip, port, namespace);
		if(resp != null) {
			return new ResponseEntity<JsonNode>(resp, HttpStatus.OK);
		}else {
			return new ResponseEntity<JsonNode>(resp, HttpStatus.BAD_REQUEST);
		}
	}
	
	@PostMapping("qosInfo")
	@ResponseStatus(HttpStatus.OK)
	public void CreateQosInfo(
			@RequestParam(name = "ip") String ip,
			@RequestParam(name = "port") int port,
			@RequestParam(name = "namespace") String namespace,
			@RequestBody JsonNode body){
		qosAppInfoService.createQosInfo(ip, port, body);
	}
	
	@DeleteMapping("qosInfo")
	@ResponseStatus(HttpStatus.OK)
	public void DeleteQosInfo(@RequestParam(name = "ip") String ip,
			@RequestParam(name = "port") int port,
			@RequestParam(name = "namespace") String namespace) {
		qosAppInfoService.deleteQosInfo(ip, port, namespace);
	}
}
