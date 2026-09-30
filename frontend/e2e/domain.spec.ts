import {test,expect} from '@playwright/test';
test('real Java domain data reaches the frontend proxy without fabricated backend analysis',async({request})=> {
  const list=await request.get('/api/v1/conversations');expect(list.ok()).toBe(true);const conversations=await list.json();
  const seed=conversations.find((c:{id:string})=>c.id==='00000000-0000-4000-8000-000000000001');expect(seed).toMatchObject({title:'The launch plan',messageCount:7,analysisStatus:'NONE',participants:[{id:'alex',name:'Alex',variant:'male'},{id:'nova',name:'Nova',variant:'female'}]});
  const conversation=await request.get(`/api/v1/conversations/${seed.id}`);expect(conversation.ok()).toBe(true);expect(await conversation.json()).toEqual(seed);
});
test('malformed/missing domain IDs preserve stable health availability',async({request})=> {
  expect((await request.get('/api/v1/conversations/not-a-uuid')).status()).toBe(400);
  expect((await request.get('/api/v1/conversations/ffffffff-ffff-4fff-8fff-ffffffffffff')).status()).toBe(404);
  const health=await request.get('/api/v1/health');expect(await health.json()).toMatchObject({status:'UP',analysisMode:'NONE'});
});
