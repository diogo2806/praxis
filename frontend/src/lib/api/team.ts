import { apiRequest } from "@/lib/api/http";

export type TeamProfile =
  | "ADMINISTRADOR"
  | "AUTOR"
  | "ANALISTA"
  | "OPERADOR"
  | "ESPECIALISTA";

export type AssignableTeamProfile = Exclude<TeamProfile, "ESPECIALISTA">;
export type TeamUserStatus = "ATIVO" | "CONVIDADO" | "BLOQUEADO";

export interface TeamUser {
  id: number;
  name: string;
  email: string;
  roles: string[];
  profile: TeamProfile;
  permissions: string[];
  status: TeamUserStatus;
  lastLoginAt: string | null;
  createdAt: string | null;
}

export interface InviteTeamUserResponse {
  user: TeamUser;
  inviteUrl: string;
}

export function listTeamUsers() {
  return apiRequest<TeamUser[]>("/api/v1/team");
}

export function inviteTeamUser(body: {
  name: string;
  email: string;
  profile: AssignableTeamProfile;
}) {
  return apiRequest<InviteTeamUserResponse>("/api/v1/team/invite", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateTeamUserAccess(userId: number, profile: AssignableTeamProfile) {
  return apiRequest<TeamUser>(`/api/v1/team/${userId}/access`, {
    method: "PUT",
    body: JSON.stringify({ profile }),
  });
}

export function resendTeamUserInvite(userId: number) {
  return apiRequest<InviteTeamUserResponse>(`/api/v1/team/${userId}/resend-invite`, {
    method: "POST",
  });
}

export function blockTeamUser(userId: number) {
  return apiRequest<TeamUser>(`/api/v1/team/${userId}/block`, { method: "POST" });
}

export function unblockTeamUser(userId: number) {
  return apiRequest<TeamUser>(`/api/v1/team/${userId}/unblock`, { method: "POST" });
}
