import { useEffect, useRef, useState } from "react";
import { Stomp } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export const useWebSocket = (username) => {
    const stompClient = useRef(null);
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        if (!username) return;
        let reconnectInterval;

        const connect = () => {
            stompClient.current = Stomp.over(() => new SockJS(`${import.meta.env.VITE_SOCKET_URL}`));
            // Hide debug logs if not needed
            // stompClient.current.debug = () => {};
            
            stompClient.current.connect(
                { "client-id": username, username: username },
                () => {
                    console.log("STOMP connected");
                    setIsConnected(true);
                    clearInterval(reconnectInterval);
                },
                (error) => {
                    console.error("STOMP error:", error);
                    setIsConnected(false);
                    if (!reconnectInterval) {
                        reconnectInterval = setInterval(connect, 5000);
                    }
                }
            );
        };

        connect();

        return () => {
            if (stompClient.current?.connected) {
                stompClient.current.disconnect();
            }
            setIsConnected(false);
            clearInterval(reconnectInterval);
        };
    }, [username]);

    return { stompClient, isConnected };
};
