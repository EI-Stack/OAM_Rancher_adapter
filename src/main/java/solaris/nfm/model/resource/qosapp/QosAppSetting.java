package solaris.nfm.model.resource.qosapp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "QosAppSetting")
public class QosAppSetting{
	@Id
	private String				ip;
	private int				    port;
	
	public QosAppSetting() {
		
	}
	
	public QosAppSetting(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "QosAppSetting [ip=" + this.ip
				+ ",port=" + port + "]";
	}

}
