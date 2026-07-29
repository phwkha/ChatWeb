import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketClient {
  constructor() {
    this.stompClient = null;
    this.isConnected = false;
    this.callbacks = new Map();
  }

  connect(token, onConnectCallback) {
    if (this.isConnected) return;

    const serverUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    
    // Spring WebSocket with SockJS fallback
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(`${serverUrl}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: function (str) {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      this.isConnected = true;
      console.log('Connected: ' + frame);

      // Subscribe to user specific topics
      // e.g. /user/queue/messages, /user/queue/friends, /user/queue/errors
      
      this.stompClient.subscribe('/user/queue/messages', (message) => {
        if (message.body && this.callbacks.has('onMessageReceived')) {
          this.callbacks.get('onMessageReceived')(JSON.parse(message.body));
        }
      });

      this.stompClient.subscribe('/user/queue/errors', (message) => {
        console.error("WS Error:", message.body);
      });

      if (onConnectCallback) onConnectCallback();
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient !== null) {
      this.stompClient.deactivate();
    }
    this.isConnected = false;
  }

  on(event, callback) {
    this.callbacks.set(event, callback);
  }

  sendMessage(destination, body) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: destination,
        body: JSON.stringify(body)
      });
    } else {
      console.error("Cannot send message, WebSocket is not connected");
    }
  }
}

const wsClient = new WebSocketClient();
export default wsClient;
