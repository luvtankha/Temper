import { test, expect } from '@playwright/test';
test('frontend reaches the running Java backend through the proxy', async ({page}) => {
  await page.goto('/');
  await expect(page.getByRole('status')).toHaveText('Backend UP');
});
