/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.examples.gretty.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.ClientEndpoint;
import javax.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
@ServerEndpoint(value="/hello")
public class EventSocket {

  private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());

  private static final Logger log = LoggerFactory.getLogger(EventSocket.class);

  @OnOpen
  public void onWebSocketConnect(final Session session) {
    log.info("Socket Connected: {}", session);
    sessions.add(session);
  }
  
  @OnMessage
  public void onWebSocketText(final Session client, String message) throws Exception {
    log.info("Received TEXT message: {}", message);
    for( final Session session: sessions ) {
      if(session != client)
        session.getBasicRemote().sendText(message);
    }
  }

  @OnClose
  public void onWebSocketClose(final Session session, CloseReason reason) {
    log.info("Socket Closed: {}", reason);
    sessions.remove(session);
  }
  
  @OnError
  public void onWebSocketError(Throwable cause) {
    log.error("Socket Error", cause);
  }
}
