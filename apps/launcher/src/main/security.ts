export const APP_URL = 'space://app/index.html';
export const CSP =
  "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; font-src 'self'; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-src 'none'";
export function trustedSender(
  actualContents: unknown,
  expectedContents: unknown,
  actualFrame: unknown,
  expectedFrame: unknown,
  url: string,
): boolean {
  return (
    actualContents === expectedContents &&
    !!expectedContents &&
    actualFrame === expectedFrame &&
    !!expectedFrame &&
    url === APP_URL
  );
}
export function assetName(url: string): string | null {
  try {
    const parsed = new URL(url);
    if (
      parsed.protocol !== 'space:' ||
      parsed.host !== 'app' ||
      parsed.username ||
      parsed.password ||
      parsed.search ||
      parsed.hash
    )
      return null;
    return ['/index.html', '/app.js', '/app.css', '/hero.png'].includes(parsed.pathname)
      ? parsed.pathname.slice(1)
      : null;
  } catch {
    return null;
  }
}
