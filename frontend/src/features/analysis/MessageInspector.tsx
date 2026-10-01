import {useEffect,useRef} from 'react';
import {createPortal} from 'react-dom';
import {useNavigate} from 'react-router-dom';
import {X,Info,ArrowUpRight} from 'lucide-react';
import {useChat} from '../chat/ChatProvider';
import {useAnalysis} from './AnalysisProvider';
import {emotionLabels,signalLabels} from '../../models/analysis';
import {participants} from '../../models/chat';
import {SignalMeter} from './AnalyticsPanel';
import {AnalysisProvenance} from './AnalysisProvenance';

export function MessageInspector() {
  const {messages,selectedMessageId,inspectorOpen,closeInspector,focusMessage}=useChat();
  const {snapshot,status,error,retry}=useAnalysis();const navigate=useNavigate();
  const dialog=useRef<HTMLElement>(null),closeButton=useRef<HTMLButtonElement>(null),restoreFocus=useRef(true);
  const message=messages.find(m=>m.id===selectedMessageId);
  const analysis=snapshot?.messages.find(m=>m.messageId===selectedMessageId);
  useEffect(()=> {
    if(!inspectorOpen||!selectedMessageId)return;
    restoreFocus.current=true;const previous=document.activeElement as HTMLElement|null;
    const shell=document.querySelector<HTMLElement>('.app-shell'),wasInert=shell?.inert??false;
    if(shell)shell.inert=true;closeButton.current?.focus();
    function key(event:KeyboardEvent) {
      if(event.key==='Escape'){event.preventDefault();event.stopPropagation();closeInspector();return;}
      if(event.key!=='Tab')return;
      const nodes=Array.from(dialog.current?.querySelectorAll<HTMLElement>('button:not(:disabled),a,input,select,textarea,[tabindex="0"]')??[]).filter(node=>node.getClientRects().length>0);
      const first=nodes[0],last=nodes.at(-1);
      if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}
      else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}
    }
    window.addEventListener('keydown',key,true);
    return ()=>{window.removeEventListener('keydown',key,true);if(shell)shell.inert=wasInert;if(restoreFocus.current&&previous?.isConnected)previous.focus({preventScroll:true});};
  },[inspectorOpen,selectedMessageId,closeInspector]);
  if(!inspectorOpen||!message)return null;
  const sequence=messages.findIndex(m=>m.id===message.id)+1;
  const context=analysis?.contextMessageIds.map(id=>messages.find(m=>m.id===id)).filter(m=>!!m)??[];
  function activateContext(id:string){restoreFocus.current=false;closeInspector();focusMessage(id);navigate('/chat');}
  return createPortal(<div className="inspector-backdrop" onClick={event=>{if(event.target===event.currentTarget)closeInspector();}}>
    <section ref={dialog} className="message-inspector" role="dialog" aria-modal="true" aria-label="Message details" tabIndex={-1}>
      <header className="inspector-heading"><div><span className="eyebrow">A CLOSER LOOK AT THE LANGUAGE</span><h2>Message #{sequence}</h2><p>{participants.find(p=>p.id===message.speakerId)?.name} · {new Intl.DateTimeFormat('en-IN',{hour:'numeric',minute:'2-digit',timeZone:'Asia/Calcutta'}).format(new Date(message.sentAt))}</p></div><button ref={closeButton} className="icon-button" aria-label="Close message details" onClick={closeInspector}><X size={20}/></button></header>
      <div className="inspector-body"><blockquote>{message.text}</blockquote>
        {!analysis ? <div className="inspector-unavailable" role="status"><Info size={20}/><h3>{status==='error'?'Analysis unavailable':status==='loading'?'Analysis pending…':'No analysis for this message'}</h3><p>{status==='error'?error:'Signal details appear once the adapter returns this turn.'}</p>{status==='error'&&<button className="analytics-toggle" onClick={retry}>Retry analysis</button>}</div> : <>
          <AnalysisProvenance mode={snapshot?.mode??'MOCK'}/>
          <div className="inspector-signal-grid"><section><h3>Estimated emotions</h3>{Object.entries(emotionLabels).map(([key,label])=><SignalMeter key={key} label={label} value={analysis.emotions[key as keyof typeof emotionLabels]}/>)}{(['surprise','neutral'] as const).map(key=>analysis.emotions[key]===undefined?null:<SignalMeter key={key} label={key==='surprise'?'Surprise':'Neutral language'} value={analysis.emotions[key]!}/>)}</section><section><h3>Language signals</h3>{Object.entries(signalLabels).map(([key,label])=><SignalMeter key={key} label={label} value={analysis.signals[key as keyof typeof signalLabels]} color="magenta"/>)}</section></div>
          <section className="inspector-explanation"><h3>Why this matters</h3><p>{analysis.explanation}</p></section>
          <section className="inspector-evidence"><h3>Source & evidence</h3>{analysis.evidence.map((e,i)=><div key={`${e.label}-${i}`}><span className="pill">{e.source}</span><strong>{e.label}</strong><p>{e.description}</p></div>)}</section>
          <section className="inspector-context"><h3>Context shown <span>{context.length} prior turns</span></h3><p>Speaker information and preceding messages provide context for inspection.</p>{context.length?context.map(m=><button key={m.id} onClick={()=>activateContext(m.id)}><span>#{messages.findIndex(row=>row.id===m.id)+1} · {participants.find(p=>p.id===m.speakerId)?.name}</span><strong>{m.text}</strong><ArrowUpRight size={14}/></button>):<small>This is the first turn; no preceding context is available.</small>}</section>
          <p className="inspector-uncertainty"><Info size={15}/><span>Signal scores describe estimated patterns in language. They do not establish someone’s internal emotional state.</span></p>
        </>}
      </div>
    </section>
  </div>,document.body);
}
