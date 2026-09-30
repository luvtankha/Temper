export interface Health { status: string; application: string; analysisMode: string }
const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, { ...init, signal: init?.signal ?? AbortSignal.timeout(5000) });
  if (!response.ok) throw new Error(`Request failed (${response.status})`);
  return response.json() as Promise<T>;
}
export const getHealth = () => request<Health>('/health');
