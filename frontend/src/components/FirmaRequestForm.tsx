import { useState } from 'react';
import { FirmaRequest, Member, SignPosition } from '../types/firma';
import { useFirmaStore } from '../store/firmaStore';
import { firmaApi } from '../services/api';

export default function FirmaRequestForm() {
  const { createRequest, loading, error } = useFirmaStore();

  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [firstname, setFirstname] = useState('');
  const [lastname, setLastname] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('+39');
  const [page, setPage] = useState(1);
  const [position, setPosition] = useState('');

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && file.type === 'application/pdf') {
      setPdfFile(file);
    } else {
      alert('Please select a PDF file');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!pdfFile) {
      alert('Please select a PDF file');
      return;
    }

    try {
      const base64Content = await firmaApi.fileToBase64(pdfFile);

      const request: FirmaRequest = {
        filename: `firma_${Date.now()}.pdf`,
        content: base64Content,
        members: [{
          firstname,
          lastname,
          email,
          phone,
          signs: [{ page, position: position || undefined }]
        }]
      };

      const result = await createRequest(request);

      if (result) {
        alert('Signature request created successfully!');
        // Reset form
        setPdfFile(null);
        setFirstname('');
        setLastname('');
        setEmail('');
        setPhone('+39');
        setPage(1);
        setPosition('');
      }
    } catch (err) {
      console.error('Error creating request:', err);
    }
  };

  return (
    <div className="firma-form">
      <h2>Crea Richiesta di Firma</h2>

      {error && <div className="error-message">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Documento PDF *</label>
          <input
            type="file"
            accept="application/pdf"
            onChange={handleFileChange}
            required
          />
          {pdfFile && <p className="file-info">File selezionato: {pdfFile.name}</p>}
        </div>

        <div className="members-section">
          <h3>Firmatario</h3>
          <div className="member-card">
            <div className="form-row">
              <div className="form-group">
                <label>Nome *</label>
                <input
                  type="text"
                  value={firstname}
                  onChange={(e) => setFirstname(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Cognome *</label>
                <input
                  type="text"
                  value={lastname}
                  onChange={(e) => setLastname(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Email *</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Telefono (con prefisso +39) *</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+39123456789"
                  required
                />
              </div>
            </div>

            <div className="sign-positions">
              <h5>Posizione Firma</h5>
              <div className="sign-position-row">
                <div className="form-group">
                  <label>Pagina *</label>
                  <input
                    type="number"
                    min="1"
                    value={page}
                    onChange={(e) => setPage(parseInt(e.target.value))}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Posizione (x1,y1,x2,y2) - opzionale</label>
                  <input
                    type="text"
                    value={position}
                    onChange={(e) => setPosition(e.target.value)}
                    placeholder="10,15,45,35"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Creazione in corso...' : 'Crea Richiesta di Firma'}
          </button>
        </div>
      </form>
    </div>
  );
}
