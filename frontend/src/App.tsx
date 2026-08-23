import { Routes, Route, Link } from 'react-router-dom';
import { Dashboard } from './components/Dashboard';
import { UploadMeeting } from './components/UploadMeeting';
import { MeetingDetails } from './components/MeetingDetails';
import { Calendar } from './components/Calendar';

function App() {
  return (
    <>
      <nav className="navbar">
        <Link to="/" className="nav-brand">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#00f2fe' }}>
            <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
            <polyline points="2 17 12 22 22 17"></polyline>
            <polyline points="2 12 12 17 22 12"></polyline>
          </svg>
          Meeting Charcha
        </Link>
        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
            <Link to="/upload" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontWeight: 500, transition: 'var(--transition)' }} onMouseOver={(e) => e.currentTarget.style.color = '#fff'} onMouseOut={(e) => e.currentTarget.style.color = 'var(--text-secondary)'}>Upload</Link>
            <Link to="/calendar" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontWeight: 500, transition: 'var(--transition)' }} onMouseOver={(e) => e.currentTarget.style.color = '#fff'} onMouseOut={(e) => e.currentTarget.style.color = 'var(--text-secondary)'}>Calendar</Link>
        </div>
      </nav>
      
      <div className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/upload" element={<UploadMeeting />} />
          <Route path="/meeting/:id" element={<MeetingDetails />} />
          <Route path="/calendar" element={<Calendar />} />
        </Routes>
      </div>
    </>
  );
}

export default App;
