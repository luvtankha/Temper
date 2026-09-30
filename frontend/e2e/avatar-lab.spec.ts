import { test, expect } from '@playwright/test';
test('authoring harness remains honest about missing rigs while targets are inspectable',async ({page})=> {
  await page.goto('/avatar-lab');
  await expect(page.getByRole('alert')).toContainText('Nova has no configured Rive rig');
  await page.getByLabel('Expression target').selectOption('angry');
  await expect(page.getByLabel('Semantic avatar controls')).toContainText('"anger": 0.8');
  await page.getByRole('button',{name:'Return to neutral'}).click();
  await expect(page.getByLabel('Semantic avatar controls')).toContainText('"anger": 0');
  await page.getByLabel('Viewing as').selectOption('nova');
  await expect(page.getByRole('alert')).toContainText('Alex has no configured Rive rig');
});
