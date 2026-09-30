import {test,expect} from '@playwright/test';
const sizes=[[2560,1080],[1920,1080],[1440,900],[1024,768],[1180,820],[820,1180],[768,1024],[430,932],[320,568],[844,390]];
for(const [width,height] of sizes) {
  test(`timeline fits and selects a message ${width}x${height}`,async({page})=> {
    await page.setViewportSize({width,height});await page.goto('/analysis');
    const timeline=page.locator('.timeline-page').getByLabel('Emotional arc timeline',{exact:true});
    const point=timeline.getByRole('button',{name:'Focus message 5 · conflict',exact:true});await expect(point).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    const fits=await timeline.locator('.recharts-surface').evaluate(element=>{const rect=element.getBoundingClientRect();return rect.width>0&&rect.right<=innerWidth&&rect.left>=0;});expect(fits).toBe(true);
    await point.click();await expect(page).toHaveURL(/\/chat$/);
    const message=page.getByRole('button',{name:/Message 5 from Nova:/});await expect(message).toHaveAttribute('aria-pressed','true');await expect(message).toBeFocused();await expect(message).toBeInViewport();
  });
}
test('keyboard points and event markers focus the correct turn; series toggle works',async({page})=> {
  await page.goto('/analysis');const full=page.locator('.timeline-page');
  const anger=full.getByRole('button',{name:'Anger',exact:true});await anger.click();await expect(anger).toHaveAttribute('aria-pressed','false');await expect(full.getByRole('button',{name:'Focus message 5 · anger',exact:true})).toHaveCount(0);
  await anger.click();const point=full.getByRole('button',{name:'Focus message 4 · frustration',exact:true});await point.focus();await point.press('Enter');
  await expect(page.getByRole('button',{name:/Message 4 from Alex:/})).toBeFocused();
  await page.goto('/analysis');await page.locator('.timeline-page').getByRole('button',{name:'Recovery #7',exact:true}).click();
  await expect(page.getByRole('button',{name:/Message 7 from Nova:/})).toBeFocused();
});
