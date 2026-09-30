import { useEffect, useState } from 'react';
import { Alignment, Fit, Layout, RuntimeLoader, StateMachineInputType, useRive } from '@rive-app/react-canvas';
import wasmUrl from '@rive-app/canvas/rive.wasm?url';
import fallbackWasmUrl from '@rive-app/canvas/rive_fallback.wasm?url';
import { applyRigControls, normalizeControls, validateRigInputs, type AvatarControls, type ControlName } from './controls';
import { rigStateMachine } from './rigConfig';

// Bundle the MIT runtime with this app instead of fetching executable WASM from a CDN.
RuntimeLoader.setWasmUrl(wasmUrl);
RuntimeLoader.setWasmFallbackUrl(fallbackWasmUrl);

interface Props { src:string; artboard:string; controls:AvatarControls; label:string; typing:boolean; paused:boolean; idle:boolean; reducedMotion:boolean; }
export default function RiveCharacter({src,artboard,controls,label,typing,paused,idle,reducedMotion}:Props) {
  const [failure,setFailure]=useState<string|null>(null);
  const [ready,setReady]=useState(false);
  const [inputValues,setInputValues]=useState<string>();
  const {rive,RiveComponent}=useRive({
    src,artboard,stateMachines:rigStateMachine,autoplay:true,
    layout:new Layout({fit:Fit.Contain,alignment:Alignment.BottomCenter}),
    onLoadError:()=>setFailure('Avatar rig could not load.'),
  });
  useEffect(()=> {
    if (!rive) return;
    let frame=0;
    try {
      const inputs=rive.stateMachineInputs(rigStateMachine);
      validateRigInputs(inputs,StateMachineInputType.Number,['attentive','motion']);
      const required=validateRigInputs(inputs,StateMachineInputType.Number);
      const start=normalizeControls(Object.fromEntries(required.map(i=>[i.name,Number(i.value)/100])));
      const target=normalizeControls(controls),began=performance.now(),duration=reducedMotion||paused?0:300;
      const tick=(time:number)=> {
        const progress=duration===0?1:Math.min(1,(time-began)/duration);
        const eased=progress*progress*(3-2*progress);
        const current=Object.fromEntries(required.map(i=>{const key=i.name as ControlName;return [key,start[key]+(target[key]-start[key])*eased];})) as AvatarControls;
        applyRigControls(inputs,current,StateMachineInputType.Number);
        if(progress<1)frame=requestAnimationFrame(tick);
        else if(import.meta.env.DEV)setInputValues(JSON.stringify(Object.fromEntries((inputs??[]).map(i=>[i.name,i.value]))));
      };
      frame=requestAnimationFrame(tick);
      setReady(true); setFailure(null);
    } catch (cause) {setFailure(cause instanceof Error ? cause.message : 'Avatar rig is incompatible.'); setReady(false); rive.pause();}
    return ()=>cancelAnimationFrame(frame);
  },[rive,controls,reducedMotion,paused]);
  useEffect(()=> {
    if(!rive||!ready||failure)return;
    const inputs=rive.stateMachineInputs(rigStateMachine);
    const [attention,motion]=validateRigInputs(inputs,StateMachineInputType.Number,['attentive','motion']);
    attention.value=typing&&!paused?100:0;motion.value=idle?100:0;
    if(import.meta.env.DEV)setInputValues(JSON.stringify(Object.fromEntries((inputs??[]).map(i=>[i.name,i.value]))));
    if(paused)rive.pause();else rive.play(rigStateMachine);
  },[rive,ready,failure,typing,paused,idle]);
  return <div className="rive-character" role="img" aria-label={label} data-rig-status={failure ? 'error' : ready ? 'ready' : 'loading'} data-rig-inputs={inputValues} data-avatar-paused={paused} data-avatar-typing={typing&&!paused} data-idle-motion={idle}>
    {failure ? <span className="rig-load-error" role="alert">{failure}</span> : <><RiveComponent aria-hidden="true" />{!ready && <span className="rig-loading-label">Loading avatar rig…</span>}</>}
  </div>;
}
