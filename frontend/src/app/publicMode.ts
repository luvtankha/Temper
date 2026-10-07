/** Static GitHub Pages builds must never connect to the development backend. */
export const publicDemo = import.meta.env.VITE_PUBLIC_DEMO === 'true';

export function backendTransport(search: string, configured: string | undefined, hosted = publicDemo) {
  if (hosted) return null;
  const query = new URLSearchParams(search);
  const transport = query.get('transport') ?? configured;
  return transport === 'backend' || transport === 'rest'
    ? { conversation: query.get('conversation') ?? undefined, realtime: transport === 'backend' }
    : null;
}
