export type Profile = { id: string; name: string; memoryMb: number; javaPath: string | null };
export type SavedState = { schemaVersion: 1; activeProfileId: string; profiles: Profile[] };
export type Snapshot = SavedState & {
  maxMemoryMb: number;
  totalMemoryMb: number;
  dataRoot: string;
  warning: string | null;
};
export type ProfileEdit = { id: string; name: string; memoryMb: number };
export type JavaInfo = {
  usable: boolean;
  major: number | null;
  bits: number | null;
  message: string;
};
export type Result<T> = { ok: true; value: T } | { ok: false; error: string };
export type LauncherApi = {
  getState(): Promise<Result<Snapshot>>;
  saveProfile(edit: ProfileEdit): Promise<Result<Snapshot>>;
  createProfile(): Promise<Result<Snapshot>>;
  selectProfile(id: string): Promise<Result<Snapshot>>;
  chooseJava(id: string): Promise<Result<Snapshot>>;
  checkJava(id: string): Promise<Result<JavaInfo>>;
  openInstance(id: string): Promise<Result<null>>;
};
