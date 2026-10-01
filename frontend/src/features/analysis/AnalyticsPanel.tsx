import {motion,useReducedMotion} from 'framer-motion';
import {Activity,ArrowDownRight,ArrowUpRight,Minus} from 'lucide-react';
import {NavLink} from 'react-router-dom';
import {lazy,Suspense} from 'react';
import {emotionLabels,signalLabels,percent} from '../../models/analysis';
import {useAnalysis} from './AnalysisProvider';
import {AnalysisProvenance} from './AnalysisProvenance';
const ConflictTimeline=lazy(()=>import('./ConflictTimeline').then(m=>({default:m.ConflictTimeline})));

export function SignalMeter({label,value,color='violet'}:{label:string;value:number;color?:'violet'|'magenta'}) {
  const reduced=useReducedMotion();const amount=percent(value);
  return <div className={`signal-meter ${color}`}><div><span>{label}</span><strong>{amount}<small>%</small></strong></div><div className="signal-track" role="progressbar" aria-label={label} aria-valuenow={amount} aria-valuemin={0} aria-valuemax={100}><motion.span initial={false} animate={{width:`${amount}%`}} transition={{duration:reduced?0:.35}} /></div></div>;
}
export function AnalyticsPanel({onNavigate}:{onNavigate?:()=>void}={}) {
  const {snapshot,status,error,retry}=useAnalysis();
  if(status==='error')return <div className="analytics-state" role="alert"><h3>Insights couldn’t load.</h3><p>{error}</p><button className="analytics-toggle" onClick={retry}>Try again</button></div>;
  if(!snapshot)return <div className="analytics-state" role="status"><Activity size={26}/><h3>{status==='loading'?'Preparing demo insights…':'A little context starts here.'}</h3><p>{status==='loading'?'Loading clearly labeled mock fixtures.':'Send a message to preview the analytics layout.'}</p></div>;
  const last=snapshot.messages.at(-1)!;
  const Direction=snapshot.direction==='recovering'?ArrowDownRight:snapshot.direction==='escalating'?ArrowUpRight:Minus;
  return <div className="live-insights" data-analysis-mode={snapshot.mode} data-message-count={snapshot.messages.length} aria-busy={status==='loading'}>
    <AnalysisProvenance mode={snapshot.mode}/>
    <div className="insight-updated">{status==='loading'?'Refreshing preview…':`Latest turn · Message ${last.sequence}`}</div>
    <section className="conflict-card"><div><span className="metric-caption">CONFLICT SIGNAL</span><strong>{percent(snapshot.conflictScore)}<small>/100</small></strong><p>Illustrative score</p></div><div className="score-ring" style={{background:`conic-gradient(#ff2daa ${percent(snapshot.conflictScore)}%, #2b2333 0)`}}><span><Activity size={23}/></span></div></section>
    <div className="insight-stat-grid"><div><span>Current sentiment</span><strong>{snapshot.sentiment>.15?'Positive':snapshot.sentiment<-.15?'Negative':'Neutral'}</strong><small>{snapshot.sentiment.toFixed(2)} · signed scale</small></div><div><span>Emotional intensity</span><strong>{percent(snapshot.emotionalIntensity)}<small>%</small></strong><small>Estimated signal</small></div></div>
    <div className={`conversation-direction ${snapshot.direction}`}><Direction size={18}/><div><span>Conversation direction</span><strong>{snapshot.direction[0].toUpperCase()+snapshot.direction.slice(1)}</strong></div></div>
    <Suspense fallback={<p className="chart-caption">Loading timeline…</p>}><ConflictTimeline compact onNavigate={onNavigate}/></Suspense>
    <section className="signal-section"><h3>Emotion signals <span>ESTIMATED</span></h3>{Object.entries(emotionLabels).map(([key,label])=><SignalMeter key={key} label={label} value={last.emotions[key as keyof typeof emotionLabels]} />)}</section>
    <section className="signal-section"><h3>Conversation signals</h3>{Object.entries(signalLabels).map(([key,label])=><SignalMeter key={key} label={label} value={last.signals[key as keyof typeof signalLabels]} color="magenta" />)}</section>
    <section className="conversation-moments"><h3>Turning points</h3><div><span>Escalation start</span><strong>{snapshot.escalationStart?`Message ${snapshot.escalationStart}`:'None in preview'}</strong></div><div><span>Peak tension</span><strong>{snapshot.peakTension?`Message ${snapshot.peakTension}`:'No turns yet'}</strong></div></section>
    <div className="insight-note"><span className="eyebrow">UNDERSTANDING, NOT ASSUMPTIONS</span><p>{snapshot.mode==='MOCK'?'These values are hand-authored UI fixtures. Future model signals will be estimates of language, never facts about someone’s feelings.':'Some signals use local model estimates. Inspect a message for its source and any semantic proxies. Conflict and turning points remain fixtures.'}</p><NavLink to="/analysis" onClick={onNavigate}>Explore full analysis <ArrowUpRight size={14}/></NavLink></div>
  </div>;
}
