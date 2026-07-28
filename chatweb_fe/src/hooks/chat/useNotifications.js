import { useState, useCallback } from "react";

export const useNotifications = (onNotificationClick) => {
    const [notifications, setNotifications] = useState([]);
    const [unreadMessages, setUnreadMessages] = useState(new Map());

    const removeNotification = useCallback((id) => {
        setNotifications((prev) => prev.filter((n) => n.id !== id));
    }, []);

    const showNotification = useCallback(
        (sender, content = "Bạn có tin nhắn mới") => {
            const id = Date.now();
            const newNotification = { id, sender, content };
            setNotifications((prev) => [...prev, newNotification]);
            setTimeout(() => removeNotification(id), 5000);
        },
        [removeNotification]
    );

    const handleNotificationClick = useCallback((sender) => {
        if (onNotificationClick) onNotificationClick(sender);
        setNotifications((prev) => prev.filter((n) => n.sender !== sender));
    }, [onNotificationClick]);

    const incrementUnread = useCallback((sender) => {
        setUnreadMessages((prev) => {
            const newUnread = new Map(prev);
            newUnread.set(sender, (newUnread.get(sender) || 0) + 1);
            return newUnread;
        });
    }, []);

    const clearUnread = useCallback((sender) => {
        setUnreadMessages((prev) => {
            const newUnread = new Map(prev);
            newUnread.delete(sender);
            return newUnread;
        });
    }, []);

    return {
        notifications,
        unreadMessages,
        setUnreadMessages,
        showNotification,
        removeNotification,
        handleNotificationClick,
        incrementUnread,
        clearUnread
    };
};
