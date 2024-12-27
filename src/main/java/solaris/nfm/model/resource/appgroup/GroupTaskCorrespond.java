package solaris.nfm.model.resource.appgroup;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "GroupTaskCorrespond")
public class GroupTaskCorrespond {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "serial")
    private Integer id;
	@Column(name = "groupId")
	private String groupId;
	@Column(name = "groupName")
	private String groupName;
	@Column(name = "taskId")
	private String taskId;
	@Column(name = "taskName")
	private String taskName;
	
	public GroupTaskCorrespond() {
		
	}
	
	public GroupTaskCorrespond(String groupId, String groupName, String taskId, String taskName) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.taskId = taskId;
		this.taskName = taskName;
	}

}
