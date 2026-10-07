import type { ParticipantId } from '../../models/chat';
import { remoteParticipant } from '../../models/chat';
import { lazy, Suspense } from 'react';
import { neutralControls, type AvatarControls } from './controls';
import { rigConfig } from './rigConfig';
import {useReducedMotion} from './useReducedMotion';
const RiveCharacter=lazy(()=>import('./RiveCharacter'));

export function RemoteAvatar({localId,controls=neutralControls,typing=false,paused=false,idle=true}: {localId:ParticipantId;controls?:AvatarControls;typing?:boolean;paused?:boolean;idle?:boolean}) {
  const remote=remoteParticipant(localId);
  const reducedMotion=useReducedMotion();
  const rig=rigConfig[remote.variant];
  return <div className="remote-avatar-layer" data-participant-id={remote.id} data-variant={remote.variant} aria-label={`${remote.name}'s remote avatar`}>
    <div className="avatar-presence"><span className="avatar-presence-mark">✦</span><span><strong>{paused?`${remote.name}'s avatar is paused.`:typing?`${remote.name} is attentive.`:`${remote.name} is here.`}</strong><small>{typing&&!paused?'Waiting for the next thought':'Your conversation partner'}</small></span></div>
    <div className="avatar-art"><div className="avatar-halo" />{rig.src ? <Suspense fallback={<span className="rig-loading-label">Loading avatar rig…</span>}><RiveCharacter key={remote.id} src={rig.src} artboard={rig.artboard} controls={controls} typing={typing} paused={paused} idle={idle&&!reducedMotion} reducedMotion={reducedMotion} label={`${remote.name}, animated ${remote.variant} avatar`} /></Suspense> : <img src={`${import.meta.env.BASE_URL}avatars/${remote.variant}.svg`} alt={`${remote.name}, neutral ${remote.variant} avatar preview`} width="260" height="260" draggable="false" />}</div>
    <span className="avatar-preview-label">{rig.src ? 'Rive character' : 'Neutral preview · rig pending'}</span>
  </div>;
}
