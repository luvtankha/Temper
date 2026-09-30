import { test, expect } from '@playwright/test';
import {createHash} from 'node:crypto';
import {PNG} from 'pngjs';

for (const local of ['alex','nova']) {
  test(`genuine Rive ${local==='alex'?'female':'male'} rig blends eight expressions and reverses`,async ({page})=> {
    test.setTimeout(45000);
    await page.goto('/avatar-lab');
    await page.getByLabel('Viewing as').selectOption(local);
    const rig=page.locator('[data-rig-status]');
    await expect(rig).toHaveAttribute('data-rig-status','ready');
    await expect(page.getByRole('alert')).toHaveCount(0);
    const canvas=rig.locator('canvas');
    const capture=async()=> {
      // Rive suspends rendering outside the viewport; resume before capture.
      await canvas.scrollIntoViewIfNeeded();await page.waitForTimeout(100);
      const bytes=await canvas.screenshot();const png=PNG.sync.read(bytes);
      let skinPixels=0;
      for(let i=0;i<png.data.length;i+=4)if(png.data[i]>120&&png.data[i]>png.data[i+1]*1.15&&png.data[i+1]>png.data[i+2]*1.1)skinPixels++;
      expect(skinPixels).toBeGreaterThan(200);
      return createHash('sha256').update(bytes).digest('hex');
    };
    const neutral=await capture();const poses=new Set([neutral]);
    for(const [expression,input] of [['happy','happiness'],['concerned','concern'],['confused','confusion'],['sad','sadness'],['frustrated','frustration'],['angry','anger'],['surprised','surprise']]) {
      await page.getByLabel('Expression target').selectOption(expression);
      await expect(rig).toHaveAttribute('data-rig-inputs',new RegExp(`"${input}":80`));
      await page.waitForTimeout(900);
      const full=await capture(); expect(full).not.toBe(neutral);poses.add(full);
      await page.getByLabel('Intensity:').fill('0.35');await page.waitForTimeout(250);
      expect(await capture()).not.toBe(full);
      await page.getByLabel('Intensity:').fill('0.8');
      await page.getByRole('button',{name:'Return to neutral'}).click();await page.waitForTimeout(900);
      expect(await capture()).toBe(neutral);
    }
    expect(poses.size).toBe(8);
    await page.getByLabel('Expression target').selectOption('angry');
    await page.waitForTimeout(80);const mid=await capture();
    await page.waitForTimeout(850);const angry=await capture();
    expect(mid).not.toBe(neutral);expect(mid).not.toBe(angry);
    await page.getByLabel('Expression target').selectOption('mixed');await page.waitForTimeout(900);
    expect(await capture()).toBe(angry);
    for(const expression of ['concerned','frustrated','angry','neutral']) {
      await page.getByLabel('Expression target').selectOption(expression);await page.waitForTimeout(900);
      const pose=await capture();if(expression==='neutral')expect(pose).toBe(neutral);else expect(pose).not.toBe(neutral);
    }
    await page.getByLabel('Expression target').selectOption('angry');await page.waitForTimeout(900);await capture();
    await canvas.screenshot({path:`test-results/${local==='alex'?'female':'male'}-rig-angry.png`});
  });
}
