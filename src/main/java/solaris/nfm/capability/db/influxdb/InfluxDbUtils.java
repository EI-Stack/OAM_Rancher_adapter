package solaris.nfm.capability.db.influxdb;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class InfluxDbUtils
{
	private String			userName;
	private String			pwpwpwpw;
	private String			url;
	public String			database;
	private String			retentionPolicy;
	// InfluxDB 實例
	private InfluxDB		influxDB;

	// 數據保存策略
	public static String	policyNamePix	= "logmonitor";

	public InfluxDbUtils(final String userName, final String pwpwpwpw, final String url, final String database, final String retentionPolicy)
	{
		this.userName = userName;
		this.pwpwpwpw = pwpwpwpw;
		this.url = url;
		this.database = database;
		this.retentionPolicy = retentionPolicy == null || "".equals(retentionPolicy) ? "autogen" : retentionPolicy;
		this.influxDB = influxDbBuild();
	}

	/**
	 * 連接數據庫 ，若不存在則創建
	 *
	 * @return influxDb實例
	 */
	private InfluxDB influxDbBuild()
	{
		if (influxDB == null)
		{
			influxDB = InfluxDBFactory.connect(url, userName, pwpwpwpw);
		}
		try
		{
			createDB(database);
			influxDB.setDatabase(database);
		} catch (Exception e)
		{
			log.error("create influx db failed, error: {}", e.getMessage());
		} finally
		{
			influxDB.setRetentionPolicy(retentionPolicy);
		}
		influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
		return influxDB;
	}

	/****
	 * 創建數據庫
	 *
	 * @param database
	 */
	private void createDB(final String database)
	{
		influxDB.query(new Query("CREATE DATABASE " + database));
	}
}