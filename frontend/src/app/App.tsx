import { Navigate, Route, Routes } from 'react-router-dom';
import { Shell } from './Shell';
import { MessageCircle, ChartNoAxesCombined, History, Settings2 } from 'lucide-react';
import { ChatProvider } from '../features/chat/ChatProvider';
import { ChatPage } from '../features/chat/ChatPage';

export function App() {
  return <ChatProvider><Shell><Routes>
    <Route path="/" element={<Navigate to="/chat" replace />} />
    <Route path="/chat" element={<ChatPage />} />
    <Route path="/analysis" element={<Placeholder icon={ChartNoAxesCombined} title="See the bigger picture" text="Emotional arcs, pivotal moments, and the signals behind them will live here." />} />
    <Route path="/history" element={<Placeholder icon={History} title="Your conversations, revisited" text="Saved conversations and imported chats will appear here when history is connected." />} />
    <Route path="/settings" element={<Placeholder icon={Settings2} title="Make this space yours" text="Participant and conversation preferences will appear here." />} />
    <Route path="*" element={<Navigate to="/chat" replace />} />
  </Routes></Shell></ChatProvider>;
}

function Placeholder({icon: Icon, title, text}: {icon: typeof MessageCircle; title: string; text: string}) {
  return <section className="route-placeholder"><div className="placeholder-orbit"><Icon size={34} strokeWidth={1.3} /></div><p className="eyebrow">A LITTLE UNDERSTANDING GOES A LONG WAY</p><h1>{title}</h1><p>{text}</p><span className="pill">Local demo · no AI inference</span></section>;
}
