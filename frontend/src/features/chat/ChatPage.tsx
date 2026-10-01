import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { ArrowDown, ArrowUpRight, CheckCheck, ChevronDown, MessageCircle, MoreHorizontal, Send, Smile, Sparkles, X } from 'lucide-react';
import { participants, remoteParticipant } from '../../models/chat';
import { useChat } from './ChatProvider';
import { RemoteAvatar } from '../avatar/RemoteAvatar';
import {useAnalysis} from '../analysis/AnalysisProvider';

export function ChatPage() {
  const {snapshot}=useAnalysis();
  const analysisLabel=snapshot?.mode==='HYBRID'?'model + fixture analysis':snapshot?.mode==='MODEL'?'model analysis':snapshot?'mock analysis':'analysis pending';
  const {messages,localId,setLocalId,loading,paused,setPaused,remoteTyping,send,reset,previewTyping,selectedMessageId:selectedId,setSelectedMessageId:setSelectedId,focusedMessageId,focusRequest,inspectMessage} = useChat();
  const remote = remoteParticipant(localId);
  const {transport,connection,remoteOnline,setTyping,error:loadError,retry}=useChat();
  const live=connection!=='local'&&connection!=='rest';
  const disconnected=live&&connection!=='connected';
  const typingSentAt=useRef(0);
  const typingStopTimer=useRef<ReturnType<typeof setTimeout>|undefined>(undefined);
  function updateDraft(value:string){setDraft(value);if(!live)return;const now=Date.now();if(now-typingSentAt.current>500){setTyping(!!value.trim());typingSentAt.current=now;}clearTimeout(typingStopTimer.current);typingStopTimer.current=setTimeout(()=>setTyping(false),2000);}
  useEffect(()=>()=>clearTimeout(typingStopTimer.current),[]);
  const [draft,setDraft] = useState('');
  const [sending,setSending] = useState(false);
  const [error,setError] = useState('');
  const [optionsOpen,setOptionsOpen] = useState(false);
  const [emojiOpen,setEmojiOpen] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const endRef = useRef<HTMLDivElement>(null);
  const composerRef = useRef<HTMLTextAreaElement>(null);
  const [awayFromBottom,setAwayFromBottom] = useState(false);
  const messageRefs=useRef(new Map<string,HTMLButtonElement>());
  const selected = messages.find(m => m.id === selectedId);
  useEffect(() => {endRef.current?.scrollIntoView({behavior:'instant',block:'end'});},[loading]);
  useEffect(() => {if (!awayFromBottom) endRef.current?.scrollIntoView({behavior:'smooth',block:'end'});},[messages,remoteTyping,awayFromBottom]);
  useEffect(() => {setDraft(''); setError('');},[localId]);
  useEffect(()=> {
    if(!focusedMessageId||loading)return;
    setAwayFromBottom(true);const target=messageRefs.current.get(focusedMessageId);
    target?.scrollIntoView({behavior:'instant',block:'center'});target?.focus({preventScroll:true});
  },[focusedMessageId,focusRequest,loading]);
  async function submit(event?:FormEvent) {
    event?.preventDefault(); if (!draft.trim() || sending || paused || loading || disconnected) return;
    setSending(true); setError('');
    try {await send(draft); setDraft(''); setEmojiOpen(false); setAwayFromBottom(false); composerRef.current?.focus();}
    catch (cause) {setError(cause instanceof Error ? cause.message : 'Could not send message.');}
    finally {setSending(false);}
  }
  function handleKey(event:KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {event.preventDefault(); void submit();}
  }
  return <section className="chat-page" aria-label="Conversation">
    <header className="chat-header"><div className="participant-heading"><span className={`contact-avatar ${remote.variant}`}>{remote.name.slice(0,1)}<i /></span><div><h1>{remote.name}<span>your conversation partner</span></h1><p><span className={paused ? 'paused-dot' : 'online-dot'} />{paused ? 'Demo paused' : remoteTyping ? (live?'Typing…':'Typing in demo…') : live?(remoteOnline?'Online now':'Not connected'):transport==='backend'?'Backend REST session':'Available in local demo'}</p></div></div><div className="chat-header-right"><span className="private-label"><Sparkles size={13} /> More than words</span><button className="icon-button" aria-label="Conversation options" aria-expanded={optionsOpen} onClick={() => setOptionsOpen(!optionsOpen)}><MoreHorizontal size={20} /></button></div>
      {optionsOpen && <div className="conversation-menu"><label>Viewing as<select aria-label="Viewing as" value={localId} onChange={e => setLocalId(e.target.value as typeof localId)}>{participants.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><button onClick={() => {setPaused(!paused);setOptionsOpen(false);}}>{paused ? 'Resume local demo' : 'Pause local demo'}</button><button disabled={paused||live} onClick={() => {previewTyping();setOptionsOpen(false);}}>Preview remote typing</button><button onClick={() => {void reset(false);setSelectedId(null);setOptionsOpen(false);}}>New demo conversation</button><button onClick={() => {void reset(true);setSelectedId(null);setOptionsOpen(false);}}>Load sample conversation</button></div>}
    </header>
    <div className="conversation-banner"><span className="banner-icon"><ActivityMark /></span><p>A little context. A better connection.</p><span>{live?`Live chat · ${analysisLabel}`:transport==='backend'?`Backend chat · ${analysisLabel}`:'Local demo'}</span></div>
    {live&&<p className="live-connection-status" role="status" data-connection-state={connection}>{connection==='connected'?'Live connection ready':connection==='reconnecting'?'Reconnecting… messages will catch up':connection==='error'?'Live connection unavailable':'Connecting to live chat…'}</p>}
    {loadError&&<p role="alert" className="composer-error">{loadError} <button onClick={retry}>Retry conversation</button></p>}
    <div ref={listRef} className="message-list" aria-label="Messages" aria-live="polite" aria-busy={loading} onScroll={() => {const el=listRef.current; if(el) setAwayFromBottom(el.scrollHeight-el.scrollTop-el.clientHeight>90);}}>
      {loading ? <div className="chat-empty"><span className="loading-spinner" /><p>Opening your conversation…</p></div> : messages.length === 0 ? <div className="chat-empty"><MessageCircle size={32} strokeWidth={1.2} /><h2>Every connection starts somewhere.</h2><p>Say hello to {remote.name}.</p></div> : <><div className="date-divider"><span />DEMO CONVERSATION<span /></div><div className="conversation-intro"><span className="intro-spark">✦</span><p>Room for a little more understanding.</p><small>A fictional conversation. A familiar kind of moment.</small></div>{messages.map((message,index) => {
        const own = message.speakerId === localId;
        const speaker = participants.find(p => p.id === message.speakerId)!;
        const showName = index===0 || messages[index-1].speakerId !== message.speakerId;
        return <div className={`message-row ${own ? 'own' : 'remote'}`} key={message.id}>
          {!own && <span className={`message-avatar ${showName ? '' : 'hidden-avatar'}`}>{speaker.name[0]}</span>}
          <div className="message-content">{showName && <span className="message-sender">{own ? 'You' : speaker.name}</span>}<button ref={node=>{if(node)messageRefs.current.set(message.id,node);else messageRefs.current.delete(message.id);}} className={`message-bubble ${selectedId===message.id ? 'message-selected' : ''}`} aria-label={`Message ${index+1} from ${speaker.name}: ${message.text}`} aria-pressed={selectedId===message.id} onClick={() => inspectMessage(message.id)}>{message.text}</button><div className="message-meta"><time dateTime={message.sentAt}>{new Intl.DateTimeFormat('en-IN',{hour:'numeric',minute:'2-digit',timeZone:'Asia/Calcutta'}).format(new Date(message.sentAt))}</time>{own && <CheckCheck size={12} />}<span>demo</span></div></div>
        </div>;
      })}</>}
      {remoteTyping && <div className="typing-indicator"><span className="message-avatar">{remote.name[0]}</span><span className="typing-dots"><i /><i /><i /></span><small>{remote.name} is typing{live?'':' · simulated'}</small></div>}
      <div ref={endRef} />
    </div>
    {awayFromBottom && <button className="jump-bottom" aria-label="Scroll to latest message" onClick={() => {setAwayFromBottom(false); endRef.current?.scrollIntoView({behavior:'smooth'});}}><ArrowDown size={15} /> Latest messages</button>}
    {selected && <div className="message-selection" role="region" aria-label="Selected message"><div><strong>Message {messages.findIndex(m=>m.id===selected.id)+1}</strong><span>{participants.find(p=>p.id===selected.speakerId)?.name} · Signal details are available in the inspector.</span></div><button className="inspect-selected-button" onClick={()=>inspectMessage(selected.id)}>Inspect signals</button><button className="icon-button" aria-label="Close selected message" onClick={() => setSelectedId(null)}><X size={15} /></button></div>}
    <div className="composer-wrapper">
      <RemoteAvatar localId={localId} typing={remoteTyping} paused={paused} />
      <div className="composer-topline"><span><span className="small-spark">✦</span> A space to say what you mean.</span><span>CHATTING AS {participants.find(p=>p.id===localId)?.name.toUpperCase()}</span></div>
      <form className="composer" onSubmit={submit}>
        <textarea ref={composerRef} aria-label={`Message ${remote.name}`} placeholder={`Message ${remote.name}…`} value={draft} onChange={e=>updateDraft(e.target.value)} onBlur={()=>setTyping(false)} onKeyDown={handleKey} maxLength={2000} rows={2} disabled={paused || loading || disconnected} />
        <div className="composer-tools"><div className="composer-left-tools"><button type="button" className="icon-button" aria-label="Add emoji" aria-expanded={emojiOpen} onClick={()=>setEmojiOpen(!emojiOpen)} disabled={paused}><Smile size={18} /></button><span className="composer-hint">Enter to send <span>·</span> Shift + Enter for a new line</span></div><div className="composer-right-tools">{draft.length>1800 && <span className="character-count">{draft.length}/2000</span>}<button type="submit" className="send-button" aria-label="Send message" disabled={!draft.trim() || sending || paused || loading || disconnected}><span>Send</span><Send size={15} /></button></div></div>
        {emojiOpen && <div className="emoji-picker" aria-label="Emoji picker">{['✨','😊','💜','🤔','🙌','👍'].map(emoji=><button type="button" key={emoji} aria-label={`Insert ${emoji}`} onClick={()=>{setDraft(prev=>(prev+emoji).slice(0,2000)); setEmojiOpen(false); composerRef.current?.focus();}}>{emoji}</button>)}</div>}
      </form><div className="composer-footnote"><span>{live?`Live delivery · in-memory backend · ${analysisLabel}`:transport==='backend'?`REST delivery · in-memory backend · ${analysisLabel}`:'Local demo. Messages stay in memory for this session.'}</span><span>{draft.length>0 ? `${draft.length} characters` : 'Thoughtful conversations start here.'}</span></div>{error && <p className="send-error" role="alert">{error}</p>}
    </div>
  </section>;
}
function ActivityMark() {return <><i /><i /><i /><i /></>;}


