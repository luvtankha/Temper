import {afterEach,describe,it,expect,vi} from 'vitest';
import {createBackendAdapters,demoConversationId} from './backendAdapters';
const socketHarness=vi.hoisted(()=>({sockets:[] as {connected:boolean;onConnect?:()=>void;onWebSocketClose?:()=>void;subscriptions:Map<string,(frame:{body:string})=>void>}[]}));
vi.mock('@stomp/stompjs',()=>({Client:class {
  connected=true;subscriptions=new Map<string,(frame:{body:string})=>void>();onConnect?:()=>void;onWebSocketClose?:()=>void;
  constructor(){socketHarness.sockets.push(this);}activate(){}deactivate(){return Promise.resolve();}publish(){}subscribe(topic:string,handler:(frame:{body:string})=>void){this.subscriptions.set(topic,handler);}
}}));
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
  it('late close from an old identity does not cancel delivery on the new socket',async()=>{
    vi.stubGlobal('fetch',vi.fn(async()=>new Response('[]',{status:200})));const api=createBackendAdapters(demoConversationId,true);
    const events={message:vi.fn(),snapshot:vi.fn(),typing:vi.fn(),presence:vi.fn(),connection:vi.fn(),error:vi.fn()};
    const closeOld=api.chat.connect!('alex',events);const old=socketHarness.sockets.at(-1)!;old.onConnect!();closeOld();
    const closeNew=api.chat.connect!('nova',events);const current=socketHarness.sockets.at(-1)!;current.onConnect!();
    const message={id:'delivered',speakerId:'nova',text:'New identity send',sentAt:'2026-10-01T00:00:00Z'};
    // Capture generated request ID through the publish seam.
    let requestId='';(current as unknown as {publish:(value:{body:string})=>void}).publish=value=>{requestId=JSON.parse(value.body).requestId;};
    const pending=api.chat.sendMessage('nova',message.text);old.onWebSocketClose!();current.subscriptions.get(`/topic/conversations/${demoConversationId}/messages`)!({body:JSON.stringify({message,requestId})});
    await expect(pending).resolves.toEqual(message);closeNew();
  });
});
