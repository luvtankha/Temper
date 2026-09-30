import {emotionLabels,type ConversationAnalysis,type EmotionSignals} from '../../models/analysis';
import {participants} from '../../models/chat';
type EmotionKey=keyof EmotionSignals;
const average=(values:number[])=>values.length?values.reduce((sum,value)=>sum+value,0)/values.length:null;
export function speakerSummaries(snapshot:ConversationAnalysis) {
  const totalConflict=snapshot.messages.reduce((sum,row)=>sum+row.conflict,0);
  return participants.map(participant=> {
    const rows=snapshot.messages.filter(row=>row.speakerId===participant.id);
    const emotions=Object.keys(emotionLabels).map(key=>({key:key as EmotionKey,value:average(rows.map(row=>row.emotions[key as EmotionKey]))??0})).sort((a,b)=>b.value-a.value);
    return {...participant,messageCount:rows.length,dominantEmotion:rows.length?emotions[0].key:null,
      averageSentiment:average(rows.map(row=>row.sentiment)),conflictContribution:totalConflict>0?rows.reduce((sum,row)=>sum+row.conflict,0)/totalConflict:null,
      sarcasm:average(rows.map(row=>row.signals.sarcasm)),toxicity:average(rows.map(row=>row.signals.toxicity)),
      escalationPoints:snapshot.events.filter(event=>event.kind!=='recovery'&&rows.some(row=>row.messageId===event.messageId))};
  });
}
export function emotionDistribution(snapshot:ConversationAnalysis) {
  const totals=Object.entries(emotionLabels).map(([key,label])=>({key,label,total:snapshot.messages.reduce((sum,row)=>sum+row.emotions[key as EmotionKey],0)}));
  const sum=totals.reduce((sum,row)=>sum+row.total,0);
  return totals.map(row=>({...row,value:sum?row.total/sum:0}));
}
