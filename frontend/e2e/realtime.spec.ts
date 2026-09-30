import {test,expect} from '@playwright/test';
test('independent browser sessions exchange STOMP messages, typing/presence and reconnect catch-up',async({browser,request})=>{
  const response=await request.post('/api/v1/conversations',{data:{title:'Two-person socket fixture',participantIds:['alex','nova']}});expect(response.status()).toBe(201);const {id}=await response.json();
  const aContext=await browser.newContext(),bContext=await browser.newContext();
  try {
    const a=await aContext.newPage(),b=await bContext.newPage();
    await b.addInitScript(()=>{const Native=window.WebSocket;const sockets:WebSocket[]=[];(window as unknown as {temperSockets:WebSocket[]}).temperSockets=sockets;window.WebSocket=class extends Native {constructor(url:string|URL,protocols?:string|string[]){super(url,protocols);if(new URL(url.toString(),location.href).pathname==='/ws')sockets.push(this);}};});
    const url=`http://127.0.0.1:5173/chat?transport=backend&conversation=${id}`;await Promise.all([a.goto(url),b.goto(url)]);
    await b.getByRole('button',{name:'Conversation options'}).click();await b.getByLabel('Viewing as').selectOption('nova');await b.getByRole('button',{name:'Conversation options'}).click();
    for(const page of [a,b])await expect(page.locator('[data-connection-state]')).toHaveAttribute('data-connection-state','connected');
    await expect(a.getByText('Online now',{exact:true})).toBeVisible();await expect(b.getByText('Online now',{exact:true})).toBeVisible();
    await expect(a.locator('.remote-avatar-layer')).toHaveAttribute('data-participant-id','nova');await expect(b.locator('.remote-avatar-layer')).toHaveAttribute('data-participant-id','alex');
    await a.getByRole('textbox',{name:'Message Nova'}).fill('Hello through a real websocket');
    await expect(b.getByText('Alex is typing',{exact:true})).toBeVisible();await expect(b.locator('[data-rig-status]')).toHaveAttribute('data-avatar-typing','true');
    await a.getByRole('textbox',{name:'Message Nova'}).press('Enter');for(const page of [a,b]){await expect(page.getByRole('button',{name:'Message 1 from Alex: Hello through a real websocket'})).toHaveCount(1);}
    await expect(b.locator('[data-rig-status]')).toHaveAttribute('data-avatar-typing','false');
    await b.getByRole('textbox',{name:'Message Alex'}).fill('And a reply from Nova');await b.getByRole('textbox',{name:'Message Alex'}).press('Enter');for(const page of [a,b])await expect(page.getByRole('button',{name:'Message 2 from Nova: And a reply from Nova'})).toHaveCount(1);
    await a.setViewportSize({width:390,height:844});await b.setViewportSize({width:320,height:568});for(const page of [a,b]){expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);const box=await page.getByRole('textbox').boundingBox();expect(box).not.toBeNull();expect(box!.y+box!.height).toBeLessThanOrEqual(page.viewportSize()!.height);}
    await bContext.setOffline(true);await b.evaluate(()=>{for(const socket of (window as unknown as {temperSockets:WebSocket[]}).temperSockets)socket.close();});
    await expect(b.locator('[data-connection-state]')).not.toHaveAttribute('data-connection-state','connected');await expect(a.getByText('Not connected',{exact:true})).toBeVisible();
    await a.getByRole('textbox').fill('Delivered while the partner reconnects');await a.getByRole('textbox').press('Enter');await expect(a.getByRole('button',{name:'Message 3 from Alex: Delivered while the partner reconnects'})).toHaveCount(1);
    await bContext.setOffline(false);await expect(b.locator('[data-connection-state]')).toHaveAttribute('data-connection-state','connected',{timeout:10000});await expect(b.getByRole('button',{name:'Message 3 from Alex: Delivered while the partner reconnects'})).toHaveCount(1);await expect(a.getByText('Online now',{exact:true})).toBeVisible();
    const stored=await(await request.get(`/api/v1/conversations/${id}/messages`)).json();expect(stored.map((m:{sequence:number})=>m.sequence)).toEqual([1,2,3]);
  }finally{await aContext.close();await bContext.close();}
});
