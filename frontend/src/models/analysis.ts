import type {ChatMessage,ParticipantId} from './chat';
export const emotionLabels={frustration:'Frustration',anger:'Anger',sadness:'Sadness',happiness:'Happiness',confusion:'Confusion',concern:'Concern'} as const;
export const signalLabels={negativeSentiment:'Negative sentiment',sarcasm:'Sarcasm',toxicity:'Toxicity',passiveAggression:'Passive aggression',defensiveness:'Defensiveness',blame:'Blame'} as const;
export type EmotionSignals=Record<keyof typeof emotionLabels,number>&{surprise?:number;neutral?:number};
export type LinguisticSignals=Record<keyof typeof signalLabels,number>;
export interface MessageAnalysis {
  messageId:string;sequence:number;speakerId:ParticipantId;sentAt:string;
  emotions:EmotionSignals;signals:LinguisticSignals;sentiment:number;conflict:number;
  explanation:string;contextMessageIds:string[];
  evidence:{source:'MOCK'|'MODEL'|'HEURISTIC';label:string;description:string}[];
}
export interface ConversationAnalysis {
  conversationId:string;mode:'MOCK'|'MODEL'|'HYBRID';messages:MessageAnalysis[];
  conflictScore:number;sentiment:number;emotionalIntensity:number;
  direction:'steady'|'escalating'|'recovering';escalationStart:number|null;peakTension:number|null;
  events:{sequence:number;messageId:string;kind:'escalation'|'peak'|'recovery';label:string}[];
}
export interface AnalysisApi {
  getConversation(messages:readonly ChatMessage[],signal?:AbortSignal):Promise<ConversationAnalysis>;
}
export const percent=(value:number)=>Math.round(Math.max(0,Math.min(1,Number.isFinite(value)?value:0))*100);
