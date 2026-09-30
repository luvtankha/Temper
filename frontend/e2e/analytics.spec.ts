import {test,expect} from '@playwright/test';
for(const [width,height] of [[1440,900],[820,1180],[390,844],[320,568]]) {
  test(`mock insights are readable and collapsible ${width}x${height}`,async({page})=> {
    await page.setViewportSize({width,height});await page.goto('/chat');
    const toggle=page.getByRole('button',{name:'Insights',exact:true});if(await toggle.getAttribute('aria-expanded')!=='true')await toggle.click();
    const panel=page.getByLabel('Conversation insights',{exact:true});
    await expect(panel.locator('[data-analysis-mode]')).toHaveAttribute('data-analysis-mode','MOCK');
    await expect(panel.getByText('No AI inference',{exact:true})).toBeVisible();
    await expect(panel.getByRole('progressbar')).toHaveCount(12);
    await expect(panel.getByRole('progressbar',{name:'Frustration',exact:true})).toHaveAttribute('aria-valuenow','16');
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.getByRole('button',{name:'Close insights'}).click();await expect(panel).toHaveCount(0);
    const composer=page.getByRole('textbox',{name:'Message Nova'});await expect(composer).toBeEnabled();await composer.fill('A new mock analytics turn');await composer.press('Enter');
    await toggle.click();await expect(panel.locator('[data-message-count]')).toHaveAttribute('data-message-count','8');
    await page.keyboard.press('Escape');await expect(composer).toBeEnabled();
  });
}
test('empty conversation clears stale metrics and send restores a fixture',async({page})=> {
  await page.goto('/chat');await expect(page.locator('[data-message-count]')).toHaveAttribute('data-message-count','7');
  await page.getByRole('button',{name:'Conversation options'}).click();await page.getByRole('button',{name:'New demo conversation'}).click();
  await expect(page.getByText('A little context starts here.',{exact:true})).toBeVisible();await expect(page.locator('[data-message-count]')).toHaveCount(0);
  await page.getByRole('textbox').fill('Hello');await page.getByRole('button',{name:'Send message',exact:true}).click();
  await expect(page.locator('[data-message-count]')).toHaveAttribute('data-message-count','1');
});
