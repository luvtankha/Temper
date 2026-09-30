import {test,expect} from '@playwright/test';
for(const [width,height] of [[1440,900],[820,1180],[390,844],[320,568]]) {
  test(`full analysis sections fit and message inspector works ${width}x${height}`,async({page})=> {
    await page.setViewportSize({width,height});await page.goto('/analysis');const main=page.locator('.analysis-dashboard');
    await expect(main.getByLabel('Speaker comparison',{exact:true})).toBeVisible();await expect(main.getByLabel('Alex analysis',{exact:true}).getByText('3 turns',{exact:true})).toBeVisible();await expect(main.getByLabel('Nova analysis',{exact:true}).getByText('4 turns',{exact:true})).toBeVisible();
    await expect(main.getByLabel('Emotion signal distribution',{exact:true})).toBeVisible();await expect(main.getByLabel('sarcasm timeline',{exact:true})).toBeVisible();await expect(main.getByLabel('toxicity timeline',{exact:true})).toBeVisible();
    await expect(main.getByLabel('Conversation summary',{exact:true})).toBeVisible();await expect(main.getByLabel('Key insights',{exact:true})).toBeVisible();
    const rows=main.getByLabel('Message inspector list',{exact:true});await expect(rows.getByRole('button')).toHaveCount(7);
    expect(await main.evaluate(e=>e.scrollWidth<=e.clientWidth)).toBe(true);
    const message=rows.getByRole('button',{name:'Inspect message 5 from Nova',exact:true});await message.click();
    const dialog=page.getByRole('dialog',{name:'Message details',exact:true});await expect(dialog.getByRole('heading',{name:'Message #5',exact:true})).toBeVisible();await dialog.getByRole('button',{name:'Close message details'}).click();await expect(message).toBeFocused();
  });
}
test('dashboard clears after reset and repopulates without stale speaker data',async({page})=> {
  await page.goto('/chat');await page.getByRole('button',{name:'Conversation options'}).click();await page.getByRole('button',{name:'New demo conversation'}).click();await page.getByRole('link',{name:'Full analysis',exact:true}).click();
  await expect(page.locator('.analysis-dashboard').getByText('No conversation turns yet.',{exact:true})).toBeVisible();await expect(page.locator('.speaker-card')).toHaveCount(0);
  await page.locator('.analysis-dashboard').getByRole('link',{name:'Back to conversation'}).click();await page.getByRole('textbox').fill('A fresh conversation');await page.getByRole('button',{name:'Send message',exact:true}).click();await page.getByRole('link',{name:'Full analysis',exact:true}).click();
  await expect(page.locator('.analysis-dashboard').getByLabel('Alex analysis',{exact:true}).getByText('1 turn',{exact:true})).toBeVisible();await expect(page.locator('.analysis-dashboard').getByLabel('Nova analysis',{exact:true}).getByText('No data',{exact:true})).toBeVisible();
});
