import {test,expect} from '@playwright/test';
test('estimated language indicators stay separate and inspectable on a phone',async({page,request})=>{
  const room=await(await request.post('/api/v1/conversations',{data:{title:'Estimated indicator UI',participantIds:['alex','nova']}})).json();
  const first=await(await request.post(`/api/v1/conversations/${room.id}/messages`,{data:{speakerId:'nova',text:'I disagree.'}})).json();
  await request.post(`/api/v1/messages/${first.id}/analyze`);
  const text="Your fault. I disagree. Leave me alone. Not my fault. Whatever you say.";
  const message=await(await request.post(`/api/v1/conversations/${room.id}/messages`,{data:{speakerId:'nova',text}})).json();
  const result=await(await request.post(`/api/v1/messages/${message.id}/analyze`)).json();
  for(const key of ['passiveAggression','blame','defensiveness','disagreement','withdrawal','repeatedDisagreement'])expect(result.signals[key]).toBeGreaterThan(0);
  await page.setViewportSize({width:320,height:568});await page.goto(`/chat?transport=rest&conversation=${room.id}`);
  await page.getByRole('button',{name:`Message 2 from Nova: ${text}`}).click();
  const dialog=page.getByRole('dialog',{name:'Message details',exact:true});
  for(const name of ['Passive aggression','Blame','Defensiveness','Estimated disagreement','Estimated withdrawal','Estimated repeated disagreement'])await expect(dialog.getByRole('progressbar',{name,exact:true})).toBeAttached();
  await expect(dialog.getByText('Estimated repeatedDisagreement',{exact:true})).toBeAttached();
  await expect(dialog.getByText(/Earlier matching sequences: \[1\]/)).toBeAttached();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
