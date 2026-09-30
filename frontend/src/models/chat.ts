export type ParticipantId = 'alex' | 'nova';
export interface Participant { id: ParticipantId; name: string; variant: 'male' | 'female'; }
export interface ChatMessage { id: string; speakerId: ParticipantId; text: string; sentAt: string; }
export interface ChatApi {
  listMessages(): Promise<ChatMessage[]>;
  sendMessage(speakerId: ParticipantId, text: string): Promise<ChatMessage>;
  reset(sample: boolean): Promise<ChatMessage[]>;
}
export const participants: Participant[] = [
  {id:'alex', name:'Alex', variant:'male'},
  {id:'nova', name:'Nova', variant:'female'},
];
export function remoteParticipant(localId: ParticipantId): Participant {
  return participants.find(p => p.id !== localId)!;
}
