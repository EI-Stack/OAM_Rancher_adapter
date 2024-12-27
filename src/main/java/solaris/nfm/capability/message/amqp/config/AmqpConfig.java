package solaris.nfm.capability.message.amqp.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.batch.BatchingStrategy;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.BatchingRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AmqpConfig
{
	@Autowired
	private ObjectMapper objectMapper;

	// @Bean
	// @Primary
	// @ConfigurationProperties(prefix = "solaris.datasource.tenant")
	// public DataSource defaultDataSource()
	// {
	// return DataSourceBuilder.create().build();
	// }

	// RabbitAdmin will find this and bind it to the default exchange with a routing key
	// 會先偵測 broker 上面有無此 queue，若無，則會新建。建立後，此段程式碼就不再被執行，但不能因此誤認為此段不需要
	// @Bean
	// public Queue logServiceQueue(@Value("${spring.rabbitmq.template.routing-key}") final String queueName)
	// {
	// return new Queue(queueName, true, false, false);
	// }

	@Bean
	public Declarables topicBindings(@Value("${spring.rabbitmq.template.exchange}") final String exchangeName, @Value("${spring.rabbitmq.template.routing-key}") final String defaultRoutingKey,
			@Value("${spring.rabbitmq.template.durable-routing-key}") final String durableRoutingKey)
	{
		final TopicExchange topicExchange = new TopicExchange(exchangeName);

		// final Queue queueForDebug = new Queue("policy-for-debug", true, false, false);
		final Queue queueFromLm = new Queue("lm-to-nfm", true, false, false);
		final Queue queueFromAdapter = new Queue("adapter-to-nfm", true, false, false);

		// @formatter:off
		return new Declarables(
			// queueForDebug,
			queueFromLm,
			queueFromAdapter,
			topicExchange,
			BindingBuilder.bind(queueFromLm).to(topicExchange).with("lm-to-nfm"),
			BindingBuilder.bind(queueFromAdapter).to(topicExchange).with("adapter-to-nfm"));
		// @formatter:on
	}

	/**
	 * AmqpTemplate 在整個系統中都不會用到，看似無用，但是，若無此段，就不會在 RabbitMQ 產生 Queue
	 */
	@Bean
	@Primary  // Spring bean pool 裡面已經存在 amqpTemplate，所以使用 @Primary 覆蓋過去
	public AmqpTemplate amqpTemplate(@Autowired final RabbitTemplate rabbitTemplate, @Autowired final ObjectMapper objectMapper)
	{
		rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
		rabbitTemplate.setConfirmCallback((correlationData, ack, cause) ->
		{
			// 當 exchage 錯誤時，message 不會有值。當只有 routing key 錯誤時，message 才會有值
			if (correlationData != null)
			{
				if (ack)
				{
					// log.info("\t[Log] [Confirm] Message ID: [{}]", correlationData.getId());
				} else
				{
					log.error("\t[Log] [Confirm] \n\tMessage ID: {}\n\tError Cause: {}", correlationData.getId(), cause);
					if (correlationData.getReturned() != null) log.error("\t[Log] [Confirm] \n\tMessage Body: {}", new String(correlationData.getReturned().getMessage().getBody()));
				}
			}
		});
		rabbitTemplate.setReturnsCallback(returnedMessage ->
		{
			log.error("\t[Log] [Return] \n\tMessage Body: {}\n\tReplyCode: {}\n\tReplyText: {}\n\tExchange: {}\n\tRouting key: {}", new String(returnedMessage.getMessage().getBody()),
					returnedMessage.getReplyCode(), returnedMessage.getReplyText(), returnedMessage.getExchange(), returnedMessage.getRoutingKey());
		});

		return rabbitTemplate;
	}

	@Bean
	@Scope("prototype")
	public BatchingRabbitTemplate batchingRabbitTemplate(final ConnectionFactory connectionFactory)
	{
		// 已知條件
		// (1) 研華 SA RabbitMQ Queue 的限制條件： 最大允許筆數 2000， 最大保存容量 2MB
		// (2) 一般的 DM ---> Policy 封包約是 150 byte
		// (3) 雲平台的最大連線能力為 2000 連線 / 20 秒 = 100 連線/秒
		final int batchFrequency = 5;   // 訊息批量包發送的頻率，合法值區間為 1~5 (整數)。單位：秒。建議值 3 秒

		// 创建 BatchingStrategy 对象，代表批量策略，下面三個條件任一滿足，就會送出訊息批量包
		final int batchSize = batchFrequency * 100 * 120 / 100;           // 訊息批量包所允許的最大筆數。建議值 360 筆
		final int bufferLimit = batchFrequency * 100 * 150 * 120 / 100;   // 訊息批量包所允許的最大資料量，單位 byte。建議值 54 KB
		final int timeout = batchFrequency * 1_000;                       // 訊息批量包所允許的最大逾時時間，單位：毫秒。建議值 3 秒
		// 原先 SimpleBatchingStrategy 的 timeout 是指從最後 1 筆開始計時，而我需要的是從第 1 筆開始計時，所以，使用自定義的 BatchingStrategy
		final BatchingStrategy batchingStrategy = new NeoBatchingStrategy(batchSize, bufferLimit, timeout);

		// 建立 TaskScheduler 的物件，用於實現逾時發送的定時器
		final TaskScheduler taskScheduler = new ConcurrentTaskScheduler();

		// 建立 BatchingRabbitTemplate 的物件
		final BatchingRabbitTemplate batchTemplate = new BatchingRabbitTemplate(batchingStrategy, taskScheduler);
		batchTemplate.setConnectionFactory(connectionFactory);
		batchTemplate.setMessageConverter(new Jackson2JsonMessageConverter(this.objectMapper));

		return batchTemplate;
	}

	/**
	 * 接收 batch message 所需的工廠類別
	 */
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(final SimpleRabbitListenerContainerFactoryConfigurer configurer, final ConnectionFactory connectionFactory)
	{
		final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(new Jackson2JsonMessageConverter());
		factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		factory.setMaxConcurrentConsumers(16);
		factory.setConcurrentConsumers(6);

		factory.setBatchListener(true);
		factory.setConsumerBatchEnabled(true);
		factory.setDeBatchingEnabled(true);
		return factory;
	}
}
