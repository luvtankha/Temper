import {type AnalysisApi,type ConversationAnalysis,type EmotionSignals,type LinguisticSignals} from '../models/analysis';
import type {ChatMessage} from '../models/chat';

// Hand-authored UI fixtures by turn position. Never inspect text or imply inference.
const fixtures=[
  {emotion:[.05,.02,.02,.38,.08,.12],signal:[.04,.02,.01,.03,.04,.02],sentiment:.45,conflict:.06},
  {emotion:[.02,.01,.01,.82,.02,.05],signal:[.02,.01,.01,.02,.02,.01],sentiment:.86,conflict:.03},
  {emotion:[.08,.02,.03,.51,.07,.2],signal:[.08,.02,.01,.05,.08,.03],sentiment:.38,conflict:.12},
  {emotion:[.38,.13,.12,.08,.21,.63],signal:[.56,.09,.02,.18,.34,.14],sentiment:-.46,conflict:.39},
  {emotion:[.64,.32,.22,.04,.48,.51],signal:[.73,.22,.06,.36,.51,.42],sentiment:-.63,conflict:.58},
  {emotion:[.41,.18,.12,.11,.18,.48],signal:[.39,.08,.02,.14,.43,.21],sentiment:-.25,conflict:.34},
  {emotion:[.16,.06,.05,.48,.1,.25],signal:[.13,.03,.01,.04,.12,.05],sentiment:.41,conflict:.13},
];
const emotionKeys=['frustration','anger','sadness','happiness','confusion','concern'] as const;
const signalKeys=['negativeSentiment','sarcasm','toxicity','passiveAggression','defensiveness','blame'] as const;
export function mockSnapshot(messages:readonly ChatMessage[]):ConversationAnalysis {
  const analyzed=messages.map((message,index)=> {
    const fixture=fixtures[index%fixtures.length];
    return {messageId:message.id,sequence:index+1,speakerId:message.speakerId,sentAt:message.sentAt,
      emotions:Object.fromEntries(emotionKeys.map((key,i)=>[key,fixture.emotion[i]])) as EmotionSignals,
      signals:Object.fromEntries(signalKeys.map((key,i)=>[key,fixture.signal[i]])) as LinguisticSignals,
      sentiment:fixture.sentiment,conflict:fixture.conflict};
  });
  const last=analyzed.at(-1),previous=analyzed.at(-2);
  const peak=analyzed.reduce<typeof last>((highest,row)=>!highest||row.conflict>highest.conflict?row:highest,undefined);
  const escalation=analyzed.find(row=>row.conflict>=.35),recovery=analyzed.find(row=>row.sequence===7);
  return {conversationId:'local-demo',mode:'MOCK',messages:analyzed,conflictScore:last?.conflict??0,sentiment:last?.sentiment??0,
    emotionalIntensity:last?Math.max(...Object.values(last.emotions)):0,
    direction:!last||!previous||Math.abs(last.conflict-previous.conflict)<.08?'steady':last.conflict>previous.conflict?'escalating':'recovering',
    escalationStart:escalation?.sequence??null,peakTension:peak?.sequence??null,
    events:[...(escalation?[{sequence:escalation.sequence,messageId:escalation.messageId,kind:'escalation' as const,label:'Escalation / major shift'}]:[]),...(peak?[{sequence:peak.sequence,messageId:peak.messageId,kind:'peak' as const,label:'Peak tension'}]:[]),...(recovery?[{sequence:recovery.sequence,messageId:recovery.messageId,kind:'recovery' as const,label:'Recovery'}]:[])]};
}
export const mockAnalysisApi:AnalysisApi={
  async getConversation(messages,signal) {
    await new Promise<void>((resolve,reject)=> {
      if(signal?.aborted){reject(new DOMException('Aborted','AbortError'));return;}
      const abort=()=>{clearTimeout(timer);reject(new DOMException('Aborted','AbortError'));};
      const timer=setTimeout(()=>{signal?.removeEventListener('abort',abort);resolve();},180);
      signal?.addEventListener('abort',abort,{once:true});
    });
    return mockSnapshot(messages);
  },
};
