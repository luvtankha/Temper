import type { ParticipantId } from '../../models/chat';
import { remoteParticipant } from '../../models/chat';

export function RemoteAvatar({localId}: {localId:ParticipantId}) {
  const remote=remoteParticipant(localId);
  return <div className="remote-avatar-layer" data-participant-id={remote.id} data-variant={remote.variant} aria-label={`${remote.name}'s remote avatar`}>
    <div className="avatar-presence"><span className="avatar-presence-mark">✦</span><span><strong>{remote.name} is here.</strong><small>Your conversation partner</small></span></div>
    <div className="avatar-art"><div className="avatar-halo" /><img src={`/avatars/${remote.variant}.svg`} alt={`${remote.name}, neutral ${remote.variant} avatar preview`} width="260" height="260" draggable="false" /></div>
    <span className="avatar-preview-label">Neutral preview</span>
  </div>;
}
