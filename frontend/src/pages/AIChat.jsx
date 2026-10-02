import React, { useState, useEffect, useRef } from 'react';
import { 
    Bot, Send, Trash2, ArrowRight, ChevronDown, ChevronUp, 
    CheckCircle2, XCircle, AlertCircle, Wrench, Sparkles, Clock, Check
} from 'lucide-react';
import api from '../services/api';

const SUGGESTED_PROMPTS = [
    { text: "Analyze my category spending breakdown for this month", label: "Monthly Spend Breakdown" },
    { text: "Detect any recurring subscriptions or price increases", label: "Subscription Audit" },
    { text: "Compare my spending against the 50/30/20 budget rule", label: "50/30/20 Budget Check" },
    { text: "Simulate saving $250/month over 24 months with 8% return", label: "Wealth Forecast" },
];

const AIChat = () => {
    const [messages, setMessages] = useState([
        { 
            id: 'init-1',
            text: "Hello! I am FinAgent, your autonomous financial AI assistant. I can query your actual database records, analyze subscriptions, forecast wealth trajectory, and help you manage your budget.", 
            sender: 'ai',
            thoughts: [],
            actionProposal: null
        }
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [activeThoughts, setActiveThoughts] = useState([]);
    const [expandedThoughts, setExpandedThoughts] = useState({});
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, loading, activeThoughts]);

    const toggleThoughtExpand = (msgId) => {
        setExpandedThoughts(prev => ({ ...prev, [msgId]: !prev[msgId] }));
    };

    const handleSend = async (textToSend) => {
        const text = textToSend || input;
        if (!text.trim() || loading) return;

        const userMsgId = 'user-' + Date.now();
        const aiMsgId = 'ai-' + (Date.now() + 1);

        setMessages(prev => [
            ...prev, 
            { id: userMsgId, text, sender: 'user', thoughts: [] }
        ]);
        if (!textToSend) setInput('');
        setLoading(true);
        setActiveThoughts([]);

        // Create initial placeholder for streaming AI message
        setMessages(prev => [
            ...prev,
            { id: aiMsgId, text: '', sender: 'ai', thoughts: [], actionProposal: null, isStreaming: true }
        ]);

        const token = localStorage.getItem('token');
        const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085/api/v1';
        const streamUrl = `${baseUrl}/ai/agent/stream?prompt=${encodeURIComponent(text)}&token=${encodeURIComponent(token || '')}`;

        try {
            const response = await fetch(streamUrl, {
                headers: {
                    'Authorization': token ? `Bearer ${token}` : ''
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let currentThoughtSteps = [];
            let responseText = '';
            let currentActionProposal = null;

            while (true) {
                const { value, done } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop(); // Keep incomplete chunk in buffer

                let currentEvent = 'message';

                for (const line of lines) {
                    const trimmed = line.trim();
                    if (trimmed.startsWith('event:')) {
                        currentEvent = trimmed.replace('event:', '').trim();
                    } else if (trimmed.startsWith('data:')) {
                        const rawData = trimmed.replace('data:', '').trim();
                        if (!rawData) continue;

                        try {
                            const parsed = JSON.parse(rawData);

                            if (currentEvent === 'thought' || parsed.type === 'thought') {
                                const step = { type: 'thought', message: parsed.message || 'Thinking...' };
                                currentThoughtSteps = [...currentThoughtSteps, step];
                                setActiveThoughts([...currentThoughtSteps]);
                            } else if (currentEvent === 'tool_call' || parsed.type === 'tool_call') {
                                const step = { 
                                    type: 'tool_call', 
                                    toolName: parsed.toolName, 
                                    message: `Running tool: ${parsed.toolName}`,
                                    payload: parsed.payload 
                                };
                                currentThoughtSteps = [...currentThoughtSteps, step];
                                setActiveThoughts([...currentThoughtSteps]);
                            } else if (currentEvent === 'tool_result' || parsed.type === 'tool_result') {
                                const step = { 
                                    type: 'tool_result', 
                                    toolName: parsed.toolName, 
                                    message: `Completed ${parsed.toolName}` 
                                };
                                currentThoughtSteps = [...currentThoughtSteps, step];
                                setActiveThoughts([...currentThoughtSteps]);
                            } else if (currentEvent === 'token' || parsed.type === 'token') {
                                responseText += (parsed.message || '');
                            } else if (currentEvent === 'action_required' || parsed.type === 'action_required') {
                                currentActionProposal = parsed.payload;
                            } else if (currentEvent === 'done' || parsed.type === 'done') {
                                if (parsed.message && !responseText) {
                                    responseText = parsed.message;
                                }
                            }

                            // Update message state in real time
                            setMessages(prev => prev.map(m => {
                                if (m.id === aiMsgId) {
                                    return {
                                        ...m,
                                        text: responseText,
                                        thoughts: currentThoughtSteps,
                                        actionProposal: currentActionProposal,
                                        isStreaming: true
                                    };
                                }
                                return m;
                            }));
                        } catch (err) {
                            console.warn('Failed to parse SSE JSON chunk:', rawData);
                        }
                    }
                }
            }

            // Mark stream finished
            setMessages(prev => prev.map(m => {
                if (m.id === aiMsgId) {
                    return {
                        ...m,
                        text: responseText || "Task completed.",
                        thoughts: currentThoughtSteps,
                        actionProposal: currentActionProposal,
                        isStreaming: false
                    };
                }
                return m;
            }));

        } catch (error) {
            console.error('SSE Stream Error:', error);
            // Fallback to regular chat endpoint if SSE fails
            try {
                const fallbackRes = await api.post('/ai/agent/chat', { prompt: text });
                setMessages(prev => prev.map(m => {
                    if (m.id === aiMsgId) {
                        return {
                            ...m,
                            text: fallbackRes.data?.response || "I have analyzed your request.",
                            isStreaming: false
                        };
                    }
                    return m;
                }));
            } catch (fallbackErr) {
                setMessages(prev => prev.map(m => {
                    if (m.id === aiMsgId) {
                        return {
                            ...m,
                            text: "I encountered an error connecting to the agent engine. Please check your backend connection.",
                            isStreaming: false
                        };
                    }
                    return m;
                }));
            }
        } finally {
            setLoading(false);
            setActiveThoughts([]);
        }
    };

    const handleExecuteAction = async (msgId, proposal) => {
        try {
            setMessages(prev => prev.map(m => {
                if (m.id === msgId && m.actionProposal) {
                    return {
                        ...m,
                        actionProposal: { ...m.actionProposal, status: 'EXECUTING' }
                    };
                }
                return m;
            }));

            const response = await api.post('/ai/agent/execute-action', proposal);

            setMessages(prev => prev.map(m => {
                if (m.id === msgId && m.actionProposal) {
                    return {
                        ...m,
                        actionProposal: { 
                            ...m.actionProposal, 
                            status: 'APPROVED', 
                            resultMessage: response.data?.message || 'Executed successfully!' 
                        }
                    };
                }
                return m;
            }));
        } catch (err) {
            console.error('Failed to execute agent action:', err);
            setMessages(prev => prev.map(m => {
                if (m.id === msgId && m.actionProposal) {
                    return {
                        ...m,
                        actionProposal: { 
                            ...m.actionProposal, 
                            status: 'FAILED', 
                            resultMessage: err.response?.data?.message || 'Failed to execute action.' 
                        }
                    };
                }
                return m;
            }));
        }
    };

    const handleRejectAction = (msgId) => {
        setMessages(prev => prev.map(m => {
            if (m.id === msgId && m.actionProposal) {
                return {
                    ...m,
                    actionProposal: { ...m.actionProposal, status: 'REJECTED' }
                };
            }
            return m;
        }));
    };

    const clearChat = () => {
        if (window.confirm("Clear current conversation history?")) {
            setMessages([
                { 
                    id: 'init-1', 
                    text: "Chat cleared! How can I assist with your financial analytics today?", 
                    sender: 'ai', 
                    thoughts: [], 
                    actionProposal: null 
                }
            ]);
        }
    };

    return (
        <div className="flex flex-col h-[calc(100vh-8rem)] max-w-4xl mx-auto space-y-4">
            {/* Header */}
            <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                <div>
                    <h2 className="text-3xl font-bold text-gray-800 flex items-center gap-3">
                        <div className="p-2 bg-blue-50 border border-blue-200 rounded-xl text-primary">
                            <Bot size={28} />
                        </div>
                        FinAgent Assistant
                        <span className="text-xs bg-emerald-100 text-emerald-700 font-semibold px-2.5 py-1 rounded-full border border-emerald-300">
                            ReAct Engine
                        </span>
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">Autonomous reasoning with real-time tool calling and interactive approvals.</p>
                </div>
                <button
                    onClick={clearChat}
                    title="Clear Chat"
                    className="p-2 text-gray-400 hover:text-red-600 hover:bg-gray-100 rounded-lg transition-colors"
                >
                    <Trash2 size={20} />
                </button>
            </div>

            {/* Chat Area */}
            <div className="flex-1 min-h-0 bg-white border border-gray-200 rounded-2xl flex flex-col overflow-hidden shadow-sm">
                {/* Messages List */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                    {messages.map((msg) => {
                        const hasThoughts = msg.thoughts && msg.thoughts.length > 0;
                        const isExpanded = expandedThoughts[msg.id] ?? false;

                        return (
                            <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                                <div className={`flex items-start gap-3 max-w-[90%] ${msg.sender === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                                    <div className={`w-8 h-8 rounded-full shrink-0 flex items-center justify-center text-xs font-bold ${
                                        msg.sender === 'user' 
                                            ? 'bg-primary text-white shadow-sm' 
                                            : 'bg-blue-50 text-primary border border-blue-200'
                                    }`}>
                                        {msg.sender === 'user' ? 'U' : <Sparkles size={16} />}
                                    </div>

                                    <div className="space-y-3 flex-1 min-w-0">
                                        {/* Thought Trace Accordion */}
                                        {msg.sender === 'ai' && hasThoughts && (
                                            <div className="border border-indigo-100 bg-indigo-50/50 rounded-xl overflow-hidden text-xs">
                                                <button
                                                    onClick={() => toggleThoughtExpand(msg.id)}
                                                    className="w-full px-3.5 py-2 flex items-center justify-between text-indigo-700 hover:bg-indigo-100/50 font-medium transition-colors"
                                                >
                                                    <span className="flex items-center gap-2">
                                                        <Wrench size={14} className="text-indigo-600" />
                                                        <span>Agent Reasoning & Tool Calls ({msg.thoughts.length} steps)</span>
                                                        {msg.isStreaming && (
                                                            <span className="inline-block w-2 h-2 rounded-full bg-indigo-500 animate-pulse" />
                                                        )}
                                                    </span>
                                                    {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                                                </button>

                                                {isExpanded && (
                                                    <div className="p-3 border-t border-indigo-100 bg-white/70 space-y-2 font-mono text-[11px] text-gray-700">
                                                        {msg.thoughts.map((step, idx) => (
                                                            <div key={idx} className="flex items-start gap-2">
                                                                <span className="text-indigo-500 font-bold">[{idx + 1}]</span>
                                                                <div className="flex-1">
                                                                    <span className={step.type === 'tool_call' ? 'text-amber-700 font-semibold' : 'text-gray-700'}>
                                                                        {step.message}
                                                                    </span>
                                                                    {step.payload && (
                                                                        <pre className="mt-1 p-2 bg-gray-50 rounded border border-gray-200 text-[10px] text-gray-600 overflow-x-auto">
                                                                            {typeof step.payload === 'string' ? step.payload : JSON.stringify(step.payload, null, 2)}
                                                                        </pre>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>
                                        )}

                                        {/* Main Message Bubble */}
                                        {(msg.text || !msg.isStreaming) && (
                                            <div className={`p-4 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap ${
                                                msg.sender === 'user'
                                                    ? 'bg-primary text-white rounded-tr-none shadow-md'
                                                    : 'bg-gray-50 border border-gray-200 text-gray-800 rounded-tl-none shadow-sm'
                                            }`}>
                                                {msg.text || (msg.isStreaming ? "Thinking..." : "")}
                                            </div>
                                        )}

                                        {/* Interactive Action Proposal Card */}
                                        {msg.actionProposal && (
                                            <div className="border-2 border-primary/30 bg-blue-50/60 rounded-2xl p-4 shadow-sm space-y-3">
                                                <div className="flex items-start justify-between gap-2">
                                                    <div>
                                                        <div className="flex items-center gap-2">
                                                            <span className="bg-primary text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full">
                                                                Action Proposal
                                                            </span>
                                                            <h4 className="font-bold text-gray-900 text-sm">
                                                                {msg.actionProposal.title || "Proposed Financial Action"}
                                                            </h4>
                                                        </div>
                                                        <p className="text-xs text-gray-600 mt-1">
                                                            {msg.actionProposal.description}
                                                        </p>
                                                    </div>
                                                </div>

                                                {/* Action Parameter Details */}
                                                {msg.actionProposal.parameters && Object.keys(msg.actionProposal.parameters).length > 0 && (
                                                    <div className="grid grid-cols-2 gap-2 bg-white/80 p-2.5 rounded-xl border border-blue-100 text-xs">
                                                        {Object.entries(msg.actionProposal.parameters).map(([k, v]) => (
                                                            <div key={k} className="flex flex-col">
                                                                <span className="text-[10px] font-semibold text-gray-400 uppercase">{k}</span>
                                                                <span className="font-medium text-gray-800">{String(v)}</span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}

                                                {/* Action Status and Buttons */}
                                                <div className="pt-2 flex items-center justify-between border-t border-blue-100">
                                                    {msg.actionProposal.status === 'PENDING' && (
                                                        <div className="flex items-center gap-2 w-full justify-end">
                                                            <button
                                                                onClick={() => handleRejectAction(msg.id)}
                                                                className="px-3 py-1.5 text-xs font-semibold text-gray-600 hover:text-gray-800 hover:bg-gray-200/60 rounded-lg transition-colors"
                                                            >
                                                                Dismiss
                                                            </button>
                                                            <button
                                                                onClick={() => handleExecuteAction(msg.id, msg.actionProposal)}
                                                                className="px-4 py-1.5 text-xs font-semibold bg-primary hover:bg-blue-700 text-white rounded-lg shadow-sm flex items-center gap-1.5 transition-all"
                                                            >
                                                                <Check size={14} /> Approve & Execute
                                                            </button>
                                                        </div>
                                                    )}

                                                    {msg.actionProposal.status === 'EXECUTING' && (
                                                        <div className="flex items-center gap-2 text-primary text-xs font-medium">
                                                            <Clock size={14} className="animate-spin" /> Executing proposal in database...
                                                        </div>
                                                    )}

                                                    {msg.actionProposal.status === 'APPROVED' && (
                                                        <div className="flex items-center gap-1.5 text-emerald-700 text-xs font-semibold">
                                                            <CheckCircle2 size={16} /> {msg.actionProposal.resultMessage || 'Action Executed Successfully'}
                                                        </div>
                                                    )}

                                                    {msg.actionProposal.status === 'REJECTED' && (
                                                        <div className="flex items-center gap-1.5 text-gray-500 text-xs font-medium">
                                                            <XCircle size={16} /> Proposal Dismissed
                                                        </div>
                                                    )}

                                                    {msg.actionProposal.status === 'FAILED' && (
                                                        <div className="flex items-center gap-1.5 text-red-600 text-xs font-medium">
                                                            <AlertCircle size={16} /> {msg.actionProposal.resultMessage || 'Execution Failed'}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })}

                    {/* Active Live Thought Stream Indicator */}
                    {loading && activeThoughts.length > 0 && (
                        <div className="flex justify-start">
                            <div className="flex items-start gap-3 max-w-[90%]">
                                <div className="w-8 h-8 rounded-full shrink-0 flex items-center justify-center bg-blue-50 text-primary border border-blue-200">
                                    <Sparkles size={16} className="animate-spin" />
                                </div>
                                <div className="p-3 bg-blue-50/50 border border-blue-200 rounded-2xl text-xs text-blue-900 space-y-1">
                                    <div className="flex items-center gap-2 font-semibold text-primary">
                                        <Wrench size={14} className="animate-pulse" />
                                        <span>Agent Execution in Progress:</span>
                                    </div>
                                    <p className="text-gray-600 font-mono text-[11px]">
                                        {activeThoughts[activeThoughts.length - 1]?.message}
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}

                    <div ref={messagesEndRef} />
                </div>

                {/* Suggested Prompts */}
                {messages.length <= 2 && !loading && (
                    <div className="px-6 py-4 bg-gray-50/60 border-t border-gray-200">
                        <p className="text-xs font-semibold text-gray-500 mb-2.5 tracking-wider uppercase">Agent Workflows & Queries:</p>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                            {SUGGESTED_PROMPTS.map((p, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => handleSend(p.text)}
                                    className="flex justify-between items-center text-left text-xs bg-white border border-gray-200 p-3 rounded-xl text-gray-700 hover:bg-gray-50 hover:border-primary/40 transition-all font-medium group shadow-2xs"
                                >
                                    <span>{p.label}</span>
                                    <ArrowRight size={14} className="text-gray-400 group-hover:text-primary transition-colors group-hover:translate-x-0.5 transform duration-200" />
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {/* Input Area */}
                <div className="p-4 bg-gray-50 border-t border-gray-200">
                    <div className="flex gap-3">
                        <input
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                            placeholder="Ask agent (e.g. 'Analyze my food spending', 'Detect price hikes')..."
                            disabled={loading}
                            className="flex-1 bg-white border border-gray-300 rounded-xl px-4 py-3 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary disabled:opacity-50 shadow-inner"
                        />
                        <button
                            onClick={() => handleSend()}
                            disabled={loading || !input.trim()}
                            className="bg-primary hover:bg-blue-700 text-white px-5 py-3 rounded-xl transition-colors font-semibold flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                        >
                            <Send size={16} /> <span>Send</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AIChat;
