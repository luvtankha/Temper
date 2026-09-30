import { test, expect } from '@playwright/test';
const sizes = [[2560,1080],[1920,1080],[1440,900],[1024,768],[1180,820],[820,1180],[768,1024],[430,932],[320,568],[844,390]];
for (const [width,height] of sizes) {
  test(`responsive shell ${width}x${height}`, async ({page}) => {
    await page.setViewportSize({width,height});
    await page.goto('/chat');
    await expect(page.getByRole('heading', {name:/^Nova/})).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    const toggle = page.getByRole('button',{name:'Insights',exact:true});
    if (await toggle.getAttribute('aria-expanded') === 'true') await toggle.click();
    await expect(page.getByLabel('Conversation insights', {exact:true})).toHaveCount(0);
    await toggle.click();
    await expect(page.getByLabel('Conversation insights', {exact:true})).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.keyboard.press('Escape');
    await expect(toggle).toBeFocused();
    if (width < 1024) await page.getByRole('button',{name:'Open navigation'}).click();
    await page.getByRole('link',{name:'Full analysis',exact:true}).click();
    await expect(page).toHaveURL(/\/analysis$/);
    await expect(page.getByRole('heading',{name:'See the bigger picture'})).toBeVisible();
  });
}
test('all routes and fallback', async ({page}) => {
  for (const path of ['/history','/settings','/unknown']) {
    await page.goto(path);
    await expect(page.locator('h1')).toBeVisible();
  }
  await expect(page).toHaveURL(/\/chat$/);
});
