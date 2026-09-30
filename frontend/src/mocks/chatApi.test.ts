import { describe, expect, it } from 'vitest';
import { createMockChatApi } from './chatApi';
describe('local chat adapter', () => {
  it('accepts both participants without altering existing messages',async () => {
    const api=createMockChatApi(); const before=await api.listMessages();
    const alex=await api.sendMessage('alex',' Hello '); const nova=await api.sendMessage('nova','Hi!');
    expect(alex.text).toBe('Hello'); expect(nova.speakerId).toBe('nova');
    expect((await api.listMessages()).slice(0,before.length)).toEqual(before);
  });
  it('does not expose mutable repository state',async () => {
    const api=createMockChatApi(); const result=await api.listMessages(); result[0].text='changed';
    expect((await api.listMessages())[0].text).not.toBe('changed');
  });
  it('rejects blank and oversized messages',async () => {
    const api=createMockChatApi(); await expect(api.sendMessage('alex',' ')).rejects.toThrow();
    await expect(api.sendMessage('nova','x'.repeat(2001))).rejects.toThrow();
  });
  it('can start empty or load a fresh fictional sample',async () => {
    const api=createMockChatApi(); expect(await api.reset(false)).toEqual([]);
    expect(await api.reset(true)).toHaveLength(7);
  });
});
