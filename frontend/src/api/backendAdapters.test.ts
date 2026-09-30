import {afterEach,describe,it,expect,vi} from 'vitest';
import {createBackendAdapters,demoConversationId} from './backendAdapters';
afterEach(()=>vi.unstubAllGlobals());
describe('backend adapter boundary',()=>{
  it('switches conversations after reset and keeps delivery independent of analysis',async()=>{
    const calls:string[]=[];vi.stubGlobal('fetch',vi.fn(async(url:string,init?:RequestInit)=>{calls.push(`${init?.method??'GET'} ${url}`);return new Response(JSON.stringify(url.endsWith('/conversations')?{id:'new-room'}:url.endsWith('/messages')&&init?.method==='POST'?{id:'turn-1',speakerId:'alex',text:'Hello',sentAt:'2026-10-01T00:00:00Z'}:[]),{status:200});}));
    const api=createBackendAdapters();await api.chat.listMessages();await api.chat.reset(false);await api.chat.sendMessage('alex','Hello');
    expect(calls).toEqual([`GET /api/v1/conversations/${demoConversationId}/messages`,'POST /api/v1/conversations','POST /api/v1/conversations/new-room/messages']);expect(api.session.id).toBe('new-room');
  });
  it('propagates backend failures and aborted analysis without manufacturing fixtures',async()=>{
    const fetch=vi.fn(async()=>new Response('{}',{status:404}));vi.stubGlobal('fetch',fetch);const api=createBackendAdapters();
    await expect(api.chat.listMessages()).rejects.toThrow('404');const controller=new AbortController();controller.abort();
    await expect(api.analysis.getConversation([{id:'a',speakerId:'alex',text:'x',sentAt:'2026-10-01T00:00:00Z'}],controller.signal)).rejects.toMatchObject({name:'AbortError'});expect(fetch).toHaveBeenCalledTimes(1);
  });
});
