import {test,expect} from '@playwright/test';
test('dedicated emotion inference populates the existing responsive meters and mapping evidence',async({page,request})=>{
  test.skip(process.env.TEMPER_REAL_EMOTION_TEST!=='1','Requires the verified local emotion model in the running backend.');
  const room=await(await request.post('/api/v1/conversations',{data:{title:'Actual emotion UI',participantIds:['alex','nova']}})).json();
  const message=await(await request.post(`/api/v1/conversations/${room.id}/messages`,{data:{speakerId:'nova',text:'I am furious and angry with you!'}})).json();
  const result=await(await request.post(`/api/v1/messages/${message.id}/analyze`)).json();expect(result.mode).toBe('HYBRID');
  expect(Object.keys(result.emotions)).toEqual(expect.arrayContaining(['anger','frustration','sadness','happiness','confusion','concern','surprise','neutral']));
  for(const value of Object.values(result.emotions) as number[]) {expect(value).toBeGreaterThanOrEqual(0);expect(value).toBeLessThanOrEqual(1);}
  await page.setViewportSize({width:390,height:844});await page.goto(`/chat?transport=rest&conversation=${room.id}`);
  await expect(page.getByText('Backend chat · estimated + fixture analysis',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Message 1 from Nova: I am furious and angry with you!'}).click();
  const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog.getByText('GoEmotions emotion',{exact:true})).toBeVisible();
  await expect(dialog.getByText(/annoyance→frustration proxy/)).toBeVisible();
  await expect(dialog.getByRole('progressbar',{name:'Anger',exact:true})).toHaveAttribute('aria-valuenow',String(Math.round(result.emotions.anger*100)));
  await expect(dialog.getByRole('progressbar',{name:'Surprise',exact:true})).toBeVisible();await expect(dialog.getByRole('progressbar',{name:'Neutral language',exact:true})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await page.getByRole('button',{name:'Close message details'}).click();await page.getByRole('button',{name:'Open navigation',exact:true}).click();
  await page.getByRole('link',{name:'Full analysis',exact:true}).click();await expect(page.getByText('Mapped model signals',{exact:true})).toBeVisible();
});
