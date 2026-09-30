import type { ParticipantId } from '../../models/chat';
import { remoteParticipant } from '../../models/chat';
import { lazy, Suspense } from 'react';
import { neutralControls, type AvatarControls } from './controls';
import { rigConfig } from './rigConfig';
const RiveCharacter=lazy(()=>import('./RiveCharacter'));

export function RemoteAvatar({localId,controls=neutralControls}: {localId:ParticipantId;controls?:AvatarControls}) {
  const remote=remoteParticipant(localId);
  const rig=rigConfig[remote.variant];
  return <div className="remote-avatar-layer" data-participant-id={remote.id} data-variant={remote.variant} aria-label={`${remote.name}'s remote avatar`}>
    <div className="avatar-presence"><span className="avatar-presence-mark">✦</span><span><strong>{remote.name} is here.</strong><small>Your conversation partner</small></span></div>
    <div className="avatar-art"><div className="avatar-halo" />{rig.src ? <Suspense fallback={<span className="rig-loading-label">Loading avatar rig…</span>}><RiveCharacter key={remote.id} src={rig.src} artboard={rig.artboard} controls={controls} label={`${remote.name}, animated ${remote.variant} avatar`} /></Suspense> : <img src={`/avatars/${remote.variant}.svg`} alt={`${remote.name}, neutral ${remote.variant} avatar preview`} width="260" height="260" draggable="false" />}</div>
    <span className="avatar-preview-label">{rig.src ? 'Rive character' : 'Neutral preview · rig pending'}</span>
  </div>;
}
