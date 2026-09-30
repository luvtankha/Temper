import { Navigate, Route, Routes } from 'react-router-dom';
import {lazy,Suspense} from 'react';
import { Shell } from './Shell';
import { MessageCircle, History, Settings2 } from 'lucide-react';
import { ChatProvider } from '../features/chat/ChatProvider';
import { ChatPage } from '../features/chat/ChatPage';
import { AvatarLab } from '../features/avatar/AvatarLab';
import {AnalysisProvider} from '../features/analysis/AnalysisProvider';
const TimelinePage=lazy(()=>import('../features/analysis/ConflictTimeline').then(m=>({default:m.TimelinePage})));

export function App() {
  return <ChatProvider><AnalysisProvider><Shell><Routes>
    <Route path="/" element={<Navigate to="/chat" replace />} />
    <Route path="/chat" element={<ChatPage />} />
    <Route path="/analysis" element={<Suspense fallback={<p className="route-loading">Opening the timeline…</p>}><TimelinePage/></Suspense>} />
    <Route path="/history" element={<Placeholder icon={History} title="Your conversations, revisited" text="Saved conversations and imported chats will appear here when history is connected." />} />
    <Route path="/settings" element={<Placeholder icon={Settings2} title="Make this space yours" text="Participant and conversation preferences will appear here." />} />
    {import.meta.env.DEV && <Route path="/avatar-lab" element={<AvatarLab />} />}
    <Route path="*" element={<Navigate to="/chat" replace />} />
  </Routes></Shell></AnalysisProvider></ChatProvider>;
}

function Placeholder({icon: Icon, title, text}: {icon: typeof MessageCircle; title: string; text: string}) {
  return <section className="route-placeholder"><div className="placeholder-orbit"><Icon size={34} strokeWidth={1.3} /></div><p className="eyebrow">A LITTLE UNDERSTANDING GOES A LONG WAY</p><h1>{title}</h1><p>{text}</p><span className="pill">Local demo · no AI inference</span></section>;
}
