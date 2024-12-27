package solaris.nfm.capability.db.influxdb;

import java.util.Map;

import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class InfluxDbService
{
	@Value("${spring.influx.database:''}")
	private String		database;

	@Autowired
	private InfluxDB	influxDb;

	@Async("taskExecutor")
	public void write(final String endPointName, final String nodeId, final String nodeValue)
	{
		final Point point = getPoint(endPointName, nodeId, nodeValue);
		if (point != null) influxDb.write(database, "", point);
	}

	@Async("taskExecutor")
	public void writeBatch(final String endPointName, final Map<String, String> nodeMap)
	{
		final BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy("").build();

		for (final String nodePath : nodeMap.keySet())
		{
			final String nodeValue = nodeMap.get(nodePath);
			final Point point = getPoint(endPointName, nodePath, nodeValue);
			if (point == null) continue;
			batchPoints.point(point);
		}

		influxDb.write(batchPoints);
	}

	private Point getPoint(final String endPointName, final String nodeId, final String nodeValue)
	{
		String nodeDataType;
		try
		{
			// nodeDataType = dataModelSpecService.getResourceDataType(nodeId);
			nodeDataType = null;
		} catch (final Exception e)
		{
			return null;
		}
		final Builder builder = Point.measurement("/" + nodeId).tag("end_point_name", endPointName);
		switch (nodeDataType)
		{
			case "integer" :
			case "unsigned integer" :
				builder.addField("value", Long.valueOf(nodeValue));
				break;
			case "float" :
				builder.addField("value", Double.valueOf(nodeValue));
				break;
			case "boolean" :
				builder.addField("value", Boolean.valueOf(nodeValue));
				break;
			default :
				builder.addField("value", nodeValue);
				break;
		}
		final Point point = builder.build();

		return point;
	}
}
