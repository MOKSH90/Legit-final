const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

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

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Something went wrong') as Error & { status?: number };
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
