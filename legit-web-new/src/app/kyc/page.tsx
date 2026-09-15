'use client';

import { useState, useEffect, Suspense } from 'react';
import { motion } from 'framer-motion';
import { QRCodeSVG } from 'qrcode.react';
import { CheckCircle, ShieldCheck, QrCode, ArrowLeft, Target, FileText, CheckSquare, Square, Building2, Lock } from 'lucide-react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { api } from '@/lib/api';

interface BackendField {
  field: string;
  name: string;
  description: string;
}

const DEFAULT_FIELDS = [
  { id: 'FULL_NAME', label: 'Full Legal Name' },
  { id: 'DATE_OF_BIRTH', label: 'Date of Birth' },
  { id: 'ADDRESS', label: 'Residential Address Verification' },
  { id: 'IDENTITY_PROOF', label: 'Statutory Identity Proof' },
  { id: 'AGE_VERIFICATION', label: 'Age Attestation (18+)' },
  { id: 'DOCUMENT_VALIDITY', label: 'Document Validity & Non-Expiry' },
  { id: 'GENDER', label: 'Gender Record' },
  { id: 'FATHER_NAME', label: "Parent / Father's Name" },
  { id: 'DOCUMENT_NUMBER_MATCH', label: 'Document Registration Match' },
  { id: 'ADDRESS_PROOF', label: 'Address Proof Record' },
];

const AVAILABLE_DOCS = [
  { id: 'AADHAAR_CARD', label: 'Aadhaar (UIDAI)' },
  { id: 'PAN_CARD', label: 'PAN Card (ITD)' },
  { id: 'PASSPORT', label: 'Indian Passport (MEA)' },
  { id: 'DRIVING_LICENSE', label: 'Driving License (MoRTH)' },
  { id: 'VOTER_ID', label: 'Elector Photo ID (ECI)' },
  { id: 'FIR', label: 'Police FIR Dossier' },
];

function VerificationRequestContent() {
  const searchParams = useSearchParams();
  const [step, setStep] = useState(1);
  const [availableFields, setAvailableFields] = useState(DEFAULT_FIELDS);
  const [formData, setFormData] = useState({
    targetUserId: '',
    purpose: 'Official Investigation & Identity Verification',
    selectedFields: ['FULL_NAME', 'IDENTITY_PROOF', 'DATE_OF_BIRTH'] as string[],
    selectedDocs: ['AADHAAR_CARD', 'PAN_CARD'] as string[],
  });
  const [contractId, setContractId] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [error, setError] = useState('');
  const [isGeneralAccess, setIsGeneralAccess] = useState(true);
  const [createdContractTargetUserId, setCreatedContractTargetUserId] = useState<string | null>(null);
  const normalizedTargetUserId = formData.targetUserId.trim().toLowerCase();
  const isCreatedContractGeneral = createdContractTargetUserId === null;

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
        console.error('Failed to fetch verification fields:', err);
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
        throw new Error('Specific citizen / officer Dedox ID is required.');
      }

      const response = await api.pipeline.createContract({
        userId: isGeneralAccess ? null : normalizedTargetUserId,
        requiredDocumentTypes: formData.selectedDocs,
        requiredFields: formData.selectedFields,
        purpose: formData.purpose,
      });
      
      const cid = (response.data?.contractId || response.contractId) as string;
      if (!cid) throw new Error('Verification request initiated but no contract ID received.');
      
      setContractId(cid);
      setCreatedContractTargetUserId(isGeneralAccess ? null : normalizedTargetUserId);
      setStep(3);
    } catch (err: unknown) {
      const error = err as Error;
      setError(error.message || 'Failed to dispatch verification contract.');
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 pt-32 pb-16 px-4">
      <div className="max-w-3xl mx-auto">
        <Link 
          href="/dashboard" 
          className="inline-flex items-center gap-2 text-slate-400 hover:text-amber-400 font-bold mb-8 transition-colors uppercase tracking-widest text-xs"
        >
          <ArrowLeft size={16} /> Return to Command Dashboard
        </Link>

        <div className="bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl p-8 md:p-12 relative overflow-hidden">
          <div className="mb-8">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded bg-amber-500/10 text-amber-400 border border-amber-500/30 text-xs font-mono mb-3">
              <Building2 size={14} /> Official Statutory Verification Dispatch
            </div>
            <h1 className="text-3xl sm:text-4xl font-black uppercase text-white">
              Issue <span className="text-amber-400">Verification Request</span>
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Configure parameters for zero-knowledge biometric and document verification via mobile app.
            </p>
          </div>

          {/* Progress Indicator */}
          <div className="flex items-center gap-3 mb-10">
            {[1, 2, 3].map((s) => (
              <div key={s} className="flex-1 h-1.5 bg-slate-800 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-500 ${step >= s ? 'bg-amber-500' : 'bg-transparent'}`} 
                />
              </div>
            ))}
          </div>

          {error && (
            <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-4 rounded-xl mb-8 text-xs font-mono">
              {error}
            </div>
          )}

          {step === 1 && (
            <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
              {/* Access Mode Selector */}
              <div className="flex items-center gap-3 p-1 bg-slate-950 rounded-xl border border-slate-800">
                <button 
                  onClick={() => setIsGeneralAccess(true)}
                  className={`flex-1 py-3 font-bold text-xs uppercase tracking-wider rounded-lg transition-all ${isGeneralAccess ? 'bg-amber-500 text-slate-950 shadow-md' : 'text-slate-400 hover:text-white'}`}
                >
                  General Access (Any QR Scan)
                </button>
                <button 
                  onClick={() => setIsGeneralAccess(false)}
                  className={`flex-1 py-3 font-bold text-xs uppercase tracking-wider rounded-lg transition-all ${!isGeneralAccess ? 'bg-amber-500 text-slate-950 shadow-md' : 'text-slate-400 hover:text-white'}`}
                >
                  Specific Dedox ID / Officer
                </button>
              </div>

              {!isGeneralAccess && (
                <div>
                  <label className="block text-xs font-mono uppercase text-slate-400 mb-2">Target Citizen / Officer Dedox ID</label>
                  <div className="relative">
                    <Target className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                    <input
                      type="text"
                      name="targetUserId"
                      value={formData.targetUserId}
                      onChange={handleInputChange}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-12 pr-4 py-3.5 focus:border-amber-500 outline-none text-sm text-white font-mono"
                      placeholder="e.g. officer.sharma@dedox or user@dedox"
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-mono uppercase text-slate-400 mb-2">Verification Purpose / Legal Mandate</label>
                <div className="relative">
                  <FileText className="absolute left-4 top-4 text-slate-500" size={18} />
                  <textarea
                    name="purpose"
                    value={formData.purpose}
                    onChange={handleInputChange}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-12 pr-4 py-3.5 focus:border-amber-500 outline-none text-sm text-white min-h-[90px]"
                    placeholder="State the legal purpose of this verification request..."
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-mono uppercase text-slate-400 mb-3">Acceptable Document Registries</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {AVAILABLE_DOCS.map((doc) => (
                    <button
                      key={doc.id}
                      onClick={() => toggleDoc(doc.id)}
                      className={`flex items-center gap-3 p-3.5 rounded-xl border text-left transition-all ${formData.selectedDocs.includes(doc.id) ? 'border-amber-500 bg-amber-500/10 text-amber-300 font-bold' : 'border-slate-800 bg-slate-950 text-slate-400 hover:border-slate-700'}`}
                    >
                      {formData.selectedDocs.includes(doc.id) ? <CheckSquare size={18} /> : <Square size={18} />}
                      <span className="text-xs uppercase">{doc.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-mono uppercase text-slate-400 mb-3">Required Attestation Fields</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {availableFields.map((field) => (
                    <button
                      key={field.id}
                      onClick={() => toggleField(field.id)}
                      className={`flex items-center gap-3 p-3.5 rounded-xl border text-left transition-all ${formData.selectedFields.includes(field.id) ? 'border-emerald-500 bg-emerald-500/10 text-emerald-300 font-bold' : 'border-slate-800 bg-slate-950 text-slate-400 hover:border-slate-700'}`}
                    >
                      {formData.selectedFields.includes(field.id) ? <CheckSquare size={18} /> : <Square size={18} />}
                      <span className="text-xs uppercase">{field.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <button
                onClick={() => setStep(2)}
                disabled={(!isGeneralAccess && !formData.targetUserId) || !formData.purpose || formData.selectedFields.length === 0 || formData.selectedDocs.length === 0}
                className="w-full bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 py-4 rounded-xl font-bold uppercase tracking-wider text-xs transition-all disabled:opacity-30 shadow-lg shadow-amber-500/20"
              >
                Proceed to Review
              </button>
            </motion.div>
          )}

          {step === 2 && (
            <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} className="space-y-6">
              <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 space-y-4">
                <div className="flex items-center gap-2 text-amber-400 text-xs font-mono font-bold uppercase mb-2">
                  <ShieldCheck size={16} /> Verification Contract Summary
                </div>
                <div className="flex justify-between text-xs border-b border-slate-800/80 pb-2">
                  <span className="text-slate-400">Target Identity</span>
                  <span className="font-mono font-bold text-white">{isGeneralAccess ? 'Open QR Access' : normalizedTargetUserId}</span>
                </div>
                <div className="flex justify-between text-xs border-b border-slate-800/80 pb-2">
                  <span className="text-slate-400">Purpose</span>
                  <span className="font-medium text-white max-w-[240px] text-right">{formData.purpose}</span>
                </div>
                <div className="text-xs">
                  <span className="text-slate-400 block mb-2">Requested Attestation Proofs:</span>
                  <div className="flex flex-wrap gap-1.5">
                    {formData.selectedFields.map(f => (
                      <span key={f} className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-mono">
                        {f.replace('_', ' ')}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <button
                onClick={createContract}
                disabled={isVerifying}
                className="w-full bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 py-4 rounded-xl font-bold uppercase tracking-wider text-xs transition-all shadow-xl shadow-amber-500/20"
              >
                {isVerifying ? 'Generating Cryptographic Contract...' : 'Issue & Deploy Verification Contract'}
              </button>

              <button
                onClick={() => setStep(1)}
                className="w-full text-center text-xs font-mono uppercase text-slate-500 hover:text-slate-300"
              >
                Adjust Parameters
              </button>
            </motion.div>
          )}

          {step === 3 && (
            <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} className="text-center space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-mono">
                <CheckCircle size={14} /> Contract Dispatched on The Dedox Pipeline
              </div>

              <h3 className="text-2xl font-black uppercase text-white">
                {isCreatedContractGeneral ? 'Scan to Verify Citizen' : 'Direct Notification Sent'}
              </h3>

              {isCreatedContractGeneral ? (
                <div className="flex flex-col items-center">
                  <div className="bg-white p-6 rounded-2xl border-4 border-slate-800 shadow-2xl inline-block mb-4">
                    <QRCodeSVG
                      value={`${typeof window !== 'undefined' ? window.location.origin : 'https://dedox-pipeline.gov.in'}/verify/${contractId}`}
                      size={220}
                      level="H"
                      fgColor="#000000"
                      includeMargin={true}
                    />
                  </div>
                  <p className="text-xs text-slate-400 max-w-sm mx-auto">
                    Direct the officer or citizen to scan this QR code using The Dedox Mobile App to authorize biometric verification.
                  </p>
                </div>
              ) : (
                <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 text-left max-w-md mx-auto">
                  <p className="text-[10px] font-mono uppercase text-slate-400 mb-1">Target Dedox ID</p>
                  <p className="font-mono text-amber-400 font-bold text-sm break-all">{createdContractTargetUserId}</p>
                  <p className="text-xs text-slate-400 mt-3">
                    A secure push notification has been dispatched to the user's mobile device for approval.
                  </p>
                </div>
              )}

              <div className="pt-4">
                <Link
                  href="/dashboard"
                  className="inline-block w-full py-4 bg-slate-900 border border-slate-700 hover:border-slate-500 text-white rounded-xl font-bold uppercase tracking-wider text-xs transition-all"
                >
                  Monitor in Command Dashboard
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
    <Suspense fallback={<div className="min-h-screen bg-slate-950 pt-32" />}>
      <VerificationRequestContent />
    </Suspense>
  );
}
