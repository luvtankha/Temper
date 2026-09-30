import {request} from './client';
import type {ChatApi,ChatMessage,ParticipantId} from '../models/chat';
import {Client} from '@stomp/stompjs';
import type {AnalysisApi,ConversationAnalysis} from '../models/analysis';
export const demoConversationId='00000000-0000-4000-8000-000000000001';
export interface BackendSession {id:string}
export function createBackendAdapters(initialId=demoConversationId,live=false) {
  const session:BackendSession={id:initialId};
  let client:Client|null=null;
  const pending=new Map<string,{resolve:(message:ChatMessage)=>void;reject:(error:Error)=>void;timer:ReturnType<typeof setTimeout>}>();
  const failPending=()=>{for(const p of pending.values()){clearTimeout(p.timer);p.reject(new Error('Connection interrupted. Check delivered messages before retrying.'));}pending.clear();};
  const chat:ChatApi={
    listMessages:()=>request<ChatMessage[]>(`/conversations/${session.id}/messages`),
    sendMessage:(speakerId:ParticipantId,text:string)=>{
      if(!live)return request<ChatMessage>(`/conversations/${session.id}/messages`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({speakerId,text})});
      if(!client?.connected)return Promise.reject(new Error('Wait for the live connection to resume.'));
      const requestId=crypto.randomUUID();return new Promise<ChatMessage>((resolve,reject)=>{
        const timer=setTimeout(()=>{pending.delete(requestId);reject(new Error('Delivery confirmation timed out. Check the conversation before retrying.'));},10000);
        pending.set(requestId,{resolve,reject,timer});client!.publish({destination:`/app/conversations/${session.id}/send`,body:JSON.stringify({requestId,text})});
      });
    },
    async reset(sample:boolean) {
      if(sample){session.id=demoConversationId;return chat.listMessages();}
      const conversation=await request<{id:string}>('/conversations',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({title:'A new conversation',participantIds:['alex','nova']})});
      session.id=conversation.id;return [];
    },
  };
  if(live){
    chat.connect=(speakerId,events)=>{
      const room=session.id;let active=true,hasConnected=false;
      const url=import.meta.env.VITE_WS_URL??`${location.protocol==='https:'?'wss':'ws'}://${location.host}/ws`;
      const socket=new Client({brokerURL:url,connectHeaders:{conversationId:room,speakerId},reconnectDelay:1500,heartbeatIncoming:10000,heartbeatOutgoing:10000});client=socket;
      events.connection('connecting');
      socket.onConnect=()=>{
        if(!active)return;hasConnected=true;events.connection('connected');
        socket.subscribe(`/topic/conversations/${room}/messages`,frame=>{
          if(!active||session.id!==room)return;const {message,requestId}=JSON.parse(frame.body) as {message:ChatMessage;requestId:string|null};events.message(message);
          const p=requestId?pending.get(requestId):undefined;if(p&&requestId){clearTimeout(p.timer);pending.delete(requestId);p.resolve(message);}
        });
        socket.subscribe(`/topic/conversations/${room}/typing`,frame=>{if(active){const t=JSON.parse(frame.body);events.typing(t.speakerId,t.typing);}});
        socket.subscribe(`/topic/conversations/${room}/presence`,frame=>{if(active)events.presence(JSON.parse(frame.body).onlineParticipantIds);});
        socket.subscribe('/user/queue/errors',frame=>{if(active){const e=JSON.parse(frame.body);const p=pending.get(e.requestId);if(p){clearTimeout(p.timer);pending.delete(e.requestId);p.reject(new Error(e.message));}else events.error(e.message);}});
        socket.publish({destination:`/app/conversations/${room}/presence`,body:'{}'});
        // Subscribe first, then merge the REST snapshot so reconnect cannot miss a turn.
        request<ChatMessage[]>(`/conversations/${room}/messages`).then(messages=>{if(active&&session.id===room)events.snapshot(messages);}).catch(error=>{if(active)events.error(error.message);});
      };
      socket.onWebSocketClose=()=>{if(active){failPending();events.typing(speakerId==='alex'?'nova':'alex',false);events.presence([]);events.connection(hasConnected?'reconnecting':'connecting');}};
      socket.onStompError=()=>{if(active){failPending();events.connection('error');events.error('Live connection rejected. Check conversation membership.');}};
      socket.activate();return()=>{active=false;failPending();if(client===socket)client=null;void socket.deactivate();};
    };
    chat.setTyping=typing=>{if(client?.connected)client.publish({destination:`/app/conversations/${session.id}/typing`,body:JSON.stringify({typing})});};
  }
  const analysis:AnalysisApi={
    async getConversation(messages,signal) {
      // Phase 11 explicit MOCK request; delivery already completed independently.
      for(const message of messages){if(signal?.aborted)throw new DOMException('Aborted','AbortError');await request(`/messages/${message.id}/analyze`,{method:'POST',signal});}
      return request<ConversationAnalysis>(`/conversations/${session.id}/analytics`,{signal});
    },
  };
  return {chat,analysis,session};
}
