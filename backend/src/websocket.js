const userConnections = new Map(); // Map<userId, Set<WebSocket>>
const wsUser = new Map(); // Map<WebSocket, userId>

export const setupWebSocket = (wss) => {
  wss.on("connection", (ws, req) => {
    console.log("📱 New WebSocket connection");

    ws.on("message", (message) => {
      try {
        const data = JSON.parse(message);

        if (data.type === "AUTH" && data.userId) {
          // If ws was previously authenticated to another user, remove it
          const prevUserId = wsUser.get(ws);
          if (prevUserId && prevUserId !== data.userId) {
            const prevSet = userConnections.get(prevUserId);
            if (prevSet) prevSet.delete(ws);
          }

          let connections = userConnections.get(data.userId);
          if (!connections) {
            connections = new Set();
            userConnections.set(data.userId, connections);
          }
          connections.add(ws);
          wsUser.set(ws, data.userId);

          console.log(
            `👤 User ${data.userId} connected via WebSocket (tabs: ${connections.size})`
          );
        }
      } catch (error) {
        console.error("WebSocket message error:", error);
      }
    });

    ws.on("close", () => {
      const userId = wsUser.get(ws);
      if (userId) {
        const connections = userConnections.get(userId);
        if (connections) {
          connections.delete(ws);
          if (connections.size === 0) {
            userConnections.delete(userId);
          }
          console.log(
            `👤 User ${userId} disconnected from WebSocket (remaining tabs: ${connections.size})`
          );
        }
        wsUser.delete(ws);
      }
    });

    ws.on("error", (error) => {
      console.error("WebSocket error:", error);
    });
  });
};

export const broadcastToUser = (userId, message) => {
  const connections = userConnections.get(userId);
  if (!connections) return;

  const payload = JSON.stringify(message);
  for (const ws of connections) {
    if (ws.readyState === 1) {
      ws.send(payload);
    }
  }
};
