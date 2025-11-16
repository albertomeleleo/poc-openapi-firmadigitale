import { useState } from 'react';
import { FirmaRequest, Member, SignPosition } from '../types/firma';
import { useFirmaStore } from '../store/firmaStore';
import { firmaApi } from '../services/api';

export default function FirmaRequestForm() {
  const { createRequest, loading, error } = useFirmaStore();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [filename, setFilename] = useState('');
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [members, setMembers] = useState<Member[]>([{
    firstname: '',
    lastname: '',
    email: '',
    phone: '+39',
    signs: [{ page: 1, position: '' }]
  }]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && file.type === 'application/pdf') {
      setPdfFile(file);
      if (!filename) {
        setFilename(file.name);
      }
    } else {
      alert('Please select a PDF file');
    }
  };

  const addMember = () => {
    setMembers([...members, {
      firstname: '',
      lastname: '',
      email: '',
      phone: '+39',
      signs: [{ page: 1, position: '' }]
    }]);
  };

  const removeMember = (index: number) => {
    setMembers(members.filter((_, i) => i !== index));
  };

  const updateMember = (index: number, field: keyof Member, value: any) => {
    const updatedMembers = [...members];
    updatedMembers[index] = { ...updatedMembers[index], [field]: value };
    setMembers(updatedMembers);
  };

  const addSignPosition = (memberIndex: number) => {
    const updatedMembers = [...members];
    updatedMembers[memberIndex].signs.push({ page: 1, position: '' });
    setMembers(updatedMembers);
  };

  const removeSignPosition = (memberIndex: number, signIndex: number) => {
    const updatedMembers = [...members];
    updatedMembers[memberIndex].signs = updatedMembers[memberIndex].signs.filter((_, i) => i !== signIndex);
    setMembers(updatedMembers);
  };

  const updateSignPosition = (memberIndex: number, signIndex: number, field: keyof SignPosition, value: any) => {
    const updatedMembers = [...members];
    updatedMembers[memberIndex].signs[signIndex] = {
      ...updatedMembers[memberIndex].signs[signIndex],
      [field]: value
    };
    setMembers(updatedMembers);
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
        title,
        description,
        filename: filename || `firma_${Date.now()}.pdf`,
        content: base64Content,
        members: members.map(m => ({
          ...m,
          signs: m.signs.filter(s => s.page > 0) // Filter out invalid positions
        }))
      };

      const result = await createRequest(request);

      if (result) {
        alert('Signature request created successfully!');
        // Reset form
        setTitle('');
        setDescription('');
        setFilename('');
        setPdfFile(null);
        setMembers([{
          firstname: '',
          lastname: '',
          email: '',
          phone: '+39',
          signs: [{ page: 1, position: '' }]
        }]);
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
          <label>Titolo (opzionale)</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Titolo della richiesta"
          />
        </div>

        <div className="form-group">
          <label>Descrizione (opzionale)</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Descrizione della richiesta"
            rows={3}
          />
        </div>

        <div className="form-group">
          <label>Nome File</label>
          <input
            type="text"
            value={filename}
            onChange={(e) => setFilename(e.target.value)}
            placeholder="documento.pdf"
          />
        </div>

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
          <h3>Firmatari</h3>
          {members.map((member, memberIndex) => (
            <div key={memberIndex} className="member-card">
              <div className="member-header">
                <h4>Firmatario {memberIndex + 1}</h4>
                {members.length > 1 && (
                  <button type="button" onClick={() => removeMember(memberIndex)} className="btn-remove">
                    Rimuovi
                  </button>
                )}
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Nome *</label>
                  <input
                    type="text"
                    value={member.firstname}
                    onChange={(e) => updateMember(memberIndex, 'firstname', e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Cognome *</label>
                  <input
                    type="text"
                    value={member.lastname}
                    onChange={(e) => updateMember(memberIndex, 'lastname', e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Email *</label>
                  <input
                    type="email"
                    value={member.email}
                    onChange={(e) => updateMember(memberIndex, 'email', e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Telefono (con prefisso +39) *</label>
                  <input
                    type="tel"
                    value={member.phone}
                    onChange={(e) => updateMember(memberIndex, 'phone', e.target.value)}
                    placeholder="+39123456789"
                    required
                  />
                </div>
              </div>

              <div className="sign-positions">
                <h5>Posizioni Firma</h5>
                {member.signs.map((sign, signIndex) => (
                  <div key={signIndex} className="sign-position-row">
                    <div className="form-group">
                      <label>Pagina</label>
                      <input
                        type="number"
                        min="1"
                        value={sign.page}
                        onChange={(e) => updateSignPosition(memberIndex, signIndex, 'page', parseInt(e.target.value))}
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label>Posizione (x1,y1,x2,y2) - opzionale</label>
                      <input
                        type="text"
                        value={sign.position || ''}
                        onChange={(e) => updateSignPosition(memberIndex, signIndex, 'position', e.target.value)}
                        placeholder="10,15,45,35"
                      />
                    </div>

                    {member.signs.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeSignPosition(memberIndex, signIndex)}
                        className="btn-remove-small"
                      >
                        ✕
                      </button>
                    )}
                  </div>
                ))}

                <button
                  type="button"
                  onClick={() => addSignPosition(memberIndex)}
                  className="btn-add-small"
                >
                  + Aggiungi Posizione Firma
                </button>
              </div>
            </div>
          ))}

          <button type="button" onClick={addMember} className="btn-add">
            + Aggiungi Firmatario
          </button>
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
