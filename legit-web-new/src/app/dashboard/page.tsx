'use client';

import { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { 
  History, 
  ShieldCheck, 
  Zap,
  Plus,
  RefreshCcw,
  Search,
  ExternalLink,
  Activity,
  UserCheck,
  CheckCircle,
  Car,
  LogOut
} from 'lucide-react';
import Link from 'next/link';
import { api } from '@/lib/api';
import { useRouter } from 'next/navigation';

interface DocumentMetadata {
  fullName?: string;
  dateOfBirth?: string;
  address?: string;
  fatherName?: string;
  gender?: string;
}

interface FieldResult {
  verified: boolean;
  extractedValue?: string;
}

interface VerificationResult {
  fieldResults: Record<string, FieldResult>;
  proofHash: string;
}

interface Contract {
  contractId: string;
  status: string;
  purpose: string;
  targetLegitId?: string;
  requiredFields: string[];
  requiredDocumentTypes: string[];
  createdAt: number;
  userId: string | null;
  requesterId: string;
  requesterName: string;
}

interface DetailedContract extends Contract {
  metadata?: DocumentMetadata;
  result: VerificationResult;
}

const METADATA_FIELD_CONFIG = {
  FULL_NAME: { key: 'fullName', label: 'Full Name' },
  DATE_OF_BIRTH: { key: 'dateOfBirth', label: 'Date of Birth' },
  GENDER: { key: 'gender', label: 'Gender' },
  FATHER_NAME: { key: 'fatherName', label: "Father's Name" },
  ADDRESS: { key: 'address', label: 'Verified Address' },
} as const;

export default function DashboardPage() {
  const router = useRouter();
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState('');
  const [userRole, setUserRole] = useState('');
  const [userLegitId, setUserLegitId] = useState('');
  const [stats, setStats] = useState([
    { label: 'ACTIVE CONTRACTS', value: '0', icon: Activity, color: 'text-rose-600' },
    { label: 'VERIFIED BUYERS', value: '0', icon: UserCheck, color: 'text-green-600' },
    { label: 'PENDING SHOWROOM', value: '0', icon: RefreshCcw, color: 'text-slate-400' },
    { label: 'SALES TELEMETRY', value: '0', icon: ShieldCheck, color: 'text-rose-600' },
  ]);

  const handleLogout = () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('legit_token');
      localStorage.removeItem('legit_user');
    }
    router.push('/login');
  };

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setFetchError('');
    try {
      const storedToken = typeof window !== 'undefined' ? localStorage.getItem('legit_token') : null;
      const storedUser = typeof window !== 'undefined' ? localStorage.getItem('legit_user') : null;
      const parsedUser = storedUser ? JSON.parse(storedUser) : null;
      if (!storedToken || !parsedUser?.role) {
        if (typeof window !== 'undefined') {
          localStorage.removeItem('legit_token');
          localStorage.removeItem('legit_user');
        }
        router.push('/login');
        return;
      }

      const role = parsedUser?.role || '';
      setUserRole(role);
      setUserLegitId(parsedUser?.legitId || '');

      const response = role === 'USER'
        ? await api.pipeline.getUserContracts()
        : await api.pipeline.getRequesterContracts();
      const contractList = (response.data?.contracts || response.contracts || []) as Contract[];
      setContracts(contractList);
      
      // Update stats based on real data
      const active = contractList.filter((c: Contract) => c.status === 'APPROVED' || c.status === 'PENDING_APPROVAL').length;
      const verified = contractList.filter((c: Contract) => c.status === 'VERIFIED').length;
      const pending = contractList.filter((c: Contract) => c.status === 'PENDING_APPROVAL').length;
      
      setStats([
        { label: 'ACTIVE CONTRACTS', value: active.toString().padStart(2, '0'), icon: Activity, color: 'text-rose-600' },
        { label: 'VERIFIED BUYERS', value: verified.toString().padStart(2, '0'), icon: UserCheck, color: 'text-green-600' },
        { label: 'PENDING SHOWROOM', value: pending.toString().padStart(2, '0'), icon: RefreshCcw, color: 'text-slate-400' },
        { label: 'SALES TELEMETRY', value: contractList.length.toString().padStart(2, '0'), icon: ShieldCheck, color: 'text-rose-600' },
      ]);
    } catch (err: unknown) {
      const error = err as Error & { status?: number };
      console.error('Failed to fetch contracts:', error);
      if (typeof window !== 'undefined' && (error.status === 401 || !localStorage.getItem('legit_token'))) {
        localStorage.removeItem('legit_token');
        localStorage.removeItem('legit_user');
        router.push('/login');
        return;
      }
      setFetchError(error.message || 'Failed to fetch contracts.');
    } finally {
      setIsLoading(false);
    }
  }, [router]);

  useEffect(() => {
    fetchData();
    
    // Set up auto-polling for real-time telemetry (every 5 seconds)
    const interval = setInterval(() => {
      fetchData();
    }, 5000);
    
    return () => clearInterval(interval);
  }, [fetchData]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'VERIFIED': return 'bg-green-600 text-white';
      case 'COMPLETED': return 'bg-green-600 text-white';
      case 'PENDING_APPROVAL': return 'bg-amber-500 text-white';
      case 'APPROVED': return 'bg-blue-600 text-white';
      case 'REJECTED': return 'bg-rose-600 text-white';
      case 'EXPIRED': return 'bg-slate-400 text-white';
      default: return 'bg-slate-200 text-slate-600';
    }
  };

  const [selectedContract, setSelectedContract] = useState<DetailedContract | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [showVerifyModal, setShowVerifyModal] = useState(false);
  const [disposableKey, setDisposableKey] = useState('');
  const [isExecuting, setIsExecuting] = useState(false);
  const [processingContractId, setProcessingContractId] = useState<string | null>(null);

  const viewResult = async (contract: Contract) => {
    if (contract.status !== 'VERIFIED') return;
    try {
      const response = await api.pipeline.getContractResult(contract.contractId);
      setSelectedContract(response.data || response);
      setShowModal(true);
    } catch (err) {
      console.error('Failed to fetch contract result:', err);
    }
  };

  const handleVerify = async (contract: Contract) => {
    setProcessingContractId(contract.contractId);
    try {
      const result = await api.pipeline.executeP2PVerification(contract.contractId);
      fetchData();
      
      // Auto-open the result modal so the user sees metadata immediately
      setSelectedContract(result.data || result);
      setShowModal(true);
    } catch (err: unknown) {
      const error = err as Error;
      alert(error.message || 'Verification failed');
    } finally {
      setProcessingContractId(null);
    }
  };

  const executeVerification = async () => {
    // This is now legacy but kept for stability
    if (!selectedContract) return;
    handleVerify(selectedContract as Contract);
    setShowVerifyModal(false);
  };

  const closeModal = () => {
    setShowModal(false);
    setShowVerifyModal(false);
    setSelectedContract(null);
    setDisposableKey('');
  };

  const requestedMetadataFields = selectedContract
    ? selectedContract.requiredFields.filter(
        (field): field is keyof typeof METADATA_FIELD_CONFIG => field in METADATA_FIELD_CONFIG
      )
    : [];

  const visibleMetadata = selectedContract?.metadata
    ? requestedMetadataFields
        .map((field) => {
          const config = METADATA_FIELD_CONFIG[field];
          const value = selectedContract.metadata?.[config.key];
          return value ? { label: config.label, value, key: config.key } : null;
        })
        .filter((item): item is NonNullable<typeof item> => !!item)
    : [];

  return (
    <div className="min-h-screen bg-slate-50 pt-32 pb-12 px-4 lg:px-8">
      {/* Verify Modal */}
      {showVerifyModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <motion.div 
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white border-t-8 border-blue-600 w-full max-w-md p-8 shadow-2xl relative"
          >
            <button onClick={closeModal} className="absolute top-4 right-4 text-slate-400 hover:text-black font-black uppercase text-xs tracking-widest">CLOSE [X]</button>
            <div className="mb-8">
              <h2 className="text-3xl font-black uppercase italic tracking-tighter mb-2">Execute <span className="text-blue-600">Verification</span></h2>
              <p className="text-slate-500 text-xs font-bold uppercase tracking-widest italic">Contract: {selectedContract?.contractId.toUpperCase()}</p>
            </div>

            <div className="space-y-6">
              <div>
                <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-3">Disposable Key (from Buyer)</label>
                <input
                  type="text"
                  value={disposableKey}
                  onChange={(e) => setDisposableKey(e.target.value)}
                  className="w-full bg-slate-50 border-2 border-slate-100 px-4 py-4 focus:border-blue-600 outline-none transition-colors font-mono font-bold text-sm"
                  placeholder="lgk_..."
                />
              </div>

              <button 
                onClick={executeVerification}
                disabled={isExecuting || !disposableKey}
                className="w-full bg-blue-600 text-white py-4 font-black uppercase tracking-widest hover:bg-blue-700 transition-colors skew-x-[-12deg] disabled:opacity-50"
              >
                 <span className="skew-x-[12deg] flex items-center justify-center gap-2">
                   {isExecuting ? <RefreshCcw className="animate-spin" size={18} /> : 'TRIGGER SECURE PIPELINE'}
                 </span>
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {/* Result Modal */}
      {showModal && selectedContract && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <motion.div 
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white border-t-8 border-rose-600 w-full max-w-2xl p-8 shadow-2xl relative"
          >
            <button onClick={closeModal} className="absolute top-4 right-4 text-slate-400 hover:text-black font-black uppercase text-xs tracking-widest">CLOSE [X]</button>
            <div className="mb-8">
              <h2 className="text-3xl font-black uppercase italic tracking-tighter mb-2">Buyer <span className="text-rose-600">Credentials</span></h2>
              <p className="text-slate-500 text-xs font-bold uppercase tracking-widest italic">Verification ID: {selectedContract.contractId.toUpperCase()}</p>
            </div>

            {visibleMetadata.length > 0 && (
              <div className="mb-8 p-6 bg-slate-900 text-white skew-x-[-4deg]">
                <div className="skew-x-[4deg]">
                  <p className="text-[10px] font-black text-rose-500 uppercase tracking-[0.3em] mb-4">Identity Metadata</p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-y-4 gap-x-8">
                    {visibleMetadata
                      .filter((item) => item.key !== 'address')
                      .map((item) => (
                        <div key={item.key}>
                          <p className="text-[8px] font-black text-slate-500 uppercase tracking-widest mb-1">{item.label}</p>
                          <p className="font-black text-xl uppercase italic tracking-tight">{item.value}</p>
                        </div>
                      ))}
                  </div>
                  {visibleMetadata
                    .filter((item) => item.key === 'address')
                    .map((item) => (
                      <div key={item.key} className="mt-4 border-t border-slate-800 pt-4">
                        <p className="text-[8px] font-black text-slate-500 uppercase tracking-widest mb-1">{item.label}</p>
                        <p className="font-bold text-sm uppercase italic text-slate-300">{item.value}</p>
                      </div>
                    ))}
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
              {Object.entries(selectedContract.result.fieldResults).map(([key, value]) => (
                <div key={key} className="p-4 bg-slate-50 border-l-4 border-rose-600">
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{key.replace('_', ' ')}</p>
                  <div className="flex items-center justify-between">
                    <span className="font-black text-lg uppercase text-black">{value.extractedValue || 'VERIFIED'}</span>
                    {value.verified ? <CheckCircle className="text-green-600" size={16} /> : <Zap className="text-rose-600" size={16} />}
                  </div>
                </div>
              ))}
            </div>

            <div className="p-4 bg-black text-white italic">
               <p className="text-[10px] font-black text-rose-600 uppercase tracking-[0.2em] mb-2">Cryptographic Proof</p>
               <p className="font-mono text-[10px] break-all opacity-60">{selectedContract.result.proofHash}</p>
            </div>
            
            <button 
              onClick={closeModal}
              className="w-full mt-8 bg-rose-600 text-white py-4 font-black uppercase tracking-widest hover:bg-red-700 transition-colors skew-x-[-12deg]"
            >
               <span className="skew-x-[12deg]">CONFIRM & ALLOCATE VEHICLE</span>
            </button>
          </motion.div>
        </div>
      )}

      <div className="max-w-7xl mx-auto">
        <header className="mb-12 flex flex-col md:flex-row md:items-end justify-between gap-6 border-b-4 border-black pb-8">
          <div>
            <div className="flex items-center gap-2 text-rose-600 mb-2">
              <Zap size={20} fill="currentColor" />
              <span className="text-xs font-black uppercase tracking-[0.3em]">Operational Telemetry</span>
            </div>
            <h1 className="text-6xl font-black uppercase italic tracking-tighter text-black">Command <span className="text-rose-600">Center</span></h1>
            {userLegitId && (
              <div className="mt-2 inline-block bg-slate-100 px-3 py-1 border-l-4 border-black">
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">USER ID: <span className="text-black normal-case">{userLegitId}</span></p>
              </div>
            )}
          </div>
          <div className="flex gap-4">
            <button 
              onClick={fetchData}
              className="bg-white border-2 border-black text-black px-6 py-4 font-black uppercase tracking-widest text-sm transition-colors hover:bg-slate-50 flex items-center gap-2 skew-x-[-12deg]"
            >
              <span className="skew-x-[12deg] flex items-center gap-2"><RefreshCcw size={18} className={isLoading ? 'animate-spin' : ''} /> REFRESH</span>
            </button>
            <button
              onClick={handleLogout}
              className="bg-white border-2 border-black text-black px-6 py-4 font-black uppercase tracking-widest text-sm transition-colors hover:bg-slate-50 flex items-center gap-2 skew-x-[-12deg]"
            >
              <span className="skew-x-[12deg] flex items-center gap-2"><LogOut size={18} /> LOGOUT</span>
            </button>
            {userRole !== 'USER' && (
              <Link href="/kyc" className="bg-black text-white px-8 py-4 font-black uppercase tracking-widest text-sm transition-colors hover:bg-rose-600 flex items-center gap-2 skew-x-[-12deg]">
                <span className="skew-x-[12deg] flex items-center gap-2"><Plus size={18} /> NEW CONTRACT</span>
              </Link>
            )}
          </div>
        </header>

        {fetchError && (
          <div className="mb-8 border-l-4 border-rose-600 bg-rose-50 p-4">
            <p className="text-rose-600 text-xs font-bold uppercase tracking-widest">{fetchError}</p>
          </div>
        )}

        {userRole === 'USER' && (
          <div className="mb-8 border-l-4 border-blue-600 bg-blue-50 p-4">
            <p className="text-blue-700 text-xs font-bold uppercase tracking-widest">Viewing contracts assigned to your user account.</p>
          </div>
        )}

        {/* Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-0 border border-black mb-12 bg-white">
          {stats.map((stat, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              className="p-8 border-r last:border-r-0 border-black"
            >
              <stat.icon className={`${stat.color} mb-6`} size={32} />
              <p className="text-slate-400 text-xs font-black uppercase tracking-widest mb-1">{stat.label}</p>
              <h3 className="text-4xl font-black italic uppercase tracking-tighter text-black">{stat.value}</h3>
            </motion.div>
          ))}
        </div>

        <div className="grid grid-cols-1 gap-12">
          {/* Contracts Table */}
          <section>
            <div className="flex items-center justify-between mb-8">
              <h2 className="text-3xl font-black uppercase italic tracking-tighter flex items-center gap-3 text-black">
                <History size={28} className="text-rose-600" />
                Verification <span className="text-rose-600">Pipeline</span>
              </h2>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <input 
                  type="text" 
                  placeholder="SEARCH BUYERS..." 
                  className="pl-10 pr-4 py-2 bg-white border border-slate-200 outline-none focus:border-rose-600 font-bold text-xs uppercase tracking-widest text-black"
                />
              </div>
            </div>

            <div className="bg-white border border-black overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead className="bg-black text-white">
                    <tr>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Contract ID</th>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Target Buyer</th>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Purpose</th>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Fields</th>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Status</th>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Created</th>
                      <th className="p-4 text-[10px] font-black uppercase tracking-[0.2em]">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {contracts.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="p-12 text-center">
                          <Car size={48} className="mx-auto text-slate-200 mb-4" />
                          <p className="text-slate-400 font-black uppercase tracking-widest text-xs italic">No active contracts in pipeline</p>
                        </td>
                      </tr>
                    ) : (
                      contracts.map((contract, i) => (
                        <tr key={i} className="hover:bg-slate-50 transition-colors group">
                          <td className="p-4 font-mono text-[10px] font-bold text-slate-500">#{contract.contractId.slice(-8).toUpperCase()}</td>
                          <td className="p-4">
                            {contract.targetLegitId || contract.userId ? (
                              <div className="bg-rose-50 text-rose-600 text-[10px] font-black px-2 py-1 inline-block border-l-2 border-rose-600 lowercase italic">
                                {contract.targetLegitId || contract.userId}
                              </div>
                            ) : (
                              <span className="text-[10px] font-bold text-slate-300 uppercase italic">General Access</span>
                            )}
                          </td>
                          <td className="p-4">
                            <div className="font-black uppercase tracking-tight text-sm text-black">{contract.purpose}</div>
                          </td>
                          <td className="p-4">
                            <div className="flex flex-wrap gap-1">
                              {contract.requiredFields.map((field: string, j: number) => (
                                <span key={j} className="text-[8px] font-black bg-slate-100 px-1.5 py-0.5 rounded uppercase tracking-tighter text-black">
                                  {field.replace('_', ' ')}
                                </span>
                              ))}
                            </div>
                          </td>
                          <td className="p-4">
                            <span className={`text-[10px] font-black uppercase tracking-widest px-3 py-1 skew-x-[-12deg] inline-block ${getStatusColor(contract.status)}`}>
                              <span className="skew-x-[12deg] inline-block">{contract.status === 'VERIFIED' ? 'COMPLETED' : contract.status.replace('_', ' ')}</span>
                            </span>
                          </td>
                          <td className="p-4 text-[10px] font-bold text-slate-400">
                            {new Date(contract.createdAt).toLocaleDateString()}
                          </td>
                          <td className="p-4">
                            <div className="flex items-center gap-2">
                              {/* If approved, show the Zap icon to finalize/view */}
                              {contract.status === 'APPROVED' && (
                                <button 
                                  onClick={() => handleVerify(contract)}
                                  disabled={processingContractId === contract.contractId}
                                  className={`${processingContractId === contract.contractId ? 'text-slate-400' : 'text-blue-600 hover:text-blue-700'} transition-colors p-2 hover:scale-125 transition-transform`}
                                  title="Finalize & View Result"
                                >
                                  {processingContractId === contract.contractId ? (
                                    <RefreshCcw size={18} className="animate-spin" />
                                  ) : (
                                    <Zap size={18} fill="currentColor" />
                                  )}
                                </button>
                              )}
                              
                              {/* If already verified, show the Result icon */}
                              {contract.status === 'VERIFIED' && (
                                <button 
                                  onClick={() => viewResult(contract)}
                                  className="text-rose-600 hover:text-red-700 transition-colors p-2 hover:scale-125 transition-transform"
                                  title="View Verification Result"
                                >
                                  <ExternalLink size={18} />
                                </button>
                              )}
                              
                              {contract.status !== 'APPROVED' && contract.status !== 'VERIFIED' && (
                                <div className="text-slate-300 p-2 italic text-[10px]">WAITING</div>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
