import type { ChatApi, ChatMessage, ParticipantId } from '../models/chat';
const sampleText: [ParticipantId,string][] = [
  ['nova', 'Hey! Have you had a chance to look at the launch plan?'],
  ['alex', 'I have! The new direction looks really good. ✨'],
  ['nova', 'Glad you think so. I was hoping we could get it ready for Friday.'],
  ['alex', 'Friday might be a little tight. There are still a few things to work through.'],
  ['nova', 'Oh, I thought we were on the same page about the timeline.'],
  ['alex', 'We are. I just want to make sure we give it the attention it deserves.'],
  ['nova', 'Okay, let’s work out what’s realistic together.'],
];
export function sampleMessages(): ChatMessage[] {
  return sampleText.map(([speakerId,text],index) => ({id:`demo-${index+1}`, speakerId, text, sentAt: new Date(Date.UTC(2026,9,1,8,30+index)).toISOString()}));
}
export function createMockChatApi(): ChatApi {
  let messages = sampleMessages();
  return {
    async listMessages() { return messages.map(m => ({...m})); },
    async sendMessage(speakerId, text) {
      const trimmed = text.trim();
      if (!trimmed || trimmed.length > 2000) throw new Error('Messages must contain 1–2000 characters.');
      const message = {id:crypto.randomUUID(), speakerId, text:trimmed, sentAt:new Date().toISOString()};
      messages = [...messages, message];
      return {...message};
    },
    async reset(sample) { messages = sample ? sampleMessages() : []; return messages.map(m => ({...m})); },
  };
}
