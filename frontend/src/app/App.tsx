import { Navigate, Route, Routes } from 'react-router-dom';
import {lazy,Suspense,useMemo} from 'react';
import { Shell } from './Shell';
import { MessageCircle, History, Settings2 } from 'lucide-react';
import { ChatProvider } from '../features/chat/ChatProvider';
import { ChatPage } from '../features/chat/ChatPage';
import { AvatarLab } from '../features/avatar/AvatarLab';
import {AnalysisProvider} from '../features/analysis/AnalysisProvider';
import {MessageInspector} from '../features/analysis/MessageInspector';
import {createBackendAdapters} from '../api/backendAdapters';
const AnalysisDashboard=lazy(()=>import('../features/analysis/AnalysisDashboard').then(m=>({default:m.AnalysisDashboard})));

export function App() {
  const adapters=useMemo(()=>{
    const query=new URLSearchParams(window.location.search);
    const transport=query.get('transport')??import.meta.env.VITE_CHAT_TRANSPORT;
    return transport==='backend'||transport==='rest'?createBackendAdapters(query.get('conversation')??undefined,transport==='backend'):null;
  },[]);
  return <ChatProvider api={adapters?.chat} transport={adapters?'backend':'local'}><AnalysisProvider api={adapters?.analysis}><Shell><Routes>
    <Route path="/" element={<Navigate to="/chat" replace />} />
    <Route path="/chat" element={<ChatPage />} />
    <Route path="/analysis" element={<Suspense fallback={<p className="route-loading">Opening the dashboard…</p>}><AnalysisDashboard/></Suspense>} />
    <Route path="/history" element={<Placeholder icon={History} title="Your conversations, revisited" text="Saved conversations and imported chats will appear here when history is connected." />} />
    <Route path="/settings" element={<Placeholder icon={Settings2} title="Make this space yours" text="Participant and conversation preferences will appear here." />} />
    {import.meta.env.DEV && <Route path="/avatar-lab" element={<AvatarLab />} />}
    <Route path="*" element={<Navigate to="/chat" replace />} />
  </Routes></Shell><MessageInspector/></AnalysisProvider></ChatProvider>;
}

function Placeholder({icon: Icon, title, text}: {icon: typeof MessageCircle; title: string; text: string}) {
  return <section className="route-placeholder"><div className="placeholder-orbit"><Icon size={34} strokeWidth={1.3} /></div><p className="eyebrow">A LITTLE UNDERSTANDING GOES A LONG WAY</p><h1>{title}</h1><p>{text}</p><span className="pill">Local demo · no AI inference</span></section>;
}
