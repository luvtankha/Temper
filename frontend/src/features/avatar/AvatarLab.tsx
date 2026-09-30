import { useState } from 'react';
import { RemoteAvatar } from './RemoteAvatar';
import { emotionStates, expressionTarget, type EmotionState } from './controls';
import { rigConfig } from './rigConfig';
import { participants, remoteParticipant, type ParticipantId } from '../../models/chat';

/** Development-only authoring harness; never interprets a chat message or claims inference. */
export function AvatarLab() {
  const [localId,setLocalId]=useState<ParticipantId>('alex');
  const [state,setState]=useState<EmotionState>('neutral');
  const [intensity,setIntensity]=useState(.8);
  const remote=remoteParticipant(localId);
  const controls=expressionTarget(state,intensity);
  return <section className="avatar-lab"><p className="eyebrow">DEVELOPMENT · RIG VALIDATION</p><h1>Avatar authoring lab</h1><p>Manual engineering controls. These values are not conversation analysis.</p>
    <div className="lab-preview"><RemoteAvatar localId={localId} controls={controls} /></div>
    {!rigConfig[remote.variant].src && <p className="lab-warning" role="alert">{remote.name} has no configured Rive rig. The neutral preview cannot demonstrate emotional transitions.</p>}
    <label>Viewing as<select value={localId} onChange={e=>setLocalId(e.target.value as ParticipantId)}>{participants.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
    <label>Expression target<select value={state} onChange={e=>setState(e.target.value as EmotionState)}>{emotionStates.map(s=><option key={s} value={s}>{s}</option>)}</select></label>
    <label>Intensity: {intensity.toFixed(2)}<input type="range" min="0" max="1" step="0.01" value={intensity} onChange={e=>setIntensity(Number(e.target.value))} /></label>
    <button className="analytics-toggle" onClick={()=>setState('neutral')}>Return to neutral</button>
    <pre aria-label="Semantic avatar controls">{JSON.stringify(controls,null,2)}</pre>
  </section>;
}
