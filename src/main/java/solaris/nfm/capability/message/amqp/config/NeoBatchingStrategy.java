package solaris.nfm.capability.message.amqp.config;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.batch.BatchingStrategy;
import org.springframework.amqp.rabbit.batch.MessageBatch;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.util.Assert;

/**
 * @author Holisun Wu
 */
public class NeoBatchingStrategy implements BatchingStrategy
{

	private final int			batchSize;

	private final int			bufferLimit;

	private final long			timeout;

	private final List<Message>	messages				= new ArrayList<>();

	private String				exchange;

	private String				routingKey;

	private int					currentSize;

	private long				batchReadyToSendMillis	= 0;

	/**
	 * @param batchSize
	 *        the batch size.
	 * @param bufferLimit
	 *        the max buffer size; could trigger a short batch. Does not apply
	 *        to a single message.
	 * @param timeout
	 *        the batch timeout.
	 */
	public NeoBatchingStrategy(final int batchSize, final int bufferLimit, final long timeout)
	{
		this.batchSize = batchSize;
		this.bufferLimit = bufferLimit;
		this.timeout = timeout;
	}

	@Override
	public MessageBatch addToBatch(final String exch, final String routKey, final Message message)
	{
		if (this.exchange != null)
		{
			Assert.isTrue(this.exchange.equals(exch), "Cannot send to different exchanges in the same batch");
		} else
		{
			this.exchange = exch;
		}
		if (this.routingKey != null)
		{
			Assert.isTrue(this.routingKey.equals(routKey), "Cannot send with different routing keys in the same batch");
		} else
		{
			this.routingKey = routKey;
		}
		final int bufferUse = Integer.BYTES + message.getBody().length;
		MessageBatch batch = null;
		if (this.messages.size() > 0 && this.currentSize + bufferUse > this.bufferLimit)
		{
			batch = doReleaseBatch();
			this.exchange = exch;
			this.routingKey = routKey;
			this.batchReadyToSendMillis = 0;
		}
		this.currentSize += bufferUse;
		this.messages.add(message);
		if (batch == null && (this.messages.size() >= this.batchSize || this.currentSize >= this.bufferLimit))
		{
			batch = doReleaseBatch();
			this.batchReadyToSendMillis = 0;
		}
		return batch;
	}

	@Override
	public Date nextRelease()
	{
		// log.debug("this.messages.size()=[{}]", this.messages.size());
		if (this.messages.size() == 0 || this.timeout <= 0)
		{
			this.batchReadyToSendMillis = 0;
			return null;
		}
		if (this.currentSize >= this.bufferLimit)
		{
			this.batchReadyToSendMillis = 0;
			// release immediately, we're already over the limit
			return new Date();
		} else
		{
			// 此處我猜測：有個時間檢查機制，會不斷調用 nextRelease()，檢查 due date 是否已到。若是，則立即送出 batch
			// 此處的邏輯為只要有新的 message 加入，due date 就重新設置。
			// 現在此處邏輯應該修改為只有當第 1 筆 message 加入時，才會重設 due date。
			// nextRelease() 的回傳值是個時間，我猜會被其他程序拿來倒數或是比對，所以，當 message.size() > 1 時，不能回傳 null，應該回傳時間
			// return new Date(System.currentTimeMillis() + this.timeout);

			if (this.messages.size() == 1)
			{
				this.batchReadyToSendMillis = System.currentTimeMillis() + this.timeout;
			}
			// log.debug("this.batchReadyToSendMillis=[{}]", this.batchReadyToSendMillis);

			return new Date(this.batchReadyToSendMillis);
		}
	}

	@Override
	public Collection<MessageBatch> releaseBatches()
	{
		final MessageBatch batch = doReleaseBatch();
		if (batch == null)
		{
			return Collections.emptyList();
		}
		return Collections.singletonList(batch);
	}

	private MessageBatch doReleaseBatch()
	{
		if (this.messages.size() < 1)
		{
			return null;
		}
		final Message message = assembleMessage();
		final MessageBatch messageBatch = new MessageBatch(this.exchange, this.routingKey, message);
		this.messages.clear();
		this.currentSize = 0;
		this.exchange = null;
		this.routingKey = null;
		return messageBatch;
	}

	private Message assembleMessage()
	{
		if (this.messages.size() == 1)
		{
			return this.messages.get(0);
		}
		final MessageProperties messageProperties = this.messages.get(0).getMessageProperties();
		final byte[] body = new byte[this.currentSize];
		final ByteBuffer bytes = ByteBuffer.wrap(body);
		for (final Message message : this.messages)
		{
			bytes.putInt(message.getBody().length);
			bytes.put(message.getBody());
		}
		messageProperties.getHeaders().put(MessageProperties.SPRING_BATCH_FORMAT, MessageProperties.BATCH_FORMAT_LENGTH_HEADER4);
		messageProperties.getHeaders().put(AmqpHeaders.BATCH_SIZE, this.messages.size());
		// messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		return new Message(body, messageProperties);
	}

	@Override
	public boolean canDebatch(final MessageProperties properties)
	{
		return MessageProperties.BATCH_FORMAT_LENGTH_HEADER4.equals(properties.getHeaders().get(MessageProperties.SPRING_BATCH_FORMAT));
	}

	/**
	 * Debatch a message that has a header with {@link MessageProperties#SPRING_BATCH_FORMAT}
	 * set to {@link MessageProperties#BATCH_FORMAT_LENGTH_HEADER4}.
	 *
	 * @param message
	 *        the batched message.
	 * @param fragmentConsumer
	 *        a consumer for each fragment.
	 * @since 2.2
	 */
	@Override
	public void deBatch(final Message message, final Consumer<Message> fragmentConsumer)
	{
		final ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBody());
		final MessageProperties messageProperties = message.getMessageProperties();
		messageProperties.getHeaders().remove(MessageProperties.SPRING_BATCH_FORMAT);
		while (byteBuffer.hasRemaining())
		{
			final int length = byteBuffer.getInt();
			if (length < 0 || length > byteBuffer.remaining())
			{
				throw new ListenerExecutionFailedException("Bad batched message received", new MessageConversionException("Insufficient batch data at offset " + byteBuffer.position()), message);
			}
			final byte[] body = new byte[length];
			byteBuffer.get(body);
			messageProperties.setContentLength(length);
			// messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			// Caveat - shared MessageProperties.
			final Message fragment = new Message(body, messageProperties);
			if (!byteBuffer.hasRemaining())
			{
				messageProperties.setLastInBatch(true);
			}
			fragmentConsumer.accept(fragment);
		}
	}
}
