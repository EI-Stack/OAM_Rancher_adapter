package solaris.nfm.capability.db.influxdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

//@Configuration
public class InfluxDbConfig
{
	@Value("${spring.influx.url:''}")
	private String	influxDBUrl;
	@Value("${spring.influx.user:''}")
	private String	userName;

	@Value("${spring.influx.password:''}")
	private String	pwpwpwpw;

	@Value("${spring.influx.database:''}")
	private String	database;

	@Bean
	public InfluxDbUtils influxDbUtils()
	{
		return new InfluxDbUtils(userName, pwpwpwpw, influxDBUrl, database, "logmonitor");
	}
}