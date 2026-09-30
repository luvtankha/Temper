import {describe,it,expect} from 'vitest';
import {speakerSummaries,emotionDistribution} from './dashboardData';
import {mockSnapshot} from '../../mocks/analysisApi';
import {sampleMessages} from '../../mocks/chatApi';
describe('dashboard aggregation',()=> {
  it('assigns messages and escalation markers to the right speakers; contribution and mix normalize',()=> {
    const snapshot=mockSnapshot(sampleMessages());const [alex,nova]=speakerSummaries(snapshot);
    expect(alex.messageCount).toBe(3);expect(nova.messageCount).toBe(4);
    expect(alex.escalationPoints.map(e=>e.sequence)).toEqual([4]);expect(nova.escalationPoints.map(e=>e.sequence)).toEqual([5]);
    expect(alex.conflictContribution!+nova.conflictContribution!).toBeCloseTo(1);
    expect(emotionDistribution(snapshot).reduce((sum,row)=>sum+row.value,0)).toBeCloseTo(1);
  });
  it('keeps missing speaker estimates distinct from measured zeros',()=> {
    const empty=speakerSummaries(mockSnapshot([]));expect(empty.every(s=>s.averageSentiment===null&&s.dominantEmotion===null&&s.conflictContribution===null)).toBe(true);
    const one=speakerSummaries(mockSnapshot(sampleMessages().slice(0,1)));expect(one[0].messageCount).toBe(0);expect(one[0].sarcasm).toBeNull();expect(one[1].messageCount).toBe(1);
  });
});
