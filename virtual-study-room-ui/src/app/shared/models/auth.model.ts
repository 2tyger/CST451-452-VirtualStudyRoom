export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  displayName: string;
}