import {createContext,useContext,useEffect,useState,type ReactNode} from 'react';
import {useChat} from '../chat/ChatProvider';
import type {AnalysisApi,ConversationAnalysis} from '../../models/analysis';
import {mockAnalysisApi} from '../../mocks/analysisApi';
interface State {snapshot:ConversationAnalysis|null;status:'empty'|'loading'|'ready'|'error';error:string|null;retry:()=>void;}
const Context=createContext<State|null>(null);
export function AnalysisProvider({children,api=mockAnalysisApi}:{children:ReactNode;api?:AnalysisApi}) {
  const {messages,loading}=useChat();
  const [snapshot,setSnapshot]=useState<ConversationAnalysis|null>(null);
  const [status,setStatus]=useState<State['status']>('loading'),[error,setError]=useState<string|null>(null),[attempt,setAttempt]=useState(0);
  useEffect(()=> {
    const controller=new AbortController();setError(null);
    if(loading){setStatus('loading');return ()=>controller.abort();}
    if(!messages.length){setSnapshot(null);setStatus('empty');return ()=>controller.abort();}
    setStatus('loading');
    api.getConversation(messages,controller.signal).then(result=> {
      if(!controller.signal.aborted){setSnapshot(result);setStatus('ready');}
    }).catch(cause=> {
      if(!controller.signal.aborted){setError(cause instanceof Error?cause.message:'Could not load analysis.');setStatus('error');}
    });
    return ()=>controller.abort();
  },[messages,loading,api,attempt]);
  return <Context.Provider value={{snapshot,status,error,retry:()=>setAttempt(a=>a+1)}}>{children}</Context.Provider>;
}
export function useAnalysis(){const context=useContext(Context);if(!context)throw new Error('AnalysisProvider required');return context;}
