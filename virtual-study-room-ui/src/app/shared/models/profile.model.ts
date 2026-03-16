/*
declares frontend data models shared across services and components
*/
export interface ProfileResponse {
  userId: number;
  email: string;
  displayName: string;
  bio?: string;
  createdAt: string;
}

export interface UpdateProfileRequest {
  email: string;
  displayName: string;
  bio?: string;
  newPassword?: string;
}

