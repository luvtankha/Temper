import {test,expect} from '@playwright/test';
test('backend adapter delivers real REST turns and populates existing inspector/dashboard',async({page,request})=>{
  const created=await request.post('/api/v1/conversations',{data:{title:'Browser REST fixture',participantIds:['alex','nova']}});expect(created.status()).toBe(201);const {id}=await created.json();
  await page.goto(`/chat?transport=backend&conversation=${id}`);await expect(page.getByText('Backend chat · mock analysis',{exact:true})).toBeVisible();
  const composer=page.getByRole('textbox',{name:'Message Nova'});await expect(composer).toBeEnabled();await composer.fill('A real backend delivery');await composer.press('Enter');
  await expect(page.getByRole('button',{name:'Message 1 from Alex: A real backend delivery'})).toBeVisible();
  await expect(page.locator('[data-message-count]')).toHaveAttribute('data-message-count','1');
  const stored=await(await request.get(`/api/v1/conversations/${id}/messages`)).json();expect(stored).toHaveLength(1);expect(stored[0].text).toBe('A real backend delivery');
  await page.reload();await expect(page.getByRole('button',{name:'Message 1 from Alex: A real backend delivery'})).toBeVisible();
  await page.getByRole('button',{name:'Message 1 from Alex: A real backend delivery'}).click();await expect(page.getByRole('dialog',{name:'Message details',exact:true})).toBeVisible();await expect(page.getByText(/Scores are assigned by message sequence only/)).toBeVisible();await page.getByRole('button',{name:'Close message details'}).click();
  await page.getByRole('link',{name:'Full analysis',exact:true}).click();await expect(page.getByRole('heading',{name:'Speaker comparison',exact:true})).toBeVisible();
});
test('backend read failure exposes retry without falling back to fake chat',async({page})=>{
  await page.goto('/chat?transport=backend&conversation=00000000-0000-4000-8000-ffffffffffff');
  await expect(page.getByRole('alert')).toContainText('404');await expect(page.getByRole('button',{name:'Retry conversation'})).toBeVisible();expect(await page.getByRole('button',{name:/Message \d+ from/}).count()).toBe(0);
});
