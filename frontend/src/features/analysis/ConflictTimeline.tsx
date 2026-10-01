import {useMemo,useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {CartesianGrid,Line,LineChart,ReferenceArea,ReferenceLine,ResponsiveContainer,Tooltip,XAxis,YAxis} from 'recharts';
import {useAnalysis} from './AnalysisProvider';
import {useChat} from '../chat/ChatProvider';
import {percent,type MessageAnalysis} from '../../models/analysis';
const series=[{key:'sentiment',name:'Sentiment',color:'#79c7b0'},{key:'anger',name:'Anger',color:'#ac80f5'},{key:'frustration',name:'Frustration',color:'#e6a878'},{key:'sarcasm',name:'Sarcasm',color:'#9ba9e5'},{key:'conflict',name:'Conflict',color:'#ff61bb'}] as const;
type SeriesKey=typeof series[number]['key'];
export type TimelineRow={messageId:string;sequence:number}&Record<SeriesKey,number>;
export function timelineRows(messages:MessageAnalysis[]):TimelineRow[]{return messages.map(row=>({messageId:row.messageId,sequence:row.sequence,sentiment:Math.round(Math.max(-1,Math.min(1,row.sentiment))*100),anger:percent(row.emotions.anger),frustration:percent(row.emotions.frustration),sarcasm:percent(row.signals.sarcasm),conflict:percent(row.conflict)}));}
interface PointProps {cx?:number;cy?:number;payload?:TimelineRow;}
export function ConflictTimeline({compact=false,onNavigate}:{compact?:boolean;onNavigate?:()=>void}) {
  const {snapshot,status}=useAnalysis();const {focusMessage}=useChat();const navigate=useNavigate();
  const [visible,setVisible]=useState<SeriesKey[]>(series.map(s=>s.key));
  const rows=useMemo(()=>timelineRows(snapshot?.messages??[]),[snapshot]);
  if(!rows.length)return <div className="timeline-empty" role="status">{status==='loading'?'Loading timeline…':'No conversation turns to plot.'}</div>;
  const shown=series.filter(s=>compact?['sentiment','conflict'].includes(s.key):visible.includes(s.key));
  function activate(messageId:string){focusMessage(messageId);navigate('/chat');onNavigate?.();}
  const markers=snapshot!.events.map(event=>({...event,color:event.kind==='recovery'?'#79c7b0':event.kind==='peak'?'#ff61bb':'#d8a8ee'}));
  return <section className={`conflict-timeline ${compact?'compact':''}`} aria-label="Emotional arc timeline">
    <div className="timeline-heading"><div><span className="eyebrow">THE SHAPE OF THE CONVERSATION</span><h2>Emotional arc</h2></div><span className="pill">{snapshot?.mode==='MOCK'?'Mock fixtures':'Model + fixture signals'}</span></div>
    <p className="chart-caption">{compact?'Sentiment & conflict by message.':'Sentiment, anger, frustration, sarcasm and conflict by message.'} Select a point to return to its message.</p>
    <div className="timeline-plot" data-testid="timeline-plot"><ResponsiveContainer width="100%" height="100%" minWidth={0} debounce={30}>
      <LineChart data={rows} margin={{top:18,right:12,left:0,bottom:6}} accessibilityLayer>
        <CartesianGrid stroke="#312539" strokeDasharray="3 5" vertical={false}/>
        <XAxis dataKey="sequence" tick={{fill:'#9b85ad',fontSize:10}} axisLine={false} tickLine={false} minTickGap={14}/>
        <YAxis domain={[-100,100]} ticks={[-100,0,100]} tick={{fill:'#806c91',fontSize:9}} axisLine={false} tickLine={false} width={compact?32:40}/>
        <ReferenceLine y={0} stroke="#564060"/>
        {snapshot!.escalationStart&&snapshot!.peakTension&&snapshot!.escalationStart!==snapshot!.peakTension&&<ReferenceArea x1={snapshot!.escalationStart} x2={snapshot!.peakTension} fill="#ff2daa" fillOpacity={.055}/>}
        {markers.map(marker=><ReferenceLine key={marker.label} x={marker.sequence!} stroke={marker.color} strokeOpacity={.5} strokeDasharray="3 4"/>)}
        <Tooltip contentStyle={{background:'#211629',border:'1px solid #54345f',borderRadius:8,fontSize:11}} labelFormatter={label=>`Message ${label}`} formatter={(value,name)=>[value,name]} />
        {shown.map(s=><Line key={s.key} dataKey={s.key} name={s.name} stroke={s.color} strokeWidth={s.key==='conflict'?2.5:1.5} type="linear" isAnimationActive={false} activeDot={false} dot={(props:PointProps)=> {
          if(props.cx===undefined||props.cy===undefined||!props.payload)return <g/>;
          const row=props.payload;return <g key={`${s.key}-${row.messageId}`} className="timeline-point" role="button" tabIndex={0} aria-label={`Focus message ${row.sequence} · ${s.name.toLowerCase()}`} onClick={()=>activate(row.messageId)} onKeyDown={event=>{if(event.key==='Enter'||event.key===' '){event.preventDefault();activate(row.messageId);}}}>
            <circle cx={props.cx} cy={props.cy} r={8} fill={s.color} opacity={0}/><circle cx={props.cx} cy={props.cy} r={3} fill="#151019" stroke={s.color} strokeWidth={1.5}/>
          </g>;
        }}/>)}
      </LineChart>
    </ResponsiveContainer></div>
    <div className="timeline-legend" aria-label="Timeline series">{series.filter(s=>!compact||['sentiment','conflict'].includes(s.key)).map(s=>compact?<span key={s.key}><i style={{background:s.color}}/>{s.name}</span>:<button key={s.key} aria-pressed={visible.includes(s.key)} disabled={visible.length===1&&visible.includes(s.key)} onClick={()=>setVisible(current=>current.includes(s.key)?current.filter(k=>k!==s.key):[...current,s.key])}><i style={{background:s.color}}/>{s.name}</button>)}</div>
    <p className="chart-scale">Sentiment −100 to +100 · other signals 0–100 · illustrative values</p>
    <div className="timeline-markers">{markers.map(marker=>{const row=rows.find(r=>r.sequence===marker.sequence);return <button key={marker.label} onClick={()=>row&&activate(row.messageId)}><i style={{background:marker.color}}/><span>{marker.label}</span><strong>#{marker.sequence}</strong></button>;})}</div>
  </section>;
}
export function TimelinePage(){return <section className="timeline-page"><p className="eyebrow">CONVERSATION INTELLIGENCE · DEMO</p><h1>See the bigger picture</h1><p>Follow the shifts between turns. These are mock UI fixtures, not model predictions.</p><ConflictTimeline/></section>;}
