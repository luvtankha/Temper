import { afterEach, describe, expect, it, vi } from 'vitest';
import { getHealth } from './client';
afterEach(() => vi.unstubAllGlobals());
describe('health adapter', () => {
  it('returns the stable backend response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({status:'UP', application:'TEMPER', analysisMode:'NONE'}))));
    expect(await getHealth()).toEqual({status:'UP', application:'TEMPER', analysisMode:'NONE'});
    expect(fetch).toHaveBeenCalledWith('/api/v1/health', expect.objectContaining({signal: expect.any(AbortSignal)}));
  });
  it('surfaces HTTP failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', {status:503})));
    await expect(getHealth()).rejects.toThrow('503');
  });
});
