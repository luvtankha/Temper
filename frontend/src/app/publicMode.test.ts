import {describe, expect, it} from 'vitest';
import {backendTransport} from './publicMode';

describe('public demo network isolation',()=>{
  it('ignores URL and build-configured backend transports in a public build',()=>{
    expect(backendTransport('?transport=backend&conversation=private','rest',true)).toBeNull();
    expect(backendTransport('?transport=rest','backend',true)).toBeNull();
    expect(backendTransport('','backend',true)).toBeNull();
  });
  it('preserves explicitly configured development transports',()=>{
    expect(backendTransport('?transport=rest&conversation=demo','backend',false)).toEqual({conversation:'demo',realtime:false});
    expect(backendTransport('','backend',false)).toEqual({conversation:undefined,realtime:true});
    expect(backendTransport('','local',false)).toBeNull();
  });
});
