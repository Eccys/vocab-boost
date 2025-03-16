import React from 'react';
import '../styles/StatsScreen.css';

interface StatCardProps {
  icon: string;
  value: string;
  label: string;
}

const StatCard: React.FC<StatCardProps> = ({ icon, value, label }) => {
  return (
    <div className="stat-card">
      <div className="stat-icon">{icon}</div>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
};

const StatsScreen: React.FC = () => {
  // Calculate progress arc parameters
  const radius = 70;
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
            icon="⚡"
            value="0"
            label="Words Studied"
          />
          <StatCard
            icon="⏱️"
            value="18m"
            label="Time Spent"
          />
          <StatCard
            icon="🏆"
            value="1d"
            label="Best Streak"
          />
          <StatCard
            icon="✅"
            value="5"
            label="Mastered"
          />
          <StatCard
            icon="📊"
            value="10"
            label="Today"
          />
          <StatCard
            icon="📚"
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