import {act,render,screen,waitFor,cleanup} from '@testing-library/react';
import {afterEach,it,expect,vi} from 'vitest';
import {ChatProvider,useChat} from './ChatProvider';
import type {ChatApi,ChatStream,ChatMessage} from '../../models/chat';
afterEach(cleanup);
function Probe(){const chat=useChat();return <><p data-testid="turns">{chat.messages.map(m=>m.text).join('|')}</p><p data-testid="connection">{chat.connection}</p><button onClick={()=>void chat.send('Two')}>Send</button></>;}
it('merges late initial/reconnect snapshots with live turns without duplicates or ordering loss',async()=>{
  let events:ChatStream|undefined,resolveList:(messages:ChatMessage[])=>void=()=>{};
  const one:ChatMessage={id:'1',speakerId:'alex',text:'One',sentAt:'2026-10-01T00:00:00Z',sequence:1},two:ChatMessage={...one,id:'2',text:'Two',sequence:2};
  const dispose=vi.fn();const api:ChatApi={listMessages:()=>new Promise(resolve=>{resolveList=resolve;}),sendMessage:async()=>two,reset:async()=>[],connect:(_speaker,handlers)=>{events=handlers;return dispose;}};
  const view=render(<ChatProvider api={api} transport="backend"><Probe/></ChatProvider>);
  await waitFor(()=>expect(events).toBeDefined());
  act(()=>{events!.connection('connected');events!.message(two);events!.snapshot([one,two]);});
  await waitFor(()=>expect(screen.getByTestId('turns')).toHaveTextContent('One|Two'));
  await new Promise(resolve=>setTimeout(resolve,180));await act(async()=>resolveList([one]));
  expect(screen.getByTestId('turns')).toHaveTextContent('One|Two');screen.getByRole('button',{name:'Send'}).click();await waitFor(()=>expect(screen.getByTestId('turns').textContent).toBe('One|Two'));
  view.unmount();expect(dispose).toHaveBeenCalledTimes(1);
});
