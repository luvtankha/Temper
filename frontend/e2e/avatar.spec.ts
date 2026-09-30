import { test, expect } from '@playwright/test';
for (const [width,height] of [[2560,1080],[1920,1080],[1440,900],[1024,768],[1180,820],[820,1180],[768,1024],[430,932],[320,568],[844,390]]) {
  test(`remote avatar stays anchored ${width}x${height}`,async ({page})=> {
    await page.setViewportSize({width,height}); await page.goto('/chat');
    const nova=page.getByLabel("Nova's remote avatar",{exact:true});
    await expect(nova).toBeVisible(); await expect(nova).toHaveAttribute('data-variant','female');
    await expect(page.getByRole('textbox',{name:'Message Nova'})).toBeEnabled();
    const anchored=await page.evaluate(()=> {
      const wrapper=document.querySelector('.composer-wrapper')!; const layer=wrapper.querySelector('.remote-avatar-layer')!; const input=wrapper.querySelector('textarea')!;
      const w=wrapper.getBoundingClientRect(), a=layer.getBoundingClientRect(), c=input.getBoundingClientRect();
      return layer.parentElement===wrapper && a.top>=w.top && a.bottom<=c.top && c.bottom<=innerHeight && a.right<=innerWidth && a.left>=0;
    });
    expect(anchored).toBe(true);
    await page.getByRole('button',{name:'Conversation options'}).click(); await page.getByLabel('Viewing as').selectOption('nova'); await page.getByRole('button',{name:'Conversation options'}).click();
    await expect(page.getByLabel("Alex's remote avatar",{exact:true})).toHaveAttribute('data-variant','male');
    await expect(page.getByRole('textbox',{name:'Message Alex'})).toBeEnabled();
  });
}
