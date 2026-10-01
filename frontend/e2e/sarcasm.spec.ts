import {test,expect} from '@playwright/test';
test('dedicated sarcasm model reaches inspector and its timeline with real provenance',async({page,request})=>{
  test.skip(process.env.TEMPER_REAL_SARCASM_TEST!=='1','Requires the verified local sarcasm model in the running backend.');
  const room=await(await request.post('/api/v1/conversations',{data:{title:'Actual sarcasm UI',participantIds:['alex','nova']}})).json();
  const message=await(await request.post(`/api/v1/conversations/${room.id}/messages`,{data:{speakerId:'nova',text:'Oh great, another traffic jam. Just what I needed!'}})).json();
  const result=await(await request.post(`/api/v1/messages/${message.id}/analyze`)).json();expect(result.mode).toBe('HYBRID');
  expect(result.evidence.some((e:{source:string;label:string})=>e.source==='MODEL'&&e.label==='Sarcasm classifier')).toBe(true);
  await page.setViewportSize({width:390,height:844});await page.goto(`/chat?transport=rest&conversation=${room.id}`);
  await expect(page.getByText('Backend chat · estimated + fixture analysis',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Message 1 from Nova: Oh great, another traffic jam. Just what I needed!'}).click();
  const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog.getByText('Sarcasm classifier',{exact:true})).toBeVisible();
  await expect(dialog.getByRole('progressbar',{name:'Sarcasm',exact:true})).toHaveAttribute('aria-valuenow',String(Math.round(result.signals.sarcasm*100)));
  await expect(dialog.getByText(/independently of sentiment/)).toBeVisible();
  await page.getByRole('button',{name:'Close message details'}).click();await page.getByRole('button',{name:'Open navigation',exact:true}).click();await page.getByRole('link',{name:'Full analysis',exact:true}).click();
  await expect(page.getByLabel('sarcasm timeline',{exact:true}).getByText('Model estimates',{exact:true})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
