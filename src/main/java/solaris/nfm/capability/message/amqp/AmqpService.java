package solaris.nfm.capability.message.amqp;

import org.springframework.amqp.rabbit.core.BatchingRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.system.aop.dto.ApiLogDto;
import solaris.nfm.capability.system.aop.dto.LogBaseDto;

@Service
@Slf4j
public class AmqpService
{
	@Value("${spring.rabbitmq.template.exchange}")
	private String					exchangeName;
	// @Value("${spring.rabbitmq.template.routing-key}")
	// private String routingKey;
	// private RabbitTemplate template;
	@Autowired
	private BatchingRabbitTemplate	brtForOperation;
	@Autowired
	private BatchingRabbitTemplate	brtForFault;
	@Autowired
	private BatchingRabbitTemplate	brtForPerformance;
	@Autowired
	private BatchingRabbitTemplate	brtForSystemAlarm;
	@Autowired
	private BatchingRabbitTemplate	brtToLm;
	@Autowired
	private ObjectMapper			objectMapper;

	@Async("taskExecutor")
	public void sendMsg(final LogBaseDto logBaseDto)
	{
		try
		{
			// 當使用 batch 模式發送時，不可使用 correlation 來驗證
			// final CorrelationData correlationData = new CorrelationData(String.valueOf(System.currentTimeMillis()));
			// template.convertAndSend(exchangeName, routingKey, logBaseDto);
			// template.convertAndSend(exchangeName, "policy-service.debug", logBaseDto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	@Async("taskExecutor")
	public void sendMsg(final String routingKey, final Object dto)
	{
		try
		{
			this.brtForFault.convertAndSend(exchangeName, routingKey, dto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	@Async("taskExecutor")
	public void sendMsgForOperationLog(final ApiLogDto apiLogDto) throws JsonProcessingException
	{
		log.debug("\t[AOP] [Operation Log] \n{}", objectMapper.readTree(objectMapper.writeValueAsString(apiLogDto)).toPrettyString());
		final String routingKey = "nfm-to-lm-for-api";

		try
		{
			this.brtForOperation.convertAndSend(exchangeName, routingKey, apiLogDto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	@Async("taskExecutor")
	public void sendMsgForFault(final String routingKey, final Object dto)
	{
		try
		{
			this.brtForFault.convertAndSend(exchangeName, routingKey, dto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	@Async("taskExecutor")
	public void sendMsgForPerformance(final String routingKey, final Object dto)
	{
		try
		{
			this.brtForPerformance.convertAndSend(exchangeName, routingKey, dto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	@Async("taskExecutor")
	public void sendMsgForSystemAlarm(final Object dto)
	{
		final String routingKey = "system-alarm-log";
		try
		{
			this.brtForSystemAlarm.convertAndSend(exchangeName, routingKey, dto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	@Async("taskExecutor")
	public void sendMsgToLm(final Object dto)
	{
		final String routingKey = "nfm-to-lm";
		try
		{
			this.brtToLm.convertAndSend(this.exchangeName, routingKey, dto);
		} catch (final Exception e)
		{
			log.error("\t[Log] Sending AMQP message is failed.");
			e.printStackTrace();
		}
	}

	// @RabbitListener(queues = "odl-to-nm")
	public void listen(final String message) throws JsonMappingException, JsonProcessingException
	{
		// final JsonNode messageNode = objectMapper.readTree(message);
		// log.debug("\t[Log] [Receiver] 收到 AMQP 訊息：" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
		//
		// switch (messageNode.path("type").asText())
		// {
		// case "DeviceOnline" :
		//
		// break;
		//
		// default :
		// break;
		// }
	}
}
