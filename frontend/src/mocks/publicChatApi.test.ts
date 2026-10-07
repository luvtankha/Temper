import {describe, expect, it} from 'vitest';
import {createMockChatApi, fictionalDemoTurns, publicDemoTurnLimit} from './chatApi';

describe('bounded public fictional conversation',()=>{
  it('rejects arbitrary text and accepts only supplied sample turns',async()=>{
    const api=createMockChatApi({fictionalOnly:true});
    await expect(api.sendMessage('alex','A private conversation')).rejects.toThrow('only the supplied fictional sample turns');
    await expect(api.sendMessage('alex',fictionalDemoTurns[0])).resolves.toMatchObject({text:fictionalDemoTurns[0]});
    expect(await api.listMessages()).toHaveLength(8);
  });
  it('caps the in-memory sample and lets the visitor reset it',async()=>{
    const api=createMockChatApi({fictionalOnly:true});
    const original=await api.listMessages();
    for(let index=original.length;index<publicDemoTurnLimit;index++)await api.sendMessage('nova',fictionalDemoTurns[1]);
    await expect(api.sendMessage('nova',fictionalDemoTurns[1])).rejects.toThrow('32 turns');
    expect(await api.listMessages()).toHaveLength(publicDemoTurnLimit);
    await api.reset(true);
    expect(await api.listMessages()).toHaveLength(original.length);
  });
});
