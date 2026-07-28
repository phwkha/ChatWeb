import { useState, useRef, useCallback } from "react";

export const useChatPagination = (fetchApiFn) => {
    const [messages, setMessages] = useState([]);
    const [nextCursor, setNextCursor] = useState(null);
    const [hasMore, setHasMore] = useState(true);
    const [isLoadingHistory, setIsLoadingHistory] = useState(false);
    
    // Track unique message IDs to prevent duplicates
    const messageIdsRef = useRef(new Set());

    const fetchHistory = useCallback(async (cursor, size = 20) => {
        try {
            const data = await fetchApiFn(cursor, size);
            if (data && Array.isArray(data.content)) {
                const newMessages = data.content
                    .map((m) => ({
                        ...m,
                        messageType: m.messageType || "CHAT",
                        timestamp: m.timestamp || new Date().toISOString(),
                        id: m.id || `msg-${Date.now()}-${Math.random()}`,
                    }))
                    .reverse();

                const uniqueMessages = newMessages.filter(
                    (m) => !messageIdsRef.current.has(m.id)
                );
                uniqueMessages.forEach((m) => messageIdsRef.current.add(m.id));

                return {
                    messages: uniqueMessages,
                    nextCursor: data.nextCursor,
                    hasMore: data.hasMore,
                };
            }
            return null;
        } catch (error) {
            console.error("Error fetching history:", error);
            return null;
        }
    }, [fetchApiFn]);

    const loadInitialHistory = useCallback(async () => {
        const initialData = await fetchHistory(null, 20);
        if (initialData) {
            setMessages(initialData.messages);
            setNextCursor(initialData.nextCursor);
            setHasMore(initialData.hasMore);
        }
    }, [fetchHistory]);

    return {
        messages,
        setMessages,
        nextCursor,
        setNextCursor,
        hasMore,
        setHasMore,
        isLoadingHistory,
        setIsLoadingHistory,
        messageIdsRef,
        fetchHistory,
        loadInitialHistory
    };
};
