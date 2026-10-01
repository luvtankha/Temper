import type {ConversationAnalysis} from '../../models/analysis';
export function AnalysisProvenance({mode}:{mode:ConversationAnalysis['mode']}) {
  return <div className="analysis-fixture-note"><span className="fixture-dot"/><strong>{mode==='MOCK'?'Mock fixtures':mode==='HYBRID'?'Estimates + fixture signals':'Model estimates'}</strong><span>{mode==='MOCK'?'No AI inference':mode==='HYBRID'?'See source & evidence':'Language signals'}</span></div>;
}
