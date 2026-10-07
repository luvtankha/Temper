import { Link } from 'react-router-dom';
import { Activity, ArrowRight, ArrowUpRight, Check, Code2, Github, Layers3, Smartphone, Sparkles } from 'lucide-react';
import '../styles/landing.css';

const repository = 'https://github.com/luvtankha/Temper';
const previewSignals = [
  { name: 'Joy', value: 64, color: '#ffc96c' },
  { name: 'Calm', value: 46, color: '#69d8d1' },
  { name: 'Concern', value: 29, color: '#aa96ed' },
  { name: 'Anger', value: 13, color: '#f48fbb' },
];

export function LandingPage() {
  return <div className="temper-site">
    <a className="site-skip" href="#/" onClick={event=>{event.preventDefault();document.getElementById('site-main')?.focus();}}>Skip to content</a>
    <header className="site-header">
      <Link to="/" className="site-wordmark" aria-label="TEMPER home"><span><Activity size={23} /></span>TEMPER</Link>
      <nav aria-label="Website navigation"><a href="#/" onClick={event=>{event.preventDefault();document.getElementById('how-it-works')?.scrollIntoView({behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});}}>How it works</a><a href={repository}>GitHub <ArrowUpRight size={14} /></a><Link to="/chat" className="site-nav-demo">Explore the demo <ArrowRight size={15} /></Link></nav>
    </header>
    <main id="site-main" tabIndex={-1}>
      <section className="site-hero">
        <div className="site-hero-copy">
          <span className="site-kicker"><span /> AN ANDROID PROJECT FOR HUMAN CONNECTION</span>
          <h1>A little more<br />context.<br /><em>A better connection.</em></h1>
          <p>TEMPER brings an expressive avatar and conversation signals to your Android chat experience.</p>
          <div className="site-hero-actions"><Link to="/chat" className="site-button site-button-primary">Explore the UI demo <ArrowRight size={18} /></Link><a href={`${repository}#readme`} className="site-button site-button-secondary"><Github size={18} /> View the project</a></div>
          <p className="site-demo-caption"><Check size={14} /> Fictional demo · no AI inference · no chat uploads</p>
          <div className="site-platform-note"><Smartphone size={20} /><div><strong>Built for Android.</strong><span>The live overlay requires the installed Android app and your permission. This website previews the interface.</span></div></div>
        </div>
        <div className="site-preview" aria-label="Illustrative TEMPER interface preview">
          <span className="preview-orbit orbit-one" /><span className="preview-orbit orbit-two" />
          <div className="preview-phone">
            <div className="preview-phone-top"><span className="preview-camera" /><span>9:41</span><span>● ● ●</span></div>
            <div className="preview-chat-header"><span className="preview-contact">N</span><div><strong>Nova</strong><small>Fictional conversation</small></div><Sparkles size={18} /></div>
            <div className="preview-chat"><span className="preview-date">THE LAUNCH PLAN</span><p>Friday might be a little tight. There are still a few things to work through.</p><p className="preview-own">Okay, let’s work out what’s realistic together.</p></div>
            <div className="preview-character"><img src={`${import.meta.env.BASE_URL}avatars/female.svg`} alt="Nova avatar illustration" width="200" height="200" /><span><span /> Avatar preview</span></div>
            <div className="preview-composer"><span>Sample conversation</span><span>↗</span></div>
          </div>
          <div className="preview-insights"><div><span className="preview-insight-icon"><Activity size={17} /></span><strong>Conversation spectrum</strong><span className="preview-sample-label">UI sample</span></div><div className="preview-bars">{previewSignals.map(signal=><div key={signal.name}><span>{signal.name}</span><i><b style={{width:`${signal.value}%`,background:signal.color}} /></i><small>{signal.value}%</small></div>)}</div><p>Illustrative values. No conversation has been analyzed.</p></div>
          <div className="preview-label"><span /> A glance at the experience</div>
        </div>
      </section>
      <section id="how-it-works" className="site-features">
        <div className="site-section-heading"><span className="site-kicker">SMALL PRESENCE. MORE CONTEXT.</span><h2>Meet TEMPER.</h2><p>An Android app with a simple switch and a movable companion.</p></div>
        <div className="site-feature-grid">
          <article><span className="site-feature-icon"><Layers3 size={23} /></span><h3>Choose your companion</h3><p>Browse avatars in the Android carousel and choose the character that feels right for you.</p></article>
          <article><span className="site-feature-icon"><Smartphone size={23} /></span><h3>Switch the overlay on</h3><p>After Android permission setup, use the ON/OFF switch and drag the avatar to a comfortable position.</p></article>
          <article><span className="site-feature-icon"><Activity size={23} /></span><h3>Open the spectrum</h3><p>Tap the avatar for estimated conversation signals. Language patterns can suggest context; they cannot determine a person’s feelings.</p></article>
        </div>
      </section>
      <section className="site-project-note"><div><span className="site-kicker">PUBLIC PROJECT · IN DEVELOPMENT</span><h2>Explore the interface.<br />See what’s behind it.</h2><p>The browser demo uses hand-authored sample scores. Android live behavior still has release verification checks outstanding. See the README for setup, supported chats and current limitations.</p></div><div className="site-project-links"><Link to="/chat" className="site-button site-button-primary">Open fictional demo <ArrowRight size={18} /></Link><a href={`${repository}/tree/main/android`} className="site-button site-button-secondary"><Code2 size={18} /> Android source</a></div></section>
    </main>
    <footer className="site-footer"><div><strong>TEMPER</strong><span>Developed by Luv Tankha</span></div><nav aria-label="Footer navigation"><a href={`${repository}#readme`}>README <ArrowUpRight size={13} /></a><a href={repository}>Source <ArrowUpRight size={13} /></a><a href="mailto:luvtankha06@gmail.com">Contact <ArrowUpRight size={13} /></a></nav><small>Website preview · no account required</small></footer>
  </div>;
}
