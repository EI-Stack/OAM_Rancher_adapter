package solaris.nfm.model.resource.appgroup;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "AppGroupEvent")
public class AppGroupEvent {
	
	private String type;
	@Id
	private String eventTime;
	private String group_name;
	private String task_name;
	private String event_id;
	public AppGroupEvent() {
		
	}
	public AppGroupEvent(String type, String eventTime, 
			String group_name, String task_name, String event_id) {
		this.type = type;
		this.eventTime = eventTime;
		this.group_name = group_name;
		this.task_name = task_name;
		this.event_id = event_id;
	}
	
	public String toJsonString() {
		String jsonString = "";
		jsonString += "{";
		jsonString += "\"type\": \"" + type + "\",";
		jsonString += "\"eventTime\": \"" + eventTime + "\",";
		jsonString += "\"group_name\": \"" + group_name + "\",";
		jsonString += "\"task_name\": \"" + task_name + "\",";
		jsonString += "\"event_id\": \"" + event_id + "\"";
		jsonString += "}";
		return jsonString;
	}
}
