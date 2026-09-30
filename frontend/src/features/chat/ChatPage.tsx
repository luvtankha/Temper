import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { ArrowDown, ArrowUpRight, CheckCheck, ChevronDown, MessageCircle, MoreHorizontal, Send, Smile, Sparkles, X } from 'lucide-react';
import { participants, remoteParticipant } from '../../models/chat';
import { useChat } from './ChatProvider';
import { RemoteAvatar } from '../avatar/RemoteAvatar';

export function ChatPage() {
  const {messages,localId,setLocalId,loading,paused,setPaused,remoteTyping,send,reset,previewTyping} = useChat();
  const remote = remoteParticipant(localId);
  const [draft,setDraft] = useState('');
  const [sending,setSending] = useState(false);
  const [error,setError] = useState('');
  const [selectedId,setSelectedId] = useState<string|null>(null);
  const [optionsOpen,setOptionsOpen] = useState(false);
  const [emojiOpen,setEmojiOpen] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const endRef = useRef<HTMLDivElement>(null);
  const composerRef = useRef<HTMLTextAreaElement>(null);
  const [awayFromBottom,setAwayFromBottom] = useState(false);
  const selected = messages.find(m => m.id === selectedId);
  useEffect(() => {endRef.current?.scrollIntoView({behavior:'instant',block:'end'});},[loading]);
  useEffect(() => {if (!awayFromBottom) endRef.current?.scrollIntoView({behavior:'smooth',block:'end'});},[messages,remoteTyping,awayFromBottom]);
  useEffect(() => {setDraft(''); setError(''); setSelectedId(null);},[localId]);
  async function submit(event?:FormEvent) {
    event?.preventDefault(); if (!draft.trim() || sending || paused || loading) return;
    setSending(true); setError('');
    try {await send(draft); setDraft(''); setEmojiOpen(false); setAwayFromBottom(false); composerRef.current?.focus();}
    catch (cause) {setError(cause instanceof Error ? cause.message : 'Could not send message.');}
    finally {setSending(false);}
  }
  function handleKey(event:KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {event.preventDefault(); void submit();}
  }
  return <section className="chat-page" aria-label="Conversation">
    <header className="chat-header"><div className="participant-heading"><span className={`contact-avatar ${remote.variant}`}>{remote.name.slice(0,1)}<i /></span><div><h1>{remote.name}<span>your conversation partner</span></h1><p><span className={paused ? 'paused-dot' : 'online-dot'} />{paused ? 'Demo paused' : remoteTyping ? 'Typing in demo…' : 'Available in local demo'}</p></div></div><div className="chat-header-right"><span className="private-label"><Sparkles size={13} /> More than words</span><button className="icon-button" aria-label="Conversation options" aria-expanded={optionsOpen} onClick={() => setOptionsOpen(!optionsOpen)}><MoreHorizontal size={20} /></button></div>
      {optionsOpen && <div className="conversation-menu"><label>Viewing as<select aria-label="Viewing as" value={localId} onChange={e => setLocalId(e.target.value as typeof localId)}>{participants.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><button onClick={() => {setPaused(!paused);setOptionsOpen(false);}}>{paused ? 'Resume local demo' : 'Pause local demo'}</button><button disabled={paused} onClick={() => {previewTyping();setOptionsOpen(false);}}>Preview remote typing</button><button onClick={() => {void reset(false);setSelectedId(null);setOptionsOpen(false);}}>New demo conversation</button><button onClick={() => {void reset(true);setSelectedId(null);setOptionsOpen(false);}}>Load sample conversation</button></div>}
    </header>
    <div className="conversation-banner"><span className="banner-icon"><ActivityMark /></span><p>A little context. A better connection.</p><span>Local demo</span></div>
    <div ref={listRef} className="message-list" aria-label="Messages" aria-live="polite" aria-busy={loading} onScroll={() => {const el=listRef.current; if(el) setAwayFromBottom(el.scrollHeight-el.scrollTop-el.clientHeight>90);}}>
      {loading ? <div className="chat-empty"><span className="loading-spinner" /><p>Opening your conversation…</p></div> : messages.length === 0 ? <div className="chat-empty"><MessageCircle size={32} strokeWidth={1.2} /><h2>Every connection starts somewhere.</h2><p>Say hello to {remote.name}.</p></div> : <><div className="date-divider"><span />DEMO CONVERSATION<span /></div><div className="conversation-intro"><span className="intro-spark">✦</span><p>Room for a little more understanding.</p><small>A fictional conversation. A familiar kind of moment.</small></div>{messages.map((message,index) => {
        const own = message.speakerId === localId;
        const speaker = participants.find(p => p.id === message.speakerId)!;
        const showName = index===0 || messages[index-1].speakerId !== message.speakerId;
        return <div className={`message-row ${own ? 'own' : 'remote'}`} key={message.id}>
          {!own && <span className={`message-avatar ${showName ? '' : 'hidden-avatar'}`}>{speaker.name[0]}</span>}
          <div className="message-content">{showName && <span className="message-sender">{own ? 'You' : speaker.name}</span>}<button className={`message-bubble ${selectedId===message.id ? 'message-selected' : ''}`} aria-label={`Message ${index+1} from ${speaker.name}: ${message.text}`} aria-pressed={selectedId===message.id} onClick={() => setSelectedId(selectedId===message.id ? null : message.id)}>{message.text}</button><div className="message-meta"><time dateTime={message.sentAt}>{new Intl.DateTimeFormat('en-IN',{hour:'numeric',minute:'2-digit',timeZone:'Asia/Calcutta'}).format(new Date(message.sentAt))}</time>{own && <CheckCheck size={12} />}<span>demo</span></div></div>
        </div>;
      })}</>}
      {remoteTyping && <div className="typing-indicator"><span className="message-avatar">{remote.name[0]}</span><span className="typing-dots"><i /><i /><i /></span><small>{remote.name} is typing · simulated</small></div>}
      <div ref={endRef} />
    </div>
    {awayFromBottom && <button className="jump-bottom" aria-label="Scroll to latest message" onClick={() => {setAwayFromBottom(false); endRef.current?.scrollIntoView({behavior:'smooth'});}}><ArrowDown size={15} /> Latest messages</button>}
    {selected && <div className="message-selection" role="region" aria-label="Selected message"><div><strong>Message {messages.findIndex(m=>m.id===selected.id)+1}</strong><span>{participants.find(p=>p.id===selected.speakerId)?.name} · Analysis is not available yet.</span></div><button className="icon-button" aria-label="Close selected message" onClick={() => setSelectedId(null)}><X size={15} /></button></div>}
    <div className="composer-wrapper">
      <RemoteAvatar localId={localId} />
      <div className="composer-topline"><span><span className="small-spark">✦</span> A space to say what you mean.</span><span>CHATTING AS {participants.find(p=>p.id===localId)?.name.toUpperCase()}</span></div>
      <form className="composer" onSubmit={submit}>
        <textarea ref={composerRef} aria-label={`Message ${remote.name}`} placeholder={`Message ${remote.name}…`} value={draft} onChange={e=>setDraft(e.target.value)} onKeyDown={handleKey} maxLength={2000} rows={2} disabled={paused || loading} />
        <div className="composer-tools"><div className="composer-left-tools"><button type="button" className="icon-button" aria-label="Add emoji" aria-expanded={emojiOpen} onClick={()=>setEmojiOpen(!emojiOpen)} disabled={paused}><Smile size={18} /></button><span className="composer-hint">Enter to send <span>·</span> Shift + Enter for a new line</span></div><div className="composer-right-tools">{draft.length>1800 && <span className="character-count">{draft.length}/2000</span>}<button type="submit" className="send-button" aria-label="Send message" disabled={!draft.trim() || sending || paused || loading}><span>Send</span><Send size={15} /></button></div></div>
        {emojiOpen && <div className="emoji-picker" aria-label="Emoji picker">{['✨','😊','💜','🤔','🙌','👍'].map(emoji=><button type="button" key={emoji} aria-label={`Insert ${emoji}`} onClick={()=>{setDraft(prev=>(prev+emoji).slice(0,2000)); setEmojiOpen(false); composerRef.current?.focus();}}>{emoji}</button>)}</div>}
      </form><div className="composer-footnote"><span>Local demo. Messages stay in memory for this session.</span><span>{draft.length>0 ? `${draft.length} characters` : 'Thoughtful conversations start here.'}</span></div>{error && <p className="send-error" role="alert">{error}</p>}
    </div>
  </section>;
}
function ActivityMark() {return <><i /><i /><i /><i /></>;}
