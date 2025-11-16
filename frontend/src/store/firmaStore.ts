import { create } from 'zustand';
import { FirmaResponse, FirmaRequest } from '../types/firma';
import { firmaApi } from '../services/api';

interface FirmaStore {
  // State
  requests: FirmaResponse[];
  currentRequest: FirmaResponse | null;
  loading: boolean;
  error: string | null;

  // Actions
  createRequest: (request: FirmaRequest) => Promise<FirmaResponse | null>;
  fetchAllRequests: () => Promise<void>;
  fetchRequestById: (id: string) => Promise<void>;
  downloadDocument: (id: string, filename: string) => Promise<void>;
  clearError: () => void;
  setCurrentRequest: (request: FirmaResponse | null) => void;
}

export const useFirmaStore = create<FirmaStore>((set, get) => ({
  // Initial state
  requests: [],
  currentRequest: null,
  loading: false,
  error: null,

  // Create a new signature request
  createRequest: async (request: FirmaRequest) => {
    set({ loading: true, error: null });
    try {
      const response = await firmaApi.createSignatureRequest(request);

      if (response.success && response.data) {
        set((state) => ({
          requests: [response.data, ...state.requests],
          currentRequest: response.data,
          loading: false,
        }));
        return response.data;
      } else {
        set({
          error: response.error || 'Failed to create signature request',
          loading: false
        });
        return null;
      }
    } catch (error: any) {
      set({
        error: error.message || 'An unexpected error occurred',
        loading: false
      });
      return null;
    }
  },

  // Fetch all signature requests
  fetchAllRequests: async () => {
    set({ loading: true, error: null });
    try {
      const response = await firmaApi.getAllSignatureRequests();

      if (response.success) {
        set({
          requests: response.data,
          loading: false
        });
      } else {
        set({
          error: response.error || 'Failed to fetch signature requests',
          loading: false
        });
      }
    } catch (error: any) {
      set({
        error: error.message || 'An unexpected error occurred',
        loading: false
      });
    }
  },

  // Fetch a specific signature request by ID
  fetchRequestById: async (id: string) => {
    set({ loading: true, error: null });
    try {
      const response = await firmaApi.getSignatureRequestById(id);

      if (response.success && response.data) {
        set({
          currentRequest: response.data,
          loading: false
        });

        // Update in the list if exists
        set((state) => ({
          requests: state.requests.map(req =>
            req.id === id ? response.data : req
          )
        }));
      } else {
        set({
          error: response.error || 'Failed to fetch signature request',
          loading: false
        });
      }
    } catch (error: any) {
      set({
        error: error.message || 'An unexpected error occurred',
        loading: false
      });
    }
  },

  // Download signed document
  downloadDocument: async (id: string, filename: string) => {
    set({ loading: true, error: null });
    try {
      const response = await firmaApi.downloadSignedDocument(id);

      if (response.success && response.content) {
        firmaApi.downloadBase64AsPdf(response.content, filename);
        set({ loading: false });
      } else {
        set({
          error: response.error || 'Failed to download document',
          loading: false
        });
      }
    } catch (error: any) {
      set({
        error: error.message || 'An unexpected error occurred',
        loading: false
      });
    }
  },

  // Clear error
  clearError: () => set({ error: null }),

  // Set current request
  setCurrentRequest: (request: FirmaResponse | null) =>
    set({ currentRequest: request }),
}));
