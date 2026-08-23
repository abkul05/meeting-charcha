import React, { useState, useEffect } from 'react';
import { scheduleApi, type ScheduledMeeting } from '../api';
import { Link } from 'react-router-dom';

export const Calendar: React.FC = () => {
    const [meetings, setMeetings] = useState<ScheduledMeeting[]>([]);
    const [title, setTitle] = useState('');
    const [email, setEmail] = useState('');
    const [date, setDate] = useState('');
    const [time, setTime] = useState('');
    const [isScheduling, setIsScheduling] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchMeetings();
    }, []);

    const fetchMeetings = async () => {
        try {
            const data = await scheduleApi.getAll();
            setMeetings(data);
        } catch (err) {
            console.error("Failed to fetch schedule", err);
        } finally {
            setLoading(false);
        }
    };

    const handleSchedule = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title || !email || !date || !time) return;

        setIsScheduling(true);
        try {
            // Combine date and time into ISO string
            const scheduledTime = new Date(`${date}T${time}`).toISOString();
            
            await scheduleApi.schedule({
                title,
                userEmail: email,
                scheduledTime
            });
            
            setTitle('');
            setEmail('');
            setDate('');
            setTime('');
            fetchMeetings();
            alert("Meeting Scheduled Successfully! An email reminder will be sent 10 minutes prior.");
        } catch (err) {
            console.error("Failed to schedule", err);
            alert("Failed to schedule meeting.");
        } finally {
            setIsScheduling(false);
        }
    };

    return (
        <div className="animate-fade-in" style={{ maxWidth: '1000px', margin: '0 auto' }}>
            <div className="flex items-center justify-between mb-8">
                <div className="flex items-center" style={{ gap: '1rem' }}>
                    <Link to="/" style={{ color: 'var(--text-secondary)' }}>
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ transition: 'var(--transition)' }} onMouseOver={(e) => e.currentTarget.style.color = '#fff'} onMouseOut={(e) => e.currentTarget.style.color = 'var(--text-secondary)'}>
                            <line x1="19" y1="12" x2="5" y2="12"></line>
                            <polyline points="12 19 5 12 12 5"></polyline>
                        </svg>
                    </Link>
                    <h1 style={{ marginBottom: 0, textShadow: '0 0 40px rgba(123, 47, 247, 0.4)' }}>Meeting Schedule</h1>
                </div>
            </div>

            <div className="grid" style={{ gridTemplateColumns: '1.2fr 1.5fr', gap: '2.5rem' }}>
                <div className="card form-container" style={{ position: 'relative', overflow: 'hidden' }}>
                    {/* Decorative gradient orb */}
                    <div style={{ position: 'absolute', top: '-50px', right: '-50px', width: '150px', height: '150px', background: 'radial-gradient(circle, rgba(168, 85, 247, 0.2) 0%, transparent 70%)', borderRadius: '50%' }}></div>
                    
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
                        <div style={{ padding: '0.75rem', background: 'linear-gradient(135deg, rgba(168, 85, 247, 0.2), rgba(0, 242, 254, 0.1))', borderRadius: '12px', color: '#a855f7', boxShadow: 'inset 0 0 10px rgba(168, 85, 247, 0.1)' }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                        </div>
                        <h2 style={{ margin: 0, fontSize: '1.6rem', color: '#fff' }}>Plan a Session</h2>
                    </div>
                    <form onSubmit={handleSchedule} style={{ position: 'relative', zIndex: 1 }}>
                        <div className="form-group">
                            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#a855f7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                                Meeting Title
                            </label>
                            <input 
                                type="text" 
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder="e.g. Q3 Roadmap Review" 
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#00f2fe" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
                                Your Email (for Reminders)
                            </label>
                            <input 
                                type="email" 
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com" 
                                required
                            />
                        </div>
                        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '2rem' }}>
                            <div className="form-group" style={{ marginBottom: 0 }}>
                                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                                    Date
                                </label>
                                <input 
                                    type="date" 
                                    value={date}
                                    onChange={(e) => setDate(e.target.value)}
                                    required
                                    style={{ colorScheme: 'dark', minHeight: '52px' }}
                                />
                            </div>
                            <div className="form-group" style={{ marginBottom: 0 }}>
                                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                                    Time
                                </label>
                                <input 
                                    type="time" 
                                    value={time}
                                    onChange={(e) => setTime(e.target.value)}
                                    required
                                    style={{ colorScheme: 'dark', minHeight: '52px' }}
                                />
                            </div>
                        </div>
                        <button type="submit" disabled={isScheduling} style={{ width: '100%', padding: '1rem', display: 'flex', justifyContent: 'center', gap: '0.75rem', fontSize: '1.15rem' }}>
                            {isScheduling ? 'Scheduling...' : (
                                <>
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="16"></line><line x1="8" y1="12" x2="16" y2="12"></line></svg>
                                    Add to Calendar
                                </>
                            )}
                        </button>
                    </form>
                </div>

                <div className="card" style={{ background: 'rgba(20, 25, 45, 0.6)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '2rem' }}>
                        <div style={{ padding: '0.75rem', background: 'rgba(0, 242, 254, 0.1)', borderRadius: '12px', color: '#00f2fe' }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>
                        </div>
                        <h2 style={{ margin: 0, fontSize: '1.6rem' }}>Upcoming Meetings</h2>
                    </div>
                    
                    {loading ? (
                        <div className="flex justify-center mt-8">
                            <div className="spinner" style={{ borderColor: 'var(--primary-color)', borderTopColor: 'transparent', width: '40px', height: '40px' }}></div>
                        </div>
                    ) : meetings.length === 0 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem 0', opacity: 0.5 }}>
                            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" style={{ marginBottom: '1rem' }}><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                            <p style={{ margin: 0, fontSize: '1.1rem' }}>No meetings scheduled.</p>
                            <p style={{ margin: 0, fontSize: '0.9rem' }}>Fill out the form to add one.</p>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxHeight: '500px', overflowY: 'auto', paddingRight: '0.5rem' }}>
                            {meetings.sort((a, b) => new Date(a.scheduledTime).getTime() - new Date(b.scheduledTime).getTime()).map(meeting => (
                                <div key={meeting.id} style={{ 
                                    background: 'linear-gradient(90deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.01) 100%)', 
                                    border: '1px solid rgba(255,255,255,0.08)',
                                    borderLeft: `4px solid ${meeting.reminderSent ? '#10b981' : '#a855f7'}`,
                                    borderRadius: '12px',
                                    padding: '1.25rem',
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    transition: 'var(--transition)'
                                }}
                                onMouseOver={(e) => {
                                    e.currentTarget.style.transform = 'translateX(5px)';
                                    e.currentTarget.style.background = 'rgba(255,255,255,0.05)';
                                }}
                                onMouseOut={(e) => {
                                    e.currentTarget.style.transform = 'translateX(0)';
                                    e.currentTarget.style.background = 'linear-gradient(90deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.01) 100%)';
                                }}>
                                    <div>
                                        <h3 style={{ margin: '0 0 0.4rem 0', fontSize: '1.2rem', color: '#fff' }}>{meeting.title}</h3>
                                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#00f2fe" strokeWidth="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                                            {new Date(meeting.scheduledTime).toLocaleString('en-US', {
                                                weekday: 'long', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit'
                                            })}
                                        </div>
                                    </div>
                                    <div style={{ 
                                        fontSize: '0.8rem', 
                                        fontWeight: '600',
                                        textTransform: 'uppercase',
                                        letterSpacing: '0.05em',
                                        color: meeting.reminderSent ? '#10b981' : '#a855f7', 
                                        background: meeting.reminderSent ? 'rgba(16, 185, 129, 0.1)' : 'rgba(168, 85, 247, 0.15)', 
                                        padding: '0.4rem 0.8rem', 
                                        borderRadius: '30px',
                                        boxShadow: meeting.reminderSent ? 'none' : '0 0 10px rgba(168, 85, 247, 0.2)'
                                    }}>
                                        {meeting.reminderSent ? 'Sent' : 'Pending'}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
