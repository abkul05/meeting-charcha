import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { meetingApi, type Meeting } from '../api';

export const MeetingDetails: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [meeting, setMeeting] = useState<Meeting | null>(null);
    const [loading, setLoading] = useState(true);
    const [copied, setCopied] = useState(false);
    const [targetLanguage, setTargetLanguage] = useState('Spanish');
    const [isTranslating, setIsTranslating] = useState(false);

    useEffect(() => {
        if (id) {
            meetingApi.getById(parseInt(id))
                .then(data => {
                    setMeeting(data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Failed to fetch meeting", err);
                    setLoading(false);
                });
        }
    }, [id]);

    const handleShare = () => {
        if (!meeting) return;
        const textToCopy = `Meeting: ${meeting.title}\n\nSummary:\n${meeting.summary}\n\nAction Items:\n${meeting.actionItems}\n\nDecisions:\n${meeting.decisions}`;
        navigator.clipboard.writeText(textToCopy).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        });
    };

    const handleTranslate = async () => {
        if (!meeting || !meeting.id) return;
        setIsTranslating(true);
        try {
            const translatedMeeting = await meetingApi.translate(meeting.id, targetLanguage);
            setMeeting(translatedMeeting);
        } catch (err) {
            console.error("Translation failed", err);
            alert("Translation failed. Make sure your API key is configured!");
        } finally {
            setIsTranslating(false);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center" style={{ marginTop: '10vh' }}>
                <div className="spinner" style={{ borderColor: 'var(--primary-color)', borderTopColor: 'transparent', width: '50px', height: '50px' }}></div>
            </div>
        );
    }

    if (!meeting) {
        return (
            <div className="card text-center" style={{ padding: '4rem 2rem' }}>
                <h3>Meeting not found</h3>
                <Link to="/" style={{ textDecoration: 'none' }}>
                    <button className="mt-4">Back to Dashboard</button>
                </Link>
            </div>
        );
    }

    return (
        <div className="animate-fade-in" style={{ maxWidth: '900px', margin: '0 auto' }}>
            <div className="flex items-center justify-between mb-8">
                <div className="flex items-center" style={{ gap: '1rem' }}>
                    <Link to="/" style={{ color: 'var(--text-secondary)' }}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="19" y1="12" x2="5" y2="12"></line>
                            <polyline points="12 19 5 12 12 5"></polyline>
                        </svg>
                    </Link>
                    <h1 style={{ marginBottom: 0 }}>{meeting.title}</h1>
                </div>
                
                <div className="flex items-center" style={{ gap: '1rem' }}>
                    <div className="flex items-center" style={{ gap: '0.5rem', background: 'rgba(255,255,255,0.05)', padding: '0.25rem 0.25rem 0.25rem 1rem', borderRadius: '30px', border: '1px solid rgba(255,255,255,0.1)' }}>
                        <select 
                            value={targetLanguage} 
                            onChange={(e) => setTargetLanguage(e.target.value)}
                            style={{ background: 'transparent', border: 'none', color: 'white', outline: 'none', fontFamily: 'inherit', fontSize: '0.95rem' }}
                        >
                            <option style={{color: 'black'}} value="Spanish">Spanish</option>
                            <option style={{color: 'black'}} value="French">French</option>
                            <option style={{color: 'black'}} value="German">German</option>
                            <option style={{color: 'black'}} value="Hindi">Hindi</option>
                            <option style={{color: 'black'}} value="Japanese">Japanese</option>
                        </select>
                        <button onClick={handleTranslate} disabled={isTranslating} style={{ padding: '0.5rem 1rem', fontSize: '0.95rem' }}>
                            {isTranslating ? 'Translating...' : 'Translate'}
                        </button>
                    </div>

                    <button onClick={handleShare} className="btn-secondary" style={{ backgroundColor: copied ? '#10b981' : undefined, color: copied ? 'white' : undefined }}>
                        {copied ? 'Copied!' : 'Share'}
                    </button>
                </div>
            </div>

            <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
                Captured on {new Date(meeting.createdAt || '').toLocaleDateString('en-US', { 
                    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
                    hour: '2-digit', minute: '2-digit'
                })}
            </p>

            <div className="grid mb-8" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))' }}>
                <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '4px', background: 'var(--primary-gradient)' }}></div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div style={{ padding: '0.5rem', background: 'rgba(99, 102, 241, 0.1)', borderRadius: '8px', color: '#818cf8' }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                        </div>
                        <h2 style={{ margin: 0, fontSize: '1.5rem' }}>Executive Summary</h2>
                    </div>
                    <div style={{ whiteSpace: 'pre-wrap', color: 'var(--text-primary)', lineHeight: '1.7', fontSize: '1.05rem' }}>
                        {meeting.summary}
                    </div>
                </div>

                <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '4px', background: 'linear-gradient(to right, #fbbf24, #f59e0b)' }}></div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div style={{ padding: '0.5rem', background: 'rgba(245, 158, 11, 0.1)', borderRadius: '8px', color: '#fbbf24' }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 11 12 14 22 4"></polyline><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path></svg>
                        </div>
                        <h2 style={{ margin: 0, fontSize: '1.5rem' }}>Action Items</h2>
                    </div>
                    <div style={{ whiteSpace: 'pre-wrap', color: 'var(--text-primary)', lineHeight: '1.7', fontSize: '1.05rem' }}>
                        {meeting.actionItems || 'No action items extracted.'}
                    </div>
                </div>

                <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '4px', background: 'linear-gradient(to right, #34d399, #10b981)' }}></div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div style={{ padding: '0.5rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '8px', color: '#34d399' }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M8 14s1.5 2 4 2 4-2 4-2"></path><line x1="9" y1="9" x2="9.01" y2="9"></line><line x1="15" y1="9" x2="15.01" y2="9"></line></svg>
                        </div>
                        <h2 style={{ margin: 0, fontSize: '1.5rem' }}>Key Decisions</h2>
                    </div>
                    <div style={{ whiteSpace: 'pre-wrap', color: 'var(--text-primary)', lineHeight: '1.7', fontSize: '1.05rem' }}>
                        {meeting.decisions || 'No decisions extracted.'}
                    </div>
                </div>

                <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '4px', background: 'linear-gradient(to right, #f87171, #ef4444)' }}></div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div style={{ padding: '0.5rem', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '8px', color: '#f87171' }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                        </div>
                        <h2 style={{ margin: 0, fontSize: '1.5rem' }}>Open Questions</h2>
                    </div>
                    <div style={{ whiteSpace: 'pre-wrap', color: 'var(--text-primary)', lineHeight: '1.7', fontSize: '1.05rem' }}>
                        {meeting.openQuestions || 'No open questions extracted.'}
                    </div>
                </div>
            </div>

            <div className="card" style={{ backgroundColor: '#0f172a' }}>
                <h3>Original Transcript / Audio Log</h3>
                <div style={{ 
                    whiteSpace: 'pre-wrap', 
                    color: 'var(--text-secondary)', 
                    lineHeight: '1.6',
                    maxHeight: '400px',
                    overflowY: 'auto',
                    padding: '1rem',
                    backgroundColor: 'rgba(0,0,0,0.2)',
                    borderRadius: '8px'
                }}>
                    {meeting.content}
                </div>
            </div>
        </div>
    );
};
