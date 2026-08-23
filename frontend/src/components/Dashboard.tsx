import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { meetingApi, type Meeting } from '../api';

export const Dashboard: React.FC = () => {
    const [meetings, setMeetings] = useState<Meeting[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        meetingApi.getAll()
            .then(data => {
                setMeetings(data);
                setLoading(false);
            })
            .catch(err => {
                console.error("Failed to fetch meetings", err);
                setLoading(false);
            });
    }, []);

    const filteredMeetings = meetings.filter(m => 
        m.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
        m.summary?.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const handleDelete = async (id: number | undefined, e: React.MouseEvent) => {
        e.preventDefault(); // Stop Link navigation
        if (!id) return;
        if (!window.confirm("Are you sure you want to delete this meeting?")) return;
        
        try {
            await meetingApi.delete(id);
            setMeetings(meetings.filter(m => m.id !== id));
        } catch (err) {
            console.error("Failed to delete meeting", err);
            alert("Failed to delete meeting");
        }
    };

    return (
        <div className="animate-fade-in">
            <div className="mb-8" style={{ textAlign: 'center', padding: '3rem 0', position: 'relative' }}>
                <div style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    width: '200px',
                    height: '200px',
                    background: 'radial-gradient(circle, rgba(168,85,247,0.15) 0%, rgba(0,0,0,0) 70%)',
                    filter: 'blur(40px)',
                    zIndex: -1
                }}></div>
                <h1 style={{ fontSize: '3.5rem', marginBottom: '1rem' }}>Meet Smarter.</h1>
                <p style={{ fontSize: '1.25rem', maxWidth: '600px', margin: '0 auto 2rem auto', color: 'var(--text-secondary)' }}>
                    Transform your messy meeting audio into beautifully structured intelligence instantly. Focus on the conversation, let AI handle the notes.
                </p>
                <div className="flex justify-center" style={{ gap: '1rem' }}>
                    <Link to="/upload" style={{ textDecoration: 'none' }}>
                        <button>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '0.5rem' }}>
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 16 16 12 12 8"></polyline>
                                <line x1="8" y1="12" x2="16" y2="12"></line>
                            </svg>
                            Start New Meeting
                        </button>
                    </Link>
                    <Link to="/calendar" style={{ textDecoration: 'none' }}>
                        <button className="btn-secondary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                            Schedule Meeting
                        </button>
                    </Link>
                </div>
            </div>

            <div className="mb-8 form-group" style={{ maxWidth: '600px', margin: '0 auto 3rem auto' }}>
                <div style={{ position: 'relative' }}>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--text-secondary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }}>
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                    <input 
                        type="text" 
                        placeholder="Search transcripts, titles, or action items..." 
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        style={{ paddingLeft: '3rem', width: '100%', maxWidth: 'none', background: 'rgba(15, 23, 42, 0.4)' }}
                    />
                </div>
            </div>

            {loading ? (
                <div className="flex justify-center mt-4">
                    <div className="spinner" style={{ borderColor: 'var(--primary-color)', borderTopColor: 'transparent', width: '40px', height: '40px' }}></div>
                </div>
            ) : filteredMeetings.length === 0 ? (
                <div className="card text-center" style={{ padding: '4rem 2rem' }}>
                    <h3 style={{ color: 'var(--text-secondary)' }}>No meetings found</h3>
                    {meetings.length === 0 ? (
                        <>
                            <p>Upload an audio file or transcript to get started.</p>
                            <Link to="/upload" style={{ textDecoration: 'none' }}>
                                <button className="mt-4">Get Started</button>
                            </Link>
                        </>
                    ) : (
                        <p>No meetings match your search query.</p>
                    )}
                </div>
            ) : (
                <div className="grid">
                    {filteredMeetings.map((meeting) => (
                        <Link to={`/meeting/${meeting.id}`} key={meeting.id} style={{ textDecoration: 'none', color: 'inherit' }}>
                            <div className="card" style={{ position: 'relative' }}>
                                <button 
                                    onClick={(e) => handleDelete(meeting.id, e)}
                                    style={{
                                        position: 'absolute',
                                        top: '1rem',
                                        right: '1rem',
                                        background: 'rgba(239, 68, 68, 0.1)',
                                        color: '#ef4444',
                                        padding: '0.5rem',
                                        borderRadius: '8px',
                                        boxShadow: 'none'
                                    }}
                                    title="Delete Meeting"
                                    onMouseOver={(e) => (e.currentTarget.style.background = 'rgba(239, 68, 68, 0.2)')}
                                    onMouseOut={(e) => (e.currentTarget.style.background = 'rgba(239, 68, 68, 0.1)')}
                                >
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                                </button>
                                <h3 style={{ paddingRight: '2.5rem' }}>{meeting.title}</h3>
                                <p style={{ fontSize: '0.875rem' }}>
                                    {new Date(meeting.createdAt || '').toLocaleDateString('en-US', { 
                                        year: 'numeric', month: 'long', day: 'numeric', 
                                        hour: '2-digit', minute: '2-digit' 
                                    })}
                                </p>
                                <p style={{
                                    display: '-webkit-box',
                                    WebkitLineClamp: 3,
                                    WebkitBoxOrient: 'vertical',
                                    overflow: 'hidden',
                                    marginTop: '1rem',
                                    color: 'var(--text-primary)'
                                }}>
                                    {meeting.summary}
                                </p>
                            </div>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );
};
