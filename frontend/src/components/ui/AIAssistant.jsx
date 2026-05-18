import React, { useState } from 'react';
import { Bot, X, Send } from 'lucide-react';
import api from '../../services/api';

const AIAssistant = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        { text: "Hi! I'm your financial AI assistant. I can analyze your transactions and give you advice. Just ask!", sender: 'ai' }
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSend = async () => {
        if (!input.trim()) return;

        const userMessage = input;
        setMessages(prev => [...prev, { text: userMessage, sender: 'user' }]);
        setInput('');
        setLoading(true);

        try {
            // For now, we only have one endpoint for generic advice based on context
            // In a real app, we would send the user's prompt to the backend
            // Here we trigger the advice generation
            if (userMessage.toLowerCase().includes('advice') || userMessage.toLowerCase().includes('analysis')) {
                const response = await api.get('/ai/advice');
                setMessages(prev => [...prev, { text: response.data, sender: 'ai' }]);
            } else {
                // Fallback for demo since we haven't built a full chat endpoint yet
                setMessages(prev => [...prev, { text: "I can currently only provide a general financial analysis based on your translation history. Try asking for 'advice' or 'analysis'.", sender: 'ai' }]);
            }

        } catch (error) {
            setMessages(prev => [...prev, { text: "Sorry, I couldn't reach the brain server. Please try again later.", sender: 'ai' }]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed bottom-6 right-6 z-50">
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    className="bg-primary text-white p-4 rounded-full shadow-lg hover:bg-blue-600 transition-transform hover:scale-110"
                >
                    <Bot size={28} />
                </button>
            )}

            {isOpen && (
                <div className="bg-white rounded-lg shadow-2xl w-80 sm:w-96 flex flex-col h-[500px] border border-gray-200">
                    <div className="p-4 bg-primary text-white rounded-t-lg flex justify-between items-center">
                        <h3 className="font-bold flex items-center gap-2"><Bot size={20} /> Finance AI</h3>
                        <button onClick={() => setIsOpen(false)} className="hover:text-gray-200">
                            <X size={20} />
                        </button>
                    </div>

                    <div className="flex-1 p-4 overflow-y-auto space-y-4 bg-gray-50">
                        {messages.map((msg, idx) => (
                            <div key={idx} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                                <div className={`max-w-[80%] p-3 rounded-lg text-sm ${msg.sender === 'user'
                                    ? 'bg-primary text-white rounded-tr-none'
                                    : 'bg-white border border-gray-200 text-gray-800 rounded-tl-none shadow-sm'
                                    }`}>
                                    {msg.text}
                                </div>
                            </div>
                        ))}
                        {loading && (
                            <div className="flex justify-start">
                                <div className="bg-white p-3 rounded-lg border border-gray-200 shadow-sm">
                                    <div className="flex gap-1">
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-75"></div>
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-150"></div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="p-4 border-t bg-white rounded-b-lg">
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                                placeholder="Ask for advice..."
                                className="flex-1 border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-primary"
                            />
                            <button
                                onClick={handleSend}
                                disabled={loading}
                                className="bg-primary text-white p-2 rounded-md hover:bg-blue-600 disabled:opacity-50"
                            >
                                <Send size={18} />
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AIAssistant;
