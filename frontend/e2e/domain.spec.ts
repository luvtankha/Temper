import {test,expect} from '@playwright/test';
test('real Java domain data reaches the frontend proxy without fabricated backend analysis',async({request})=> {
  const list=await request.get('/api/v1/conversations');expect(list.ok()).toBe(true);const conversations=await list.json();
  expect(conversations).toHaveLength(1);expect(conversations[0]).toMatchObject({title:'The launch plan',messageCount:7,analysisStatus:'NONE',participants:[{id:'alex',name:'Alex',variant:'male'},{id:'nova',name:'Nova',variant:'female'}]});
  const conversation=await request.get(`/api/v1/conversations/${conversations[0].id}`);expect(conversation.ok()).toBe(true);expect(await conversation.json()).toEqual(conversations[0]);
});
test('malformed/missing domain IDs preserve stable health availability',async({request})=> {
  expect((await request.get('/api/v1/conversations/not-a-uuid')).status()).toBe(400);
  expect((await request.get('/api/v1/conversations/ffffffff-ffff-4fff-8fff-ffffffffffff')).status()).toBe(404);
  const health=await request.get('/api/v1/health');expect(await health.json()).toMatchObject({status:'UP',analysisMode:'NONE'});
});
