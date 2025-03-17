import React from 'react';
import '../styles/StatsScreen.css';

interface StatCardProps {
  icon?: React.ReactNode;
  value: string;
  label: string;
}

const StatCard: React.FC<StatCardProps> = ({ icon, value, label }) => {
  return (
    <div className="stat-card">
      {icon && <div className="stat-icon">{icon}</div>}
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
};

const StatsScreen: React.FC = () => {
  // Calculate progress arc parameters
  const radius = 80;
  const circumference = 2 * Math.PI * radius;
  const progress = 10 / 50; // 10 out of 50 completed
  const strokeDashoffset = circumference * (1 - progress);
  
  return (
    <div className="stats-screen">
      <div className="stats-content">
        {/* Progress Circle */}
        <div className="progress-circle-container">
          <svg className="progress-circle" viewBox="0 0 200 200">
            <circle 
              cx="100" 
              cy="100" 
              r={radius} 
              fill="transparent" 
              stroke="#333" 
              strokeWidth="12"
            />
            <circle 
              cx="100" 
              cy="100" 
              r={radius} 
              fill="transparent" 
              stroke="#75b8ff" 
              strokeWidth="12"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              transform="rotate(-90 100 100)"
              strokeLinecap="round"
            />
          </svg>
          <div className="progress-text">
            <div className="progress-value">10/50</div>
            <div className="progress-label">Daily Goal</div>
          </div>
        </div>
        
        {/* Stats Grid */}
        <div className="stats-grid">
          <StatCard
            icon={
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path>
              </svg>
            }
            value="45"
            label="Words Studied"
          />
          <StatCard
            icon={
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
            }
            value="18m"
            label="Time Spent"
          />
          <StatCard
            icon={
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFD700" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M8 21l8 0"></path>
                <path d="M12 17l0 4"></path>
                <path d="M12 3c-1.3 0-2.4.84-2.82 2L9 5.5H7a3 3 0 0 0 0 6h10a3 3 0 0 0 0-6h-2l-.18-.5C14.4 3.84 13.3 3 12 3z"></path>
              </svg>
            }
            value="7d"
            label="Best Streak"
          />
          <StatCard
            value="5"
            label="Mastered"
          />
          <StatCard
            value="10"
            label="Today"
          />
          <StatCard
            value="40"
            label="To Review"
          />
        </div>
        
        {/* Reset Button */}
        <button className="reset-button">
          Reset Learning Progress
        </button>
      </div>
    </div>
  );
};

export default StatsScreen; 