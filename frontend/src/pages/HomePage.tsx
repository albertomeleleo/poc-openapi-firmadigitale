import { useState } from 'react';
import FirmaRequestForm from '../components/FirmaRequestForm';
import FirmaRequestList from '../components/FirmaRequestList';

export default function HomePage() {
  const [activeTab, setActiveTab] = useState<'create' | 'list'>('create');

  return (
    <div className="home-page">
      <header className="app-header">
        <h1>Firma Digitale OpenAPI</h1>
        <p>Gestione richieste di firma elettronica</p>
      </header>

      <div className="tabs">
        <button
          className={`tab ${activeTab === 'create' ? 'active' : ''}`}
          onClick={() => setActiveTab('create')}
        >
          Nuova Richiesta
        </button>
        <button
          className={`tab ${activeTab === 'list' ? 'active' : ''}`}
          onClick={() => setActiveTab('list')}
        >
          Le Mie Richieste
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'create' ? <FirmaRequestForm /> : <FirmaRequestList />}
      </div>
    </div>
  );
}
