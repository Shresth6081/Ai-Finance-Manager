import React, { useState, useEffect, useRef } from 'react';
import { Bot, Send, Trash2, ArrowRight } from 'lucide-react';
import api from '../services/api';

const SUGGESTED_PROMPTS = [
    { text: "Summarize my recent transactions", label: "Transaction Summary" },
    { text: "How can I cut down my expenses?", label: "Expense Advice" },
    { text: "Explain compound interest simply", label: "Financial Education" },
    { text: "What is my total spending this month?", label: "Total Spending" },
];

const AIChat = () => {
    const [messages, setMessages] = useState([
        { text: "Hi! I'm your FinManager AI personal assistant. Ask me anything about your finances, budgets, or for general advice!", sender: 'ai' }
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, loading]);

    const handleSend = async (textToSend) => {
        const text = textToSend || input;
        if (!text.trim()) return;

        setMessages(prev => [...prev, { text, sender: 'user' }]);
        if (!textToSend) setInput('');
        setLoading(true);

        try {
            const response = await api.post('/ai/chat', { message: text });
            setMessages(prev => [...prev, { text: response.data, sender: 'ai' }]);
        } catch (error) {
            console.error('Failed to communicate with AI', error);
            setMessages(prev => [...prev, { text: "Sorry, I'm having trouble connecting to the model server. Make sure Ollama is running.", sender: 'ai' }]);
        } finally {
            setLoading(false);
        }
    };

    const clearChat = () => {
        if (window.confirm("Are you sure you want to clear the chat conversation?")) {
            setMessages([
                { text: "Chat cleared! How can I help you with your finances now?", sender: 'ai' }
            ]);
        }
    };

    return (
        <div className="flex flex-col h-[calc(100vh-8rem)] max-w-4xl mx-auto space-y-4">
            {/* Header */}
            <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                <div>
                    <h2 className="text-3xl font-bold text-gray-800 flex items-center gap-3">
                        <Bot className="text-primary" size={32} /> AI Financial Assistant
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">Ask questions, analyze your budget, or get custom financial tip.</p>
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
            <div className="flex-1 min-h-0 bg-white border border-gray-200 rounded-xl flex flex-col overflow-hidden shadow-sm">
                {/* Messages List */}
                <div className="flex-1 overflow-y-auto p-6 space-y-4">
                    {messages.map((msg, idx) => (
                        <div key={idx} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                            <div className={`flex items-start gap-3 max-w-[85%] ${msg.sender === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                                <div className={`w-8 h-8 rounded-full shrink-0 flex items-center justify-center text-xs font-bold ${
                                    msg.sender === 'user' 
                                        ? 'bg-primary text-white' 
                                        : 'bg-gray-100 text-primary border border-gray-200'
                                }`}>
                                    {msg.sender === 'user' ? 'U' : <Bot size={16} />}
                                </div>
                                <div className={`p-4 rounded-2xl text-sm leading-relaxed ${
                                    msg.sender === 'user'
                                        ? 'bg-primary text-white rounded-tr-none shadow-md'
                                        : 'bg-gray-50 border border-gray-200 text-gray-800 rounded-tl-none shadow-sm'
                                }`}>
                                    {msg.text}
                                </div>
                            </div>
                        </div>
                    ))}
                    {loading && (
                        <div className="flex justify-start">
                            <div className="flex items-start gap-3 max-w-[85%]">
                                <div className="w-8 h-8 rounded-full shrink-0 flex items-center justify-center bg-gray-100 text-primary border border-gray-200">
                                    <Bot size={16} />
                                </div>
                                <div className="p-4 bg-gray-50 border border-gray-200 rounded-2xl rounded-tl-none shadow-sm">
                                    <div className="flex gap-1.5 items-center py-1">
                                        <div className="w-2.5 h-2.5 bg-primary/80 rounded-full animate-bounce"></div>
                                        <div className="w-2.5 h-2.5 bg-primary/80 rounded-full animate-bounce [animation-delay:0.2s]"></div>
                                        <div className="w-2.5 h-2.5 bg-primary/80 rounded-full animate-bounce [animation-delay:0.4s]"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                    <div ref={messagesEndRef} />
                </div>

                {/* Suggested Prompts */}
                {messages.length <= 2 && !loading && (
                    <div className="px-6 py-4 bg-gray-50/50 border-t border-gray-200">
                        <p className="text-xs font-semibold text-gray-500 mb-2.5 tracking-wider uppercase">Suggested Topics:</p>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                            {SUGGESTED_PROMPTS.map((p, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => handleSend(p.text)}
                                    className="flex justify-between items-center text-left text-xs bg-white border border-gray-200 p-3 rounded-lg text-gray-700 hover:bg-gray-50 hover:border-gray-350 transition-all font-medium group"
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
                            placeholder="Ask me anything..."
                            disabled={loading}
                            className="flex-1 bg-white border border-gray-300 rounded-xl px-4 py-3 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary disabled:opacity-50"
                        />
                        <button
                            onClick={() => handleSend()}
                            disabled={loading || !input.trim()}
                            className="bg-primary hover:bg-blue-700 text-white px-5 py-3 rounded-xl transition-colors font-semibold flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
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
