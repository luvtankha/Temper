import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import type { ChatApi, ChatMessage, ParticipantId } from '../../models/chat';
import { createMockChatApi } from '../../mocks/chatApi';

interface ChatState {
  messages: ChatMessage[]; localId: ParticipantId; loading: boolean; paused: boolean; remoteTyping: boolean;
  setLocalId: (id:ParticipantId) => void; setPaused: (paused:boolean) => void;
  send: (text:string) => Promise<void>; reset: (sample:boolean) => Promise<void>; previewTyping: () => void;
  selectedMessageId:string|null;setSelectedMessageId:(id:string|null)=>void;
  focusedMessageId:string|null;focusRequest:number;focusMessage:(id:string)=>void;
}
const ChatContext = createContext<ChatState | null>(null);
export function ChatProvider({children, api: suppliedApi}: {children:ReactNode; api?:ChatApi}) {
  const api = useMemo(() => suppliedApi ?? createMockChatApi(), [suppliedApi]);
  const [messages,setMessages] = useState<ChatMessage[]>([]);
  const [localId,setLocalId] = useState<ParticipantId>('alex');
  const [loading,setLoading] = useState(true);
  const [paused,setPaused] = useState(false);
  const [remoteTyping,setRemoteTyping] = useState(false);
  const [selectedMessageId,setSelectedMessageId]=useState<string|null>(null);
  const [focusedMessageId,setFocusedMessageId]=useState<string|null>(null),[focusRequest,setFocusRequest]=useState(0);
  const typingTimeout = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => api.listMessages().then(data => {if (active) {setMessages(data); setLoading(false);}}),150);
    return () => {active = false; clearTimeout(timer);};
  },[api]);
  useEffect(() => () => clearTimeout(typingTimeout.current), []);
  useEffect(() => {clearTimeout(typingTimeout.current); setRemoteTyping(false);},[localId,paused]);
  useEffect(()=>{setSelectedMessageId(null);setFocusedMessageId(null);},[localId]);
  async function send(text:string) {
    if (paused || loading) throw new Error('Resume the demo before sending.');
    const message = await api.sendMessage(localId,text);
    setMessages(previous => [...previous,message]);
  }
  async function reset(sample:boolean) {clearTimeout(typingTimeout.current); setRemoteTyping(false);setSelectedMessageId(null);setFocusedMessageId(null); setMessages(await api.reset(sample));}
  function focusMessage(id:string){if(!messages.some(m=>m.id===id))return;setSelectedMessageId(id);setFocusedMessageId(id);setFocusRequest(r=>r+1);}
  function previewTyping() {
    if (paused) return;
    clearTimeout(typingTimeout.current); setRemoteTyping(true);
    typingTimeout.current = setTimeout(() => setRemoteTyping(false),3000);
  }
  return <ChatContext.Provider value={{messages,localId,loading,paused,remoteTyping,setLocalId,setPaused,send,reset,previewTyping,selectedMessageId,setSelectedMessageId,focusedMessageId,focusRequest,focusMessage}}>{children}</ChatContext.Provider>;
}
export function useChat() { const context = useContext(ChatContext); if (!context) throw new Error('ChatProvider required'); return context; }
