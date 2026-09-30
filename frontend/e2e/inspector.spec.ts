import {test,expect} from '@playwright/test';
for(const [width,height] of [[1440,900],[820,1180],[390,844],[320,568],[844,390]]) {
  test(`message scores and evidence are inspectable ${width}x${height}`,async({page})=> {
    await page.setViewportSize({width,height});await page.goto('/chat');
    const message=page.getByRole('button',{name:/Message 5 from Nova:/});await message.click();
    const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('heading',{name:'Message #5',exact:true})).toBeVisible();await expect(dialog.getByRole('progressbar')).toHaveCount(12);
    await expect(dialog.getByRole('progressbar',{name:'Frustration',exact:true})).toHaveAttribute('aria-valuenow','64');
    await expect(dialog.getByText('No AI inference',{exact:true})).toBeVisible();await expect(dialog.getByRole('heading',{name:'Why this matters',exact:true})).toBeVisible();
    await expect(dialog.getByText('5 prior turns',{exact:true})).toHaveCount(0);await expect(dialog.getByText('4 prior turns',{exact:true})).toBeVisible();
    expect(await page.locator('.app-shell').evaluate(e=>(e as HTMLElement).inert)).toBe(true);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    const close=dialog.getByRole('button',{name:'Close message details'});await close.focus();await page.keyboard.press('Shift+Tab');
    expect(await dialog.evaluate(e=>e.contains(document.activeElement))).toBe(true);
    await page.keyboard.press('Escape');await expect(dialog).toHaveCount(0);await expect(message).toBeFocused();await expect(page.getByRole('textbox')).toBeEnabled();
  });
}
test('every analyzed turn exposes details and context links return to the correct message',async({page})=> {
  await page.goto('/chat');await expect(page.locator('[data-message-count]')).toHaveAttribute('data-message-count','7');
  for(let sequence=1;sequence<=7;sequence++) {
    await page.getByRole('button',{name:new RegExp(`Message ${sequence} from`)}).click();
    const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog.getByRole('heading',{name:`Message #${sequence}`,exact:true})).toBeVisible();await expect(dialog.getByRole('progressbar')).toHaveCount(12);
    await dialog.getByRole('button',{name:'Close message details'}).click();
  }
  await page.getByRole('button',{name:/Message 7 from Nova:/}).click();
  const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog.getByText('5 prior turns',{exact:true})).toBeVisible();
  await dialog.getByRole('button',{name:/#4 · Alex/}).click();await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('button',{name:/Message 4 from Alex:/})).toBeFocused();
});
test('new turn shows pending analysis rather than fabricated zero scores, then updates',async({page})=> {
  await page.goto('/chat');await expect(page.locator('[data-message-count]')).toHaveAttribute('data-message-count','7');
  const testClock=new Date('2030-01-01T00:00:00Z');
  await page.clock.install({time:testClock});await page.clock.pauseAt(new Date(testClock.getTime()+60_000));
  await page.getByRole('textbox').fill('A fresh turn awaiting its fixture');await page.getByRole('button',{name:'Send message',exact:true}).click();
  await page.getByRole('button',{name:/Message 8 from Alex:/}).click();
  const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog.getByRole('status')).toContainText('Analysis pending');await expect(dialog.getByRole('progressbar')).toHaveCount(0);
  await page.clock.fastForward(250);await expect(dialog.getByRole('progressbar')).toHaveCount(12);await expect(dialog.getByText('No AI inference',{exact:true})).toBeVisible();
});
