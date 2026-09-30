import {test,expect} from '@playwright/test';
import {createHash} from 'node:crypto';

for(const participant of ['alex','nova']) {
  test(`Rive presence breathes, attends and intentionally pauses viewing as ${participant}`,async ({page})=> {
    await page.goto('/avatar-lab');await page.getByLabel('Viewing as').selectOption(participant);
    const rig=page.locator('[data-rig-status]');await expect(rig).toHaveAttribute('data-rig-status','ready');
    const canvas=rig.locator('canvas');
    const capture=async()=>{await canvas.scrollIntoViewIfNeeded();await page.waitForTimeout(100);return createHash('sha256').update(await canvas.screenshot()).digest('hex');};
    const idle=await capture();await page.waitForTimeout(600);expect(await capture()).not.toBe(idle);
    await page.getByLabel('Pause avatar',{exact:true}).check();await canvas.scrollIntoViewIfNeeded();await page.waitForTimeout(200);
    const paused=await capture();await page.waitForTimeout(500);expect(await capture()).toBe(paused);
    await expect(rig).toHaveAttribute('data-avatar-paused','true');
    await page.getByLabel('Pause avatar',{exact:true}).uncheck();await page.waitForTimeout(400);expect(await capture()).not.toBe(paused);
    await page.getByLabel('Idle motion',{exact:true}).uncheck();await page.waitForTimeout(500);const resting=await capture();
    await page.getByLabel('Preview remote typing',{exact:true}).check();await page.waitForTimeout(700);const attentive=await capture();
    expect(attentive).not.toBe(resting);await expect(rig).toHaveAttribute('data-rig-inputs',/"attentive":100/);
    await page.getByLabel('Preview remote typing',{exact:true}).uncheck();await page.waitForTimeout(700);expect(await capture()).toBe(resting);
  });
}
test('reduced motion disables idle while semantic expression and typing remain usable',async({page})=> {
  await page.emulateMedia({reducedMotion:'reduce'});await page.goto('/avatar-lab');
  const rig=page.locator('[data-rig-status]');await expect(rig).toHaveAttribute('data-rig-status','ready');
  await expect(rig).toHaveAttribute('data-idle-motion','false');await expect(rig).toHaveAttribute('data-rig-inputs',/"motion":0/);
  await page.getByLabel('Expression target').selectOption('surprised');await expect(rig).toHaveAttribute('data-rig-inputs',/"surprise":80/);
});
test('chat typing and pause reach the other participant rig',async({page})=> {
  await page.goto('/chat');const rig=page.locator('[data-rig-status]');await expect(rig).toHaveAttribute('data-rig-status','ready');
  const menu=page.getByRole('button',{name:'Conversation options'});
  await menu.click();await page.getByRole('button',{name:'Preview remote typing',exact:true}).click();
  await expect(rig).toHaveAttribute('data-avatar-typing','true');await expect(page.getByText('Nova is attentive.',{exact:true})).toBeVisible();
  await menu.click();await page.getByRole('button',{name:'Pause local demo'}).click();await expect(rig).toHaveAttribute('data-avatar-paused','true');
});
