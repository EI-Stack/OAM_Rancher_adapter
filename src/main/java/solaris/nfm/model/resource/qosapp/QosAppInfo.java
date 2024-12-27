package solaris.nfm.model.resource.qosapp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.Data;

@Entity
@Table(name = "QosAppInfo")
public class QosAppInfo{
	
	private static final long	serialVersionUID	= 1L;
	
	private String				name;
	@Id
	private String				namespace;
	private int                 priority;
	private int                 max_bps;
	private int                 min_bps;
	private int                 limit_bps;
	
	@Column(columnDefinition = "jsonb")
	private ArrayNode			stats_bps;
	
	@Column(columnDefinition = "jsonb")
	private ArrayNode			pvc;
	
	public QosAppInfo() {
		
	}
	
	public QosAppInfo(String name, String namespace, int priority, int max_bps,
					int min_bps, int limit_bps, ArrayNode stats_bps, ArrayNode pvc) {
		this.name = name;
		this.namespace = namespace;
		this.priority = priority;
		this.max_bps = max_bps;
		this.min_bps = min_bps;
		this.limit_bps = limit_bps;
		this.stats_bps = stats_bps;
		this.pvc = pvc;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getMax_bps() {
		return max_bps;
	}

	public void setMax_bps(int max_bps) {
		this.max_bps = max_bps;
	}

	public int getMin_bps() {
		return min_bps;
	}

	public void setMin_bps(int min_bps) {
		this.min_bps = min_bps;
	}

	public int getLimit_bps() {
		return limit_bps;
	}

	public void setLimit_bps(int limit_bps) {
		this.limit_bps = limit_bps;
	}

	public ArrayNode getStats_bps() {
		return stats_bps;
	}

	public void setStats_bps(ArrayNode stats_bps) {
		this.stats_bps = stats_bps;
	}

	public ArrayNode getPvc() {
		return pvc;
	}

	public void setPvc(ArrayNode pvc) {
		this.pvc = pvc;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
}
