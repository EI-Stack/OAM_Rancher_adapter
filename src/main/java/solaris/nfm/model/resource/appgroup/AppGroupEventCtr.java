package solaris.nfm.model.resource.appgroup;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.controller.util.ControllerUtil;

@RestController
@RequestMapping("/v1")
@Slf4j
public class AppGroupEventCtr {
	
	@Autowired
	private AppGroupEventService appGroupEventService;
	@Autowired
	private ObjectMapper objectmapper;
	
	@PostMapping("/events")  //ITRI會透過這個API發送event過來
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void getGroupEvent(@RequestBody JsonNode event) {
		appGroupEventService.handleEvent(event);
	}
	
	@GetMapping("/events")  //前端取資料庫log用
	@ResponseStatus(HttpStatus.OK)
	public JsonNode getAllEvents(@RequestParam(required = false) String name) throws JsonMappingException, JsonProcessingException {
//		return appGroupEventService.getAllEvents();
		ArrayNode events = objectmapper.createArrayNode();
		Iterable<AppGroupEvent> eventsInDb = null;
		if(name == null) {
			eventsInDb = appGroupEventService.getAllEvents();	
		}else{
			eventsInDb = appGroupEventService.getEventsByName(name);
		}
		 
		Iterator<AppGroupEvent> iter = eventsInDb.iterator();
		while(iter.hasNext()) {
			String tmp = iter.next().toJsonString();
			log.info("events : " + tmp);
			events.add(objectmapper.readTree(tmp));
		}
		JsonNode returnJson = ControllerUtil.createResponseJson(events);
		log.info("returnJson:" + returnJson.toPrettyString());
		return returnJson;
	}

}
