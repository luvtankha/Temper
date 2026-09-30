import { useEffect, useState } from 'react';
import { getHealth } from '../api/client';
import { mockConversation } from '../mocks/provider';

export function App() {
  const [status, setStatus] = useState('Checking backend…');
  useEffect(() => { getHealth().then(h => setStatus(`Backend ${h.status}`)).catch(() => setStatus('Backend unavailable — frontend demo remains available')); }, []);
  return <main className="foundation"><p className="eyebrow">CONVERSATION INTELLIGENCE</p><h1>TEMPER<span>✦</span></h1><p>Every conversation has an emotional arc.</p><div className="foundation-card"><h2>Integration foundation</h2><p role="status">{status}</p><small>{mockConversation.title} · Local mock provider · No inference</small></div></main>;
}
