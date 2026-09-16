'use client';

import { useState, useEffect, Suspense } from 'react';
import { motion } from 'framer-motion';
import { QRCodeSVG } from 'qrcode.react';
import { CheckCircle, ShieldCheck, QrCode, ArrowLeft, Target, FileText, CheckSquare, Square } from 'lucide-react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { api } from '@/lib/api';

interface BackendField {
  field: string;
  name: string;
  description: string;
}

const DEFAULT_FIELDS = [
  { id: 'FULL_NAME', label: 'Full Name' },
  { id: 'DATE_OF_BIRTH', label: 'Date of Birth' },
  { id: 'ADDRESS', label: 'Residential Address' },
  { id: 'IDENTITY_PROOF', label: 'Identity Proof' },
  { id: 'AGE_VERIFICATION', label: 'Age Verification (18+)' },
  { id: 'DOCUMENT_VALIDITY', label: 'Document Validity' },
  { id: 'GENDER', label: 'Gender' },
  { id: 'FATHER_NAME', label: 'Father Name' },
  { id: 'DOCUMENT_NUMBER_MATCH', label: 'Doc Number Match' },
  { id: 'ADDRESS_PROOF', label: 'Address Proof' },
];

const AVAILABLE_DOCS = [
  { id: 'AADHAAR_CARD', label: 'Aadhaar Card' },
  { id: 'PAN_CARD', label: 'PAN Card' },
  { id: 'PASSPORT', label: 'Passport' },
  { id: 'DRIVING_LICENSE', label: 'Driving License' },
  { id: 'VOTER_ID', label: 'Voter ID' },
  { id: 'EMPLOYMENT_ID', label: 'Employment ID' },
];

function KYCPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [step, setStep] = useState(1);
  const [availableFields, setAvailableFields] = useState(DEFAULT_FIELDS);
  const [formData, setFormData] = useState({
    targetUserId: '',
    purpose: 'Identity Verification for Secure Access',
    selectedFields: ['FULL_NAME', 'IDENTITY_PROOF'] as string[],
    selectedDocs: ['AADHAAR_CARD'] as string[],
  });
  const [contractId, setContractId] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [error, setError] = useState('');
  const [isGeneralAccess, setIsGeneralAccess] = useState(true);
  const [createdContractTargetUserId, setCreatedContractTargetUserId] = useState<string | null>(null);
  const normalizedTargetUserId = formData.targetUserId.trim().toLowerCase();
  const isCreatedContractGeneral = createdContractTargetUserId === null;

  // Sync with backend fields on mount
  useEffect(() => {
    const fetchFields = async () => {
      try {
        const response = await api.info.getVerificationFields();
        const fields = (response.data || response) as BackendField[];
        if (Array.isArray(fields)) {
          setAvailableFields(fields.map(f => ({
            id: f.field,
            label: f.name
          })));
        }
      } catch (err) {
        console.error('Failed to fetch backend verification fields:', err);
      }
    };
    fetchFields();
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    const finalValue = name === 'targetUserId' ? value.toLowerCase() : value;
    setFormData(prev => ({ ...prev, [name]: finalValue }));
  };

  const toggleField = (fieldId: string) => {
    setFormData(prev => {
      const fields = prev.selectedFields.includes(fieldId)
        ? prev.selectedFields.filter(f => f !== fieldId)
        : [...prev.selectedFields, fieldId];
      return { ...prev, selectedFields: fields };
    });
  };

  const toggleDoc = (docId: string) => {
    setFormData(prev => {
      const docs = prev.selectedDocs.includes(docId)
        ? prev.selectedDocs.filter(d => d !== docId)
        : [...prev.selectedDocs, docId];
      return { ...prev, selectedDocs: docs };
    });
  };

  const createContract = async () => {
    setIsVerifying(true);
    setError('');
    try {
      if (!isGeneralAccess && !normalizedTargetUserId) {
        throw new Error('Specific buyer ID is required.');
      }

      const response = await api.pipeline.createContract({
        userId: isGeneralAccess ? null : normalizedTargetUserId,
        requiredDocumentTypes: formData.selectedDocs,
        requiredFields: formData.selectedFields,
        purpose: formData.purpose,
      });
      
      const cid = (response.data?.contractId || response.contractId) as string;
      if (!cid) throw new Error('Contract created but no ID received.');
      
      setContractId(cid);
      setCreatedContractTargetUserId(isGeneralAccess ? null : normalizedTargetUserId);
      setStep(3);
    } catch (err: unknown) {
      const error = err as Error & { status?: number };
      console.error('Failed to create verification contract:', error);
      if (typeof window !== 'undefined' && (error.status === 401 || error.status === 403 || !localStorage.getItem('legit_token'))) {
        localStorage.removeItem('legit_token');
        localStorage.removeItem('legit_user');
        window.location.href = '/login';
      }
      setError(error.message || 'Failed to create verification contract.');
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 pt-32 pb-12 px-4">
      <div className="max-w-2xl mx-auto">
        <Link href="/dashboard" className="inline-flex items-center gap-2 text-slate-500 hover:text-rose-600 font-bold mb-8 transition-colors uppercase tracking-widest text-xs">
          <ArrowLeft size={16} /> Back to Command Center
        </Link>

        <div className="bg-white border-t-8 border-rose-600 shadow-2xl p-8 md:p-12 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-4 opacity-5">
            {isGeneralAccess ? <QrCode size={120} /> : <ShieldCheck size={120} />}
          </div>

          <div className="mb-12 text-black">
            <h1 className="text-4xl font-black uppercase italic tracking-tighter mb-2">Contract <span className="text-rose-600">Configuration</span></h1>
            <p className="text-slate-500 font-medium italic">Configure secure verification parameters for showroom access.</p>
          </div>

          {/* Progress Bar */}
          <div className="flex items-center gap-4 mb-12">
            {[1, 2, 3].map((s) => (
              <div key={s} className="flex-1 h-2 bg-slate-100 relative">
                <motion.div 
                  initial={{ width: 0 }}
                  animate={{ width: step >= s ? '100%' : '0%' }}
                  className="absolute inset-0 bg-rose-600"
                />
              </div>
            ))}
          </div>

          {error && (
            <div className="bg-red-50 border-l-4 border-rose-600 p-4 mb-8">
              <p className="text-rose-600 text-xs font-bold uppercase tracking-widest">{error}</p>
            </div>
          )}

          {step === 1 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="space-y-8"
            >
              <div className="grid grid-cols-1 gap-8">
                <div className="flex items-center gap-4 mb-4">
                  <button 
                    onClick={() => setIsGeneralAccess(true)}
                    className={`flex-1 py-3 font-black text-xs uppercase tracking-widest border-2 transition-colors ${isGeneralAccess ? 'bg-black text-white border-black' : 'bg-white text-black border-slate-200'}`}
                  >
                    General Access (Any Buyer)
                  </button>
                  <button 
                    onClick={() => setIsGeneralAccess(false)}
                    className={`flex-1 py-3 font-black text-xs uppercase tracking-widest border-2 transition-colors ${!isGeneralAccess ? 'bg-black text-white border-black' : 'bg-white text-black border-slate-200'}`}
                  >
                    Specific Buyer ID
                  </button>
                </div>

                {!isGeneralAccess && (
                  <div>
                    <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-3">Target Buyer (Legit ID or User ID)</label>
                    <div className="relative">
                      <Target className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-300" size={20} />
                      <input
                        type="text"
                        name="targetUserId"
                        value={formData.targetUserId}
                        onChange={handleInputChange}
                        className="w-full bg-slate-50 border-2 border-slate-100 px-12 py-4 focus:border-rose-600 outline-none transition-colors font-bold text-lg normal-case text-black"
                        placeholder="enter buyer legit id (e.g. john@legit)"
                      />
                    </div>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-3">Verification Purpose</label>
                  <div className="relative">
                    <FileText className="absolute left-4 top-4 text-slate-300" size={20} />
                    <textarea
                      name="purpose"
                      value={formData.purpose}
                      onChange={handleInputChange}
                      className="w-full bg-slate-50 border-2 border-slate-100 px-12 py-4 focus:border-rose-600 outline-none transition-colors font-bold text-lg min-h-[100px] text-black"
                      placeholder="PURPOSE OF VERIFICATION"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-4">Required Documents</label>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
                    {AVAILABLE_DOCS.map((doc) => (
                      <button
                        key={doc.id}
                        onClick={() => toggleDoc(doc.id)}
                        className={`flex items-center gap-3 p-4 border-2 transition-all ${formData.selectedDocs.includes(doc.id) ? 'border-rose-600 bg-red-50 text-rose-600' : 'border-slate-100 bg-slate-50 text-slate-400'}`}
                      >
                        {formData.selectedDocs.includes(doc.id) ? <CheckSquare size={20} /> : <Square size={20} />}
                        <span className="font-black uppercase text-[10px] tracking-widest">{doc.label}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-4">Required Verification Fields</label>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {availableFields.map((field) => (
                      <button
                        key={field.id}
                        onClick={() => toggleField(field.id)}
                        className={`flex items-center gap-3 p-4 border-2 transition-all ${formData.selectedFields.includes(field.id) ? 'border-rose-600 bg-red-50 text-rose-600' : 'border-slate-100 bg-slate-50 text-slate-400'}`}
                      >
                        {formData.selectedFields.includes(field.id) ? <CheckSquare size={20} /> : <Square size={20} />}
                        <span className="font-black uppercase text-[10px] tracking-widest">{field.label}</span>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
              
              <button
                onClick={() => setStep(2)}
                disabled={(!isGeneralAccess && !formData.targetUserId) || !formData.purpose || formData.selectedFields.length === 0 || formData.selectedDocs.length === 0}
                className="w-full bg-black text-white py-5 font-black uppercase tracking-widest hover:bg-rose-600 transition-colors disabled:opacity-30 disabled:hover:bg-black skew-x-[-12deg]"
              >
                <span className="skew-x-[12deg]">NEXT: REVIEW CONTRACT</span>
              </button>
            </motion.div>
          )}

          {step === 2 && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="text-center text-black"
            >
              <div className="bg-slate-50 p-8 border-2 border-dashed border-slate-200 mb-8 text-left">
                <ShieldCheck className="mx-auto text-rose-600 mb-4" size={64} />
                <h3 className="text-2xl font-black uppercase italic mb-6 text-center">Contract Summary</h3>
                
                <div className="space-y-4">
                  <div className="flex justify-between border-b border-slate-200 pb-2">
                    <span className="text-[10px] font-black text-slate-400 uppercase">Target Buyer</span>
                    <span className="text-sm font-black lowercase italic">{isGeneralAccess ? 'ANY BUYER (GENERAL)' : normalizedTargetUserId}</span>
                  </div>
                  <div className="flex justify-between border-b border-slate-200 pb-2">
                    <span className="text-[10px] font-black text-slate-400 uppercase">Purpose</span>
                    <span className="text-[10px] font-black uppercase text-right max-w-[200px]">{formData.purpose}</span>
                  </div>
                  <div className="flex justify-between border-b border-slate-200 pb-2">
                    <span className="text-[10px] font-black text-slate-400 uppercase">Required Docs</span>
                    <div className="flex flex-wrap gap-1 justify-end max-w-[200px]">
                      {formData.selectedDocs.map(docId => (
                        <span key={docId} className="bg-slate-800 text-white text-[7px] font-black px-1.5 py-0.5 uppercase">
                          {docId.replace('_', ' ')}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div>
                    <span className="text-[10px] font-black text-slate-400 uppercase block mb-2">Requested Fields</span>
                    <div className="flex flex-wrap gap-2">
                      {formData.selectedFields.map(fid => (
                        <span key={fid} className="bg-rose-600 text-white text-[8px] font-black px-2 py-1 uppercase tracking-tighter">
                          {fid.replace('_', ' ')}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
              
              <button
                onClick={createContract}
                disabled={isVerifying}
                className="w-full bg-rose-600 text-white py-6 font-black uppercase tracking-widest hover:bg-red-700 transition-colors skew-x-[-12deg] shadow-xl shadow-rose-600/30"
              >
                <span className="skew-x-[12deg] flex items-center justify-center gap-3">
                  {isVerifying ? (
                    <div className="w-6 h-6 border-4 border-white border-t-transparent rounded-full animate-spin" />
                  ) : 'DEPLOY CONTRACT TO SHOWROOM'}
                </span>
              </button>
              
              <button 
                onClick={() => setStep(1)}
                className="mt-6 text-xs font-black uppercase tracking-[0.2em] text-slate-400 hover:text-black transition-colors"
              >
                ADJUST CONFIGURATION
              </button>
            </motion.div>
          )}

          {step === 3 && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="text-center text-black"
            >
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white font-black italic uppercase text-xs mb-8 skew-x-[-12deg]">
                <span className="skew-x-[12deg] flex items-center gap-2"><CheckCircle size={14} /> CONTRACT DEPLOYED</span>
              </div>
              
              <h3 className="text-3xl font-black uppercase italic mb-8">
                {isCreatedContractGeneral && createdContractTargetUserId === null ? <>Showroom <span className="text-rose-600">Access Key</span></> : <>Targeted <span className="text-rose-600">Contract Ready</span></>}
              </h3>

              {isCreatedContractGeneral && createdContractTargetUserId === null ? (
                <>
                  <div className="bg-white p-6 border-4 border-black inline-block mb-10 shadow-2xl relative">
                    <div className="absolute top-0 left-0 w-8 h-8 border-t-4 border-l-4 border-rose-600 -translate-x-2 -translate-y-2" />
                    <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-rose-600 translate-x-2 translate-y-2" />
                    <QRCodeSVG
                      value={`${searchParams.get('origin') || (typeof window !== 'undefined' ? window.location.origin : 'https://legit-pipeline.com')}/verify/${contractId}`}
                      size={240}
                      level="H"
                      fgColor="#000000"
                      includeMargin={true}
                    />
                  </div>

                  <p className="text-slate-500 text-sm max-w-sm mx-auto font-medium mb-10 italic">
                    Scan this QR code with the Legit Mobile App to initiate buyer verification.
                  </p>
                </>
              ) : (
                <div className="max-w-lg mx-auto mb-10 border-4 border-black bg-slate-50 p-8 text-left shadow-2xl">
                  <p className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-3">Assigned Buyer</p>
                  <p className="text-xl font-black lowercase text-black break-all">{createdContractTargetUserId}</p>
                  <p className="mt-4 text-sm font-medium italic text-slate-500">
                    This contract is locked to the selected Legit ID. The buyer will receive an automated notification to initiate verification within their Legit mobile app.
                  </p>
                </div>
              )}
              
              <div className="flex flex-col gap-4">
                <Link href="/dashboard" className="w-full bg-black text-white py-5 font-black uppercase tracking-widest hover:bg-rose-600 transition-colors skew-x-[-12deg]">
                  <span className="skew-x-[12deg]">MONITOR TELEMETRY</span>
                </Link>
              </div>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function KYCPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-slate-50 pt-32" />}>
      <KYCPageContent />
    </Suspense>
  );
}
