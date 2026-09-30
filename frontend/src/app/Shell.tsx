import { useEffect, useRef, useState, type ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { Activity, ArrowUpRight, ChartNoAxesCombined, ChevronDown, History, Menu, MessageCircle, PanelRightClose, PanelRightOpen, Settings2, ShieldCheck, Sparkles, X } from 'lucide-react';
import { getHealth } from '../api/client';
import { useChat } from '../features/chat/ChatProvider';
import { participants, remoteParticipant } from '../models/chat';

const navigation = [
  {to:'/chat', title:'Conversation', Icon:MessageCircle},
  {to:'/analysis', title:'Full analysis', Icon:ChartNoAxesCombined},
  {to:'/history', title:'History', Icon:History},
  {to:'/settings', title:'Settings', Icon:Settings2},
];

export function Shell({children}: {children: ReactNode}) {
  const {localId}=useChat();
  const local=participants.find(p=>p.id===localId)!;
  const remote=remoteParticipant(localId);
  const [analyticsOpen, setAnalyticsOpen] = useState(() => window.innerWidth >= 1024);
  const [navOpen, setNavOpen] = useState(false);
  const [backend, setBackend] = useState('Checking backend…');
  const analyticsToggle = useRef<HTMLButtonElement>(null);
  const navToggle = useRef<HTMLButtonElement>(null);
  const analyticsClose = useRef<HTMLButtonElement>(null);
  const navClose = useRef<HTMLButtonElement>(null);
  const analyticsRef = useRef<HTMLElement>(null);
  const navRef = useRef<HTMLElement>(null);
  const [mobile,setMobile]=useState(()=>window.matchMedia('(max-width: 1023px)').matches);
  useEffect(()=> {
    const query=window.matchMedia('(max-width: 1023px)');
    const update=()=>setMobile(query.matches);
    query.addEventListener('change',update);
    return ()=>query.removeEventListener('change',update);
  },[]);
  useEffect(() => { let active = true; getHealth().then(h => active && setBackend(`Backend ${h.status}`)).catch(() => active && setBackend('Backend offline')); return () => {active = false;}; }, []);
  useEffect(() => {
    if (navOpen) navClose.current?.focus();
    else if (analyticsOpen && mobile) analyticsClose.current?.focus();
    function handleKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        if (navOpen) {setNavOpen(false); navToggle.current?.focus();}
        else if (analyticsOpen) {setAnalyticsOpen(false); analyticsToggle.current?.focus();}
      }
      const panel = navOpen ? navRef.current : analyticsOpen && mobile ? analyticsRef.current : null;
      if (event.key !== 'Tab' || !panel) return;
      const nodes = Array.from(panel.querySelectorAll<HTMLElement>('a, button, input, [tabindex="0"]')).filter(el => el.getClientRects().length > 0);
      const first = nodes[0], last = nodes.at(-1);
      if (event.shiftKey && document.activeElement === first) {event.preventDefault(); last?.focus();}
      else if (!event.shiftKey && document.activeElement === last) {event.preventDefault(); first?.focus();}
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [analyticsOpen, navOpen, mobile]);
  function closeAnalytics() {setAnalyticsOpen(false); analyticsToggle.current?.focus();}
  function closeNav() {setNavOpen(false); navToggle.current?.focus();}
  return <div className={`app-shell ${analyticsOpen ? 'analytics-open' : ''}`}>
    <a href="#main-content" className="skip-link">Skip to conversation</a>
    {(navOpen || analyticsOpen) && <button tabIndex={-1} aria-label="Dismiss overlay" className={`overlay-backdrop ${navOpen ? 'nav-backdrop' : ''}`} onClick={() => {closeNav(); closeAnalytics();}} />}
    <aside ref={navRef} className={`sidebar ${navOpen ? 'nav-open' : ''}`} aria-label="Main navigation" role={navOpen ? 'dialog' : undefined} aria-modal={navOpen ? true : undefined}>
      <div className="brand"><span className="brand-symbol"><Activity size={22} /></span><strong>TEMPER<span>®</span></strong><button ref={navClose} className="icon-button mobile-close" aria-label="Close navigation" onClick={closeNav}><X size={18} /></button></div>
      <p className="brand-subtitle">A little more understanding.</p>
      <div className="workspace-label">WORKSPACE <span className="workspace-dot" /></div>
      <nav>{navigation.map(({to,title,Icon}) => <NavLink key={to} to={to} onClick={() => setNavOpen(false)} className={({isActive}) => `nav-item ${isActive ? 'active' : ''}`}><Icon size={18} /><span>{title}</span>{to === '/chat' && <span className="nav-count">1</span>}</NavLink>)}</nav>
      <div className="sidebar-divider" /><div className="workspace-label">CURRENT CONVERSATION</div>
      <NavLink to="/chat" className="conversation-link" onClick={() => setNavOpen(false)}><span className="initial-avatar">{remote.name[0]}</span><span><strong>The launch plan</strong><small>You & {remote.name} · demo</small></span><span className="little-dot" /></NavLink>
      <div className="sidebar-bottom"><div className="privacy-note"><ShieldCheck size={19} /><div><strong>A space to be human</strong><p>Signals invite understanding.<br />They don’t define a person.</p></div></div><div className="profile" aria-label={`Current participant: ${local.name}`}><span className="profile-avatar">{local.name[0]}</span><div><strong>{local.name}</strong><small>Demo workspace</small></div><ChevronDown size={15} /></div></div>
    </aside>
    <div className="workspace">
      <header className="topbar"><div className="breadcrumb"><button ref={navToggle} className="icon-button nav-toggle" onClick={() => setNavOpen(true)} aria-label="Open navigation"><Menu size={20} /></button><span>Workspace</span><span className="breadcrumb-slash">/</span><strong>The launch plan</strong></div><div className="topbar-actions"><span className="pill demo-pill"><span />Local demo</span><button ref={analyticsToggle} className={`analytics-toggle ${analyticsOpen ? 'selected' : ''}`} aria-expanded={analyticsOpen} aria-controls="analytics-panel" onClick={() => setAnalyticsOpen(!analyticsOpen)}>{analyticsOpen ? <PanelRightClose size={17} /> : <PanelRightOpen size={17} />}<span>Insights</span></button></div></header>
      <div className="workspace-body"><main id="main-content" className="main-content">{children}</main>
        {analyticsOpen && <aside ref={analyticsRef} id="analytics-panel" aria-label="Conversation insights" role={mobile ? 'dialog' : undefined} aria-modal={mobile ? true : undefined} className="analytics-panel"><div className="panel-heading"><div><span className="eyebrow">A CLOSER LOOK</span><h2>Conversation insights</h2></div><button ref={analyticsClose} className="icon-button" aria-label="Close insights" onClick={closeAnalytics}><X size={18} /></button></div><div className="analysis-availability"><Sparkles size={16} /><span>Ready for a little context</span></div><div className="insights-empty"><div className="insight-icon"><ChartNoAxesCombined size={32} strokeWidth={1.2} /></div><h3>Between the lines.</h3><p>Estimated signals and emotional shifts will appear here as analysis becomes available.</p></div><div className="insight-note"><span className="eyebrow">UNDERSTANDING, NOT ASSUMPTIONS</span><p>Language offers clues. Emotional signals are estimates, never facts about someone’s feelings.</p><NavLink to="/analysis" onClick={mobile ? closeAnalytics : undefined}>Explore full analysis <ArrowUpRight size={14} /></NavLink></div></aside>}
      </div><footer className="system-footer"><span><span className="status-dot" /> <span role="status">{backend}</span></span><span>Conversation intelligence <span className="footer-star">✦</span> Made for human connection</span></footer>
    </div>
  </div>;
}
