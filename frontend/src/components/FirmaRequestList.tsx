import { useEffect } from 'react';
import { useFirmaStore } from '../store/firmaStore';
import { FirmaResponse, FirmaStatus } from '../types/firma';

export default function FirmaRequestList() {
  const { requests, loading, error, fetchAllRequests, downloadDocument, setCurrentRequest } = useFirmaStore();

  useEffect(() => {
    fetchAllRequests();
  }, []);

  const getStatusColor = (status: FirmaStatus): string => {
    switch (status) {
      case FirmaStatus.FINISHED:
        return 'status-success';
      case FirmaStatus.CREATED:
      case FirmaStatus.STARTED:
        return 'status-pending';
      case FirmaStatus.REFUSED:
      case FirmaStatus.EXPIRED:
        return 'status-warning';
      case FirmaStatus.ERROR:
      case FirmaStatus.REQUEST_FAILED:
      case FirmaStatus.FILE_VALIDATION_FAILED:
        return 'status-error';
      default:
        return '';
    }
  };

  const handleDownload = (request: FirmaResponse) => {
    if (request.status === FirmaStatus.FINISHED) {
      downloadDocument(request.id, request.filename);
    }
  };

  const handleRefresh = () => {
    fetchAllRequests();
  };

  if (loading && requests.length === 0) {
    return <div className="loading">Caricamento richieste...</div>;
  }

  return (
    <div className="firma-list">
      <div className="list-header">
        <h2>Richieste di Firma</h2>
        <button onClick={handleRefresh} className="btn-refresh" disabled={loading}>
          {loading ? 'Aggiornamento...' : '↻ Aggiorna'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {requests.length === 0 ? (
        <div className="empty-state">
          <p>Nessuna richiesta di firma presente.</p>
          <p>Crea la tua prima richiesta!</p>
        </div>
      ) : (
        <div className="requests-grid">
          {requests.map((request) => (
            <div key={request.id} className="request-card">
              <div className="request-header">
                <h3>{request.title || request.filename}</h3>
                <span className={`status-badge ${getStatusColor(request.status)}`}>
                  {request.status}
                </span>
              </div>

              {request.description && (
                <p className="request-description">{request.description}</p>
              )}

              <div className="request-info">
                <div className="info-row">
                  <span className="info-label">ID:</span>
                  <span className="info-value">{request.id}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">File:</span>
                  <span className="info-value">{request.filename}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">Firmatari:</span>
                  <span className="info-value">{request.members.length}</span>
                </div>
              </div>

              <div className="members-list">
                <h4>Firmatari:</h4>
                {request.members.map((member, index) => (
                  <div key={index} className="member-item">
                    <div className="member-name">
                      {member.firstname} {member.lastname}
                    </div>
                    <div className="member-details">
                      <span>{member.email}</span>
                      <span className={`member-status ${member.status}`}>
                        {member.status}
                      </span>
                    </div>
                    {member.signLink && (
                      <a
                        href={member.signLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="sign-link"
                      >
                        Link di firma →
                      </a>
                    )}
                  </div>
                ))}
              </div>

              <div className="request-actions">
                <button
                  onClick={() => setCurrentRequest(request)}
                  className="btn-secondary"
                >
                  Dettagli
                </button>
                {request.status === FirmaStatus.FINISHED && (
                  <button
                    onClick={() => handleDownload(request)}
                    className="btn-primary"
                  >
                    Scarica PDF Firmato
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
