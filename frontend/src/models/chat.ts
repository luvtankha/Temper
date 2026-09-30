export type ParticipantId = 'alex' | 'nova';
export interface Participant { id: ParticipantId; name: string; variant: 'male' | 'female'; }
export interface ChatMessage { id: string; speakerId: ParticipantId; text: string; sentAt: string; sequence?:number; }
export type ConnectionState='local'|'rest'|'connecting'|'connected'|'reconnecting'|'error';
export interface ChatStream {
  message:(message:ChatMessage)=>void;snapshot:(messages:ChatMessage[])=>void;
  typing:(speakerId:ParticipantId,typing:boolean)=>void;presence:(ids:ParticipantId[])=>void;
  connection:(state:ConnectionState)=>void;error:(message:string)=>void;
}
export interface ChatApi {
  listMessages(): Promise<ChatMessage[]>;
  sendMessage(speakerId: ParticipantId, text: string): Promise<ChatMessage>;
  reset(sample: boolean): Promise<ChatMessage[]>;
  connect?(speakerId:ParticipantId,events:ChatStream):()=>void;
  setTyping?(typing:boolean):void;
}
export const participants: Participant[] = [
  {id:'alex', name:'Alex', variant:'male'},
  {id:'nova', name:'Nova', variant:'female'},
];
export function remoteParticipant(localId: ParticipantId): Participant {
  return participants.find(p => p.id !== localId)!;
}
