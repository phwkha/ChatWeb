// Simple PubSub for Toast Notifications
// This allows triggering toasts from anywhere, including Axios interceptors!

const listeners = new Set();

export const toast = {
  success: (message) => notify({ type: 'success', message }),
  error: (message) => notify({ type: 'error', message }),
  info: (message) => notify({ type: 'info', message }),
  warning: (message) => notify({ type: 'warning', message })
};

function notify(toastData) {
  const id = Date.now().toString();
  const newToast = { id, ...toastData };
  listeners.forEach(listener => listener(newToast));
}

export const subscribeToToasts = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};
