package solaris.nfm.model.resource.appgroup;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.message.websocket.WebSocketService;

@Service
@Slf4j
public class AppGroupEventService {
	@Autowired
	public AppGroupEventsRepository appGroupEventsRepository;
	@Autowired
	public CorrespondTableRepository correspondTableRepository;
	@Autowired
	public WebSocketService wsService;
	@Autowired
	private ObjectMapper objectMapper;
	
	public JsonNode handleEvent(JsonNode events) {
		log.info("handleEvent service");
		log.info("events:" + events.toPrettyString());
		//存入postgres及發送socket
		if(events.isArray()) {
			for(JsonNode event : events) {
				dealEvent(event);
			}
		}
		return events;
	}
	public void dealEvent(JsonNode event) {
		String type = event.get("type").asText();
		String Time = event.get("Time").asText();
		String event_id = "";
		String group_name = "";
		String task_name = "";
		GroupTaskCorrespond correspond = null;
		List<GroupTaskCorrespond> list = null;
		switch(type) {
			case "preempted_group":
				event_id = event.get("preempted_group_id").asText();
				log.info("type:"+type+" Time:"+Time+" event_id:" + event_id);
				list = correspondTableRepository.findByGroupIdIs(event_id);
				if(list.size() == 0) {
					group_name = "Cannot find group name";
				}else {
					correspond = correspondTableRepository.findByGroupIdIs(event_id).get(0);
					group_name = correspond.getGroupName();
				}
				task_name = "";
				break;
			case "error_task":
				event_id = event.get("error_task_id").asText();
				log.info("type:"+type+" Time:"+Time+" event_id:" + event_id);
				list = correspondTableRepository.findByTaskIdIs(event_id);
				if(list.size() == 0) {
					group_name = "Cannot find group name";
					task_name = "Cannot find task name";
				}else {
					correspond = list.get(0);
					task_name = correspond.getTaskName();
					group_name = correspond.getGroupName();
				}
				break;
		}
		AppGroupEvent entity = new AppGroupEvent(
				type, Time, group_name, task_name, event_id
				);
		appGroupEventsRepository.save(entity);
		JsonNode node = objectMapper.convertValue(entity, JsonNode.class);
		log.info("send websocket:" + node.toPrettyString());
		wsService.broadcastAll(node.toPrettyString());//發送web socket
	}
	public Iterable<AppGroupEvent> getAllEvents() {
		return appGroupEventsRepository.findAll();
	}
	
	public Iterable<AppGroupEvent> getEventsByName(String name) {
		return appGroupEventsRepository.findByGroupName(name);
	}
}

@Repository
interface AppGroupEventsRepository extends CrudRepository<AppGroupEvent, String> {
	@Query(value="SELECT * FROM backend.app_group_event WHERE group_name=?1", nativeQuery = true)
	List<AppGroupEvent> findByGroupName(String groupName);
}
