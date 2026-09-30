import {test,expect} from '@playwright/test';
test('real backend analyze route supplies bounded history and speaker identity without future leakage',async({request})=>{
  const created=await request.post('/api/v1/conversations',{data:{title:'Causal history test',participantIds:['alex','nova']}});const {id}=await created.json();const turns:{id:string}[]=[];
  for(let index=0;index<8;index++){const response=await request.post(`/api/v1/conversations/${id}/messages`,{data:{speakerId:index%2?'alex':'nova',text:index===6?'Fine.':`Fictional history ${index+1}`}});expect(response.status()).toBe(201);turns.push(await response.json());}
  const context=await(await request.get(`/api/v1/messages/${turns[6].id}/context`)).json();expect(context.current).toMatchObject({messageId:turns[6].id,text:'Fine.',speaker:{id:'nova',displayName:'Nova'}});expect(context.previous.map((t:{messageId:string})=>t.messageId)).toEqual(turns.slice(1,6).map(t=>t.id));
  const analyzed=await(await request.post(`/api/v1/messages/${turns[6].id}/analyze`)).json();expect(analyzed.mode).toBe('MOCK');expect(analyzed.contextMessageIds).toEqual(turns.slice(1,6).map(t=>t.id));expect(analyzed.contextMessageIds).not.toContain(turns[7].id);expect(analyzed.evidence.some((e:{description:string})=>e.description.includes('mock does not interpret'))).toBe(true);
});
