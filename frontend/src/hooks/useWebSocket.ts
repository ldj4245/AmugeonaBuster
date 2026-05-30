import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketRoomResponse {
  roomId: string;
  hostId: string;
  location: string;
  status: 'LOBBY' | 'PLAYING' | 'COMPLETED';
  members: Array<{ id: string; nickname: string; isReady: boolean }>;
  winningMenu?: string;
  matchedRestaurants: Array<{
    id: string;
    name: string;
    address: string;
    latitude: number;
    longitude: number;
    phone?: string;
    category?: string;
    placeUrl?: string;
  }>;
  defaultMenus: string[];
  totalMembers: number;
  completedMembersCount: number;
  maxSwipeCount: number;
  voteStats?: Array<{
    menuName: string;
    likes: number;
    dislikes: number;
  }>;
}

export function useWebSocket(
  roomId: string | null,
  onMessageReceived: (roomState: WebSocketRoomResponse) => void
) {
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!roomId) return;

    const isLocalDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    const socketUrl = isLocalDev
      ? 'http://localhost:8080/ws-connection'
      : `${window.location.origin}/ws-connection`;
    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('✅ WebSocket Connected!');
      
      // 실시간 방 토픽 구독 개시
      client.subscribe(`/topic/rooms/${roomId}`, (message) => {
        if (message.body) {
          try {
            const roomState: WebSocketRoomResponse = JSON.parse(message.body);
            onMessageReceived(roomState);
          } catch (e) {
            console.error('❌ Error parsing WebSocket message body:', e);
          }
        }
      });
    };

    client.onStompError = (frame) => {
      console.error('❌ STOMP error details:', frame.headers['message']);
    };

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        console.log('🔌 WebSocket Disconnected!');
      }
    };
  }, [roomId, onMessageReceived]);

  return stompClientRef.current;
}
