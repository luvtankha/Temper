import { createContext, useContext, useEffect, useMemo, useRef, useState, useCallback, type ReactNode } from 'react';
import type { ChatApi, ChatMessage, ParticipantId,ConnectionState } from '../../models/chat';
import { createMockChatApi } from '../../mocks/chatApi';
import { publicDemo } from '../../app/publicMode';

interface ChatState {
  transport:'local'|'backend';error:string|null;retry:()=>void;
  connection:ConnectionState;remoteOnline:boolean;setTyping:(typing:boolean)=>void;
  messages: ChatMessage[]; localId: ParticipantId; loading: boolean; paused: boolean; remoteTyping: boolean;
  setLocalId: (id:ParticipantId) => void; setPaused: (paused:boolean) => void;
  send: (text:string) => Promise<void>; reset: (sample:boolean) => Promise<void>; previewTyping: () => void;
  selectedMessageId:string|null;setSelectedMessageId:(id:string|null)=>void;
  focusedMessageId:string|null;focusRequest:number;focusMessage:(id:string)=>void;
  inspectorOpen:boolean;inspectMessage:(id:string)=>void;closeInspector:()=>void;
}
const ChatContext = createContext<ChatState | null>(null);
export function ChatProvider({children, api: suppliedApi,transport='local'}: {children:ReactNode; api?:ChatApi;transport?:'local'|'backend'}) {
  const api = useMemo(() => suppliedApi ?? createMockChatApi({fictionalOnly:publicDemo}), [suppliedApi]);
  const [messages,setMessages] = useState<ChatMessage[]>([]);
  const [localId,setLocalId] = useState<ParticipantId>('alex');
  const [connection,setConnection]=useState<ConnectionState>(transport==='local'?'local':api.connect?'connecting':'rest');
  const [online,setOnline]=useState<ParticipantId[]>([]),[roomRevision,setRoomRevision]=useState(0);
  const [loading,setLoading] = useState(true);
  const [error,setError]=useState<string|null>(null),[attempt,setAttempt]=useState(0);
  const [paused,setPaused] = useState(false);
  const [remoteTyping,setRemoteTyping] = useState(false);
  const [selectedMessageId,setSelectedMessageId]=useState<string|null>(null);
  const [focusedMessageId,setFocusedMessageId]=useState<string|null>(null),[focusRequest,setFocusRequest]=useState(0);
  const [inspectorOpen,setInspectorOpen]=useState(false);
  const closeInspector=useCallback(()=>setInspectorOpen(false),[]);
  const typingTimeout = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const merge=(previous:ChatMessage[],incoming:ChatMessage[])=>Array.from(new Map([...previous,...incoming].map(m=>[m.id,m])).values()).sort((a,b)=>a.sequence!==undefined&&b.sequence!==undefined?a.sequence-b.sequence:0);
  useEffect(()=>api.connect?.(localId,{
    message:message=>{setMessages(previous=>merge(previous,[message]));if(message.speakerId!==localId)setRemoteTyping(false);},
    snapshot:messages=>setMessages(previous=>merge(previous,messages)),
    typing:(speaker,typing)=>{if(speaker===localId)return;clearTimeout(typingTimeout.current);setRemoteTyping(typing);if(typing)typingTimeout.current=setTimeout(()=>setRemoteTyping(false),5000);},
    presence:setOnline,connection:setConnection,error:setError,
  }),[api,localId,roomRevision,attempt]);
  useEffect(() => {
    let active = true;
    setLoading(true);setError(null);
    const timer = setTimeout(() => api.listMessages().then(data => {if (active) {setMessages(previous=>merge(previous,data)); setLoading(false);}}).catch(cause=>{if(active){setError(cause instanceof Error?cause.message:'Could not open conversation.');setLoading(false);}}),150);
    return () => {active = false; clearTimeout(timer);};
  },[api,attempt]);
  useEffect(() => () => clearTimeout(typingTimeout.current), []);
  useEffect(() => {clearTimeout(typingTimeout.current); setRemoteTyping(false);},[localId,paused]);
  useEffect(()=>{setSelectedMessageId(null);setFocusedMessageId(null);setInspectorOpen(false);},[localId]);
  async function send(text:string) {
    if (paused || loading) throw new Error('Resume the demo before sending.');
    const message = await api.sendMessage(localId,text);
    setMessages(previous => merge(previous,[message]));api.setTyping?.(false);
  }
  async function reset(sample:boolean) {clearTimeout(typingTimeout.current);setError(null);setRemoteTyping(false);setSelectedMessageId(null);setFocusedMessageId(null);setInspectorOpen(false);try{setMessages(await api.reset(sample));setRoomRevision(r=>r+1);}catch(cause){setError(cause instanceof Error?cause.message:'Could not open conversation.');}}
  function focusMessage(id:string){if(!messages.some(m=>m.id===id))return;setSelectedMessageId(id);setFocusedMessageId(id);setFocusRequest(r=>r+1);}
  function inspectMessage(id:string){if(!messages.some(m=>m.id===id))return;setSelectedMessageId(id);setInspectorOpen(true);}
  function previewTyping() {
    if (paused) return;
    clearTimeout(typingTimeout.current); setRemoteTyping(true);
    typingTimeout.current = setTimeout(() => setRemoteTyping(false),3000);
  }
  return <ChatContext.Provider value={{transport,connection,remoteOnline:online.some(id=>id!==localId),setTyping:typing=>api.setTyping?.(typing),error,retry:()=>setAttempt(a=>a+1),messages,localId,loading,paused,remoteTyping,setLocalId,setPaused,send,reset,previewTyping,selectedMessageId,setSelectedMessageId,focusedMessageId,focusRequest,focusMessage,inspectorOpen,inspectMessage,closeInspector}}>{children}</ChatContext.Provider>;
}
export function useChat() { const context = useContext(ChatContext); if (!context) throw new Error('ChatProvider required'); return context; }
