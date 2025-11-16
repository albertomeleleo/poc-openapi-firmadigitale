import axios, { AxiosInstance } from 'axios';
import {
  FirmaRequest,
  FirmaResponse,
  ApiResponse,
  DownloadResponse
} from '../types/firma';

class FirmaApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: '/api',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for logging
    this.client.interceptors.request.use(
      (config) => {
        console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('[API] Request error:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => {
        console.log(`[API] Response:`, response.status);
        return response;
      },
      (error) => {
        console.error('[API] Response error:', error.response?.data || error.message);
        return Promise.reject(error);
      }
    );
  }

  /**
   * Create a new signature request
   */
  async createSignatureRequest(request: FirmaRequest): Promise<ApiResponse<FirmaResponse>> {
    try {
      const response = await this.client.post<ApiResponse<FirmaResponse>>(
        '/firma',
        request
      );
      return response.data;
    } catch (error: any) {
      return {
        data: null as any,
        success: false,
        error: error.response?.data?.error || error.message || 'Failed to create signature request'
      };
    }
  }

  /**
   * Get all signature requests
   */
  async getAllSignatureRequests(): Promise<ApiResponse<FirmaResponse[]>> {
    try {
      const response = await this.client.get<ApiResponse<FirmaResponse[]>>('/firma');
      return response.data;
    } catch (error: any) {
      return {
        data: [],
        success: false,
        error: error.response?.data?.error || error.message || 'Failed to fetch signature requests'
      };
    }
  }

  /**
   * Get signature request by ID
   */
  async getSignatureRequestById(id: string): Promise<ApiResponse<FirmaResponse>> {
    try {
      const response = await this.client.get<ApiResponse<FirmaResponse>>(`/firma/${id}`);
      return response.data;
    } catch (error: any) {
      return {
        data: null as any,
        success: false,
        error: error.response?.data?.error || error.message || 'Failed to fetch signature request'
      };
    }
  }

  /**
   * Download signed document
   */
  async downloadSignedDocument(id: string): Promise<DownloadResponse> {
    try {
      const response = await this.client.get<DownloadResponse>(`/firma/${id}/download`);
      return response.data;
    } catch (error: any) {
      return {
        content: '',
        success: false,
        error: error.response?.data?.error || error.message || 'Failed to download document'
      };
    }
  }

  /**
   * Get audit trail for a signature request
   */
  async getAuditTrail(id: string): Promise<ApiResponse<any>> {
    try {
      const response = await this.client.get<ApiResponse<any>>(`/firma/${id}/audit`);
      return response.data;
    } catch (error: any) {
      return {
        data: null,
        success: false,
        error: error.response?.data?.error || error.message || 'Failed to fetch audit trail'
      };
    }
  }

  /**
   * Health check
   */
  async healthCheck(): Promise<ApiResponse<string>> {
    try {
      const response = await this.client.get<ApiResponse<string>>('/firma/health');
      return response.data;
    } catch (error: any) {
      return {
        data: '',
        success: false,
        error: error.response?.data?.error || error.message || 'Service unavailable'
      };
    }
  }

  /**
   * Helper: Convert file to base64
   */
  fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        const result = reader.result as string;
        // Remove the data:application/pdf;base64, prefix
        const base64 = result.split(',')[1];
        resolve(base64);
      };
      reader.onerror = (error) => reject(error);
    });
  }

  /**
   * Helper: Download base64 PDF as file
   */
  downloadBase64AsPdf(base64Content: string, filename: string) {
    const linkSource = `data:application/pdf;base64,${base64Content}`;
    const downloadLink = document.createElement('a');
    downloadLink.href = linkSource;
    downloadLink.download = filename;
    downloadLink.click();
  }
}

export const firmaApi = new FirmaApiService();
