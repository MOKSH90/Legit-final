const API_URL = process.env.NEXT_PUBLIC_API_URL || '/api/proxy';

export async function apiRequest(endpoint: string, options: RequestInit = {}) {
  const token = typeof window !== 'undefined' ? localStorage.getItem('legit_token') : null;

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const error = new Error(data?.message || 'Something went wrong') as Error & { status?: number };
    error.status = response.status;
    throw error;
  }

  return data;
}

export const api = {
  auth: {
    login: (credentials: Record<string, unknown>) => apiRequest('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    }),
    register: (userData: Record<string, unknown>) => apiRequest('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    }),
  },
  user: {
    getProfile: () => apiRequest('/api/v1/user/me'),
    getByRole: (role: string) => apiRequest(`/api/v1/user/directory/role/${role}`),
    getPublicKey: (userId: string) => apiRequest(`/api/v1/user/${userId}/public-key`),
  },
  documents: {
    getAll: (params?: { all?: boolean; department?: string; caseNumber?: string }) => {
      const searchParams = new URLSearchParams();
      if (params?.all) searchParams.append('all', 'true');
      if (params?.department) searchParams.append('department', params.department);
      if (params?.caseNumber) searchParams.append('caseNumber', params.caseNumber);
      const queryString = searchParams.toString();
      return apiRequest(`/api/v1/documents${queryString ? `?${queryString}` : ''}`);
    },
    getById: (id: string) => apiRequest(`/api/v1/documents/${id}`),
    getByCase: (caseNumber: string) => apiRequest(`/api/v1/documents/case/${caseNumber}`),
    getByDepartment: (department: string) => apiRequest(`/api/v1/documents/department/${department}`),
    download: (id: string) => apiRequest(`/api/v1/documents/${id}/download`),
    verifyIntegrity: (id: string) => apiRequest(`/api/v1/documents/${id}/verify-integrity`),
    updateStatus: (id: string, status: string) => apiRequest(`/api/v1/documents/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  },
  pipeline: {
    createContract: (contractData: Record<string, unknown>) => apiRequest('/api/v1/pipeline/contracts', {
      method: 'POST',
      body: JSON.stringify(contractData),
    }),
    getUserContracts: (status?: string) => {
      const query = status ? `?status=${status}` : '';
      return apiRequest(`/api/v1/pipeline/user/contracts${query}`);
    },
    getRequesterContracts: (status?: string) => {
      const query = status ? `?status=${status}` : '';
      return apiRequest(`/api/v1/pipeline/contracts/requester${query}`);
    },
    getContractResult: (contractId: string) => apiRequest(`/api/v1/pipeline/contracts/${contractId}/result`),
    executeVerification: (verificationData: Record<string, unknown>) => apiRequest('/api/v1/pipeline/verify', {
      method: 'POST',
      body: JSON.stringify(verificationData),
    }),
    executeP2PVerification: (contractId: string) => apiRequest(`/api/v1/pipeline/p2p-verify/${contractId}`, {
      method: 'POST',
    }),
  },
  info: {
    getVerificationFields: () => apiRequest('/api/v1/pipeline/info/verification-fields'),
  }
};
