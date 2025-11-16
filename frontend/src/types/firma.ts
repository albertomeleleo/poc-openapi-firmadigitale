export interface FirmaRequest {
  title?: string;
  description?: string;
  ui?: string;
  filename?: string;
  content: string; // Base64 encoded PDF
  members: Member[];
  callback?: Callback;
}

export interface Member {
  firstname: string;
  lastname: string;
  email: string;
  phone: string; // Must include international prefix starting with '+'
  signs: SignPosition[];
}

export interface SignPosition {
  page: number; // Page number (starts at 1)
  position?: string; // Bounding box: "x1,y1,x2,y2"
}

export interface Callback {
  field: string;
  url: string;
  headers?: Record<string, string>;
}

export interface FirmaResponse {
  id: string;
  filename: string;
  title?: string;
  description?: string;
  members: MemberStatus[];
  status: FirmaStatus;
  downloadLink?: string;
  callbackStatus?: string;
  callback?: Callback;
}

export interface MemberStatus {
  firstname: string;
  lastname: string;
  email: string;
  phone: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  signLink: string;
}

export enum FirmaStatus {
  CREATED = 'created',
  STARTED = 'started',
  FINISHED = 'finished',
  REFUSED = 'refused',
  EXPIRED = 'expired',
  REQUEST_FAILED = 'request_failed',
  FILE_VALIDATION_FAILED = 'file_validation_failed',
  ERROR = 'error'
}

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
  error?: string;
}

export interface DownloadResponse {
  content: string; // Base64 encoded PDF
  success: boolean;
  message?: string;
  error?: string;
}
