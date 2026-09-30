import {request} from './client';
import type {ChatApi,ChatMessage,ParticipantId} from '../models/chat';
import type {AnalysisApi,ConversationAnalysis} from '../models/analysis';
export const demoConversationId='00000000-0000-4000-8000-000000000001';
export interface BackendSession {id:string}
export function createBackendAdapters(initialId=demoConversationId) {
  const session:BackendSession={id:initialId};
  const chat:ChatApi={
    listMessages:()=>request<ChatMessage[]>(`/conversations/${session.id}/messages`),
    sendMessage:(speakerId:ParticipantId,text:string)=>request<ChatMessage>(`/conversations/${session.id}/messages`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({speakerId,text})}),
    async reset(sample:boolean) {
      if(sample){session.id=demoConversationId;return chat.listMessages();}
      const conversation=await request<{id:string}>('/conversations',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({title:'A new conversation',participantIds:['alex','nova']})});
      session.id=conversation.id;return [];
    },
  };
  const analysis:AnalysisApi={
    async getConversation(messages,signal) {
      // Phase 11 explicit MOCK request; delivery already completed independently.
      for(const message of messages){if(signal?.aborted)throw new DOMException('Aborted','AbortError');await request(`/messages/${message.id}/analyze`,{method:'POST',signal});}
      return request<ConversationAnalysis>(`/conversations/${session.id}/analytics`,{signal});
    },
  };
  return {chat,analysis,session};
}
