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