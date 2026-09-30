import {describe,it,expect} from 'vitest';
import {mockSnapshot,mockAnalysisApi} from './analysisApi';
import {sampleMessages} from './chatApi';
describe('explicit mock analysis contract',()=> {
  it('preserves message/speaker identity, normalized signals and known turning points',()=> {
    const messages=sampleMessages();const result=mockSnapshot(messages);
    expect(result.mode).toBe('MOCK');expect(result.messages.map(m=>m.messageId)).toEqual(messages.map(m=>m.id));
    expect(result.messages.map(m=>m.speakerId)).toEqual(messages.map(m=>m.speakerId));
    expect(result).toMatchObject({escalationStart:4,peakTension:5,direction:'recovering'});
    for(const row of result.messages)for(const value of [...Object.values(row.emotions),...Object.values(row.signals),row.conflict])expect(value>=0&&value<=1).toBe(true);
  });
  it('does not analyze or mutate message text; fixtures depend only on ordinal',()=> {
    const original=sampleMessages();const changed=original.map(m=>({...m,text:'Completely different private content'}));
    expect(mockSnapshot(changed)).toEqual(mockSnapshot(original));expect(original[0].text).toContain('launch plan');
    expect(mockSnapshot([])).toMatchObject({messages:[],conflictScore:0,escalationStart:null,peakTension:null});
  });
  it('honors cancellation instead of returning a stale snapshot',async()=> {
    const controller=new AbortController();const promise=mockAnalysisApi.getConversation(sampleMessages(),controller.signal);controller.abort();
    await expect(promise).rejects.toMatchObject({name:'AbortError'});
  });
});
