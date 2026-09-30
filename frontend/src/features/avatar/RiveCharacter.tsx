import { useEffect, useState } from 'react';
import { Alignment, Fit, Layout, RuntimeLoader, StateMachineInputType, useRive } from '@rive-app/react-canvas';
import wasmUrl from '@rive-app/canvas/rive.wasm?url';
import fallbackWasmUrl from '@rive-app/canvas/rive_fallback.wasm?url';
import { applyRigControls, type AvatarControls } from './controls';
import { rigStateMachine } from './rigConfig';

// Bundle the MIT runtime with this app instead of fetching executable WASM from a CDN.
RuntimeLoader.setWasmUrl(wasmUrl);
RuntimeLoader.setWasmFallbackUrl(fallbackWasmUrl);

interface Props { src:string; artboard:string; controls:AvatarControls; label:string; }
export default function RiveCharacter({src,artboard,controls,label}:Props) {
  const [failure,setFailure]=useState<string|null>(null);
  const [ready,setReady]=useState(false);
  const {rive,RiveComponent}=useRive({
    src,artboard,stateMachines:rigStateMachine,autoplay:true,
    layout:new Layout({fit:Fit.Contain,alignment:Alignment.BottomCenter}),
    onLoadError:()=>setFailure('Avatar rig could not load.'),
  });
  useEffect(()=> {
    if (!rive) return;
    try {
      applyRigControls(rive.stateMachineInputs(rigStateMachine),controls,StateMachineInputType.Number);
      setReady(true); setFailure(null);
    } catch (cause) {setFailure(cause instanceof Error ? cause.message : 'Avatar rig is incompatible.'); setReady(false); rive.pause();}
  },[rive,controls]);
  return <div className="rive-character" role="img" aria-label={label} data-rig-status={failure ? 'error' : ready ? 'ready' : 'loading'}>
    {failure ? <span className="rig-load-error" role="alert">{failure}</span> : <><RiveComponent aria-hidden="true" />{!ready && <span className="rig-loading-label">Loading avatar rig…</span>}</>}
  </div>;
}
