package solaris.nfm.capability.message.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@ServerEndpoint("/ws/{tenantId}")
@Slf4j
public class WebSocketService
{
	// 紀錄已經連線 session 的數量，使用線程安全的 AtomicInteger
	private static AtomicInteger		onlineCount			= new AtomicInteger(0);
	// 儲存已經連線的 session
	// private static Queue<Session> sessionQueue = new ConcurrentLinkedQueue<>();
	// 建立 session id -> session 對應 Map
	private static Map<Session, Long>	sessionTenantMap	= new ConcurrentHashMap<>(10);

	/**
	 * WebSocket 連線建立時的回調方法
	 */
	@OnOpen
	public void onOpen(final Session session, @PathParam("tenantId") final Long tenantId)
	{
		if (tenantId < 1L)
		{
			log.error("\t [WebSocket] Tenant ID ({}) must be greater than 0.", tenantId);
			return;
		}

		// sessionQueue.add(session);
		sessionTenantMap.put(session, tenantId);
		onlineCount.getAndIncrement();
		// log.info("\t [WebSocket] 新連線 ({}, {}) 加入，目前在線數量：{}", tenantId, session.getId(), onlineCount);
	}

	/**
	 * WebSocket 連線關閉時的回調方法
	 */
	@OnClose
	public void onClose(final Session session, @PathParam("tenantId") final Long tenantId)
	{
		if (tenantId < 1L)
		{
			log.error("\t [WebSocket] Tenant ID ({}) must be greater than 0.", tenantId);
			return;
		}
		// sessionQueue.remove(session);
		sessionTenantMap.remove(session);
		onlineCount.getAndDecrement();
		// log.info("\t [WebSocket] 連線 ({}, {}) 關閉，目前在線數量：{}", tenantId, session.getId(), onlineCount);
	}

	/**
	 * 發生錯誤時的回調方法
	 */
	@OnError
	public void onError(final Session session, final Throwable throwable, @PathParam("tenantId") final Long tenantId)
	{
		// sessionQueue.remove(session);
		sessionTenantMap.remove(session);
		onlineCount.getAndDecrement();
		log.error("\t [WebSocket] 連線 ({}, {}) 發生錯誤，原因：{}", tenantId, session.getId(), throwable.getMessage());
		throwable.printStackTrace();
	}

	/**
	 * 收到客户端訊息時的回調方法
	 */
	@OnMessage
	public void onMessage(final Session session, final String message, @PathParam("tenantId") final Long tenantId) throws Exception
	{
		// log.debug("\t [WebSocket] 收到客户端 ({}, {}) 的訊息：{}", tenantId, session.getId(), message);
		// sendMessage(session, "reply" + message);
		// broadcastAll(1L, "broadcast: " + message);
	}

	/**
	 * 向指定的 session 推送訊息
	 */
	public void sendMessage(final Session session, final String msg)
	{
		try
		{
			session.getBasicRemote().sendText(msg);
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 向指定的 tenant ID 推送全體廣播訊息
	 */
	public void broadcastAll(final Long tenantId, final String msg)
	{
		for (final Session session : sessionTenantMap.keySet())
		{
			if (tenantId == sessionTenantMap.get(session)) sendMessage(session, msg);
		}
	}

	/**
	 * 向全體 session 廣播訊息
	 */
	public void broadcastAll(final String msg)
	{
		for (final Session session : sessionTenantMap.keySet())
		{
			sendMessage(session, msg);
		}
	}
}