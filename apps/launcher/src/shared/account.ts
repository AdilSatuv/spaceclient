import type {
  SpaceAccountProfile,
  SpaceCredentials,
  SpaceRegistration,
  SpaceProfileEdit,
  SkinModel,
} from '@space/account-contracts';
export type {
  SpaceAccountProfile,
  SpaceCredentials,
  SpaceRegistration,
  SpaceProfileEdit,
  SkinModel,
};
export type ImageSelection = { expectedRevision: number; model?: SkinModel };
export type AccountState = {
  profile: SpaceAccountProfile | null;
  expiresAt: string | null;
  skinPreview: string | null;
  capePreview: string | null;
  message: string;
};
export type RecoveryResult = { message: string; files: number };
