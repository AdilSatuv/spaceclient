const { spawn } = require('node:child_process');
const { mkdir, readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');

// Audits use a fresh headless Chrome profile, never the user's browser session.
async function main() {
  const base = new URL(process.env.SPACE_WEB_URL || 'http://127.0.0.1:3000');
  if (!['http:', 'https:'].includes(base.protocol) || base.username || base.password) {
    throw new Error('SPACE_WEB_URL must be an HTTP(S) origin without credentials');
  }
  const runs = Number(process.env.SPACE_LAB_RUNS || 3);
  if (!Number.isInteger(runs) || runs < 1 || runs > 5) throw new Error('Use 1–5 runs');
  const routes = process.argv.slice(2);
  if (!routes.length) routes.push('/');
  if (routes.some((route) => !/^\/(?!\/)[a-z0-9/-]*$/.test(route))) {
    throw new Error('Pass route paths, for example / /features /download');
  }
  const output = path.resolve(process.env.SPACE_LAB_OUTPUT || '.reports/lighthouse');
  await mkdir(output, { recursive: true });
  const cli = path.join(path.dirname(require.resolve('lighthouse/package.json')), 'cli/index.js');
  const results = [];
  for (const route of routes) {
    for (let run = 1; run <= runs; run++) {
      const file = path.join(
        output,
        `${route === '/' ? 'home' : route.slice(1).replaceAll('/', '-')}-${run}.json`,
      );
      console.log(`Lighthouse ${route}, run ${run}/${runs}`);
      await new Promise((resolve, reject) => {
        const child = spawn(
          process.execPath,
          [
            cli,
            new URL(route, base).href,
            '--chrome-flags=--headless=new --disable-gpu',
            '--only-categories=performance,accessibility,seo',
            '--output=json',
            `--output-path=${file}`,
            '--quiet',
          ],
          { stdio: 'inherit', windowsHide: true },
        );
        child.on('error', reject);
        child.on('exit', (code) =>
          code === 0 ? resolve() : reject(new Error(`Lighthouse exited ${code}`)),
        );
      });
      const report = JSON.parse(await readFile(file, 'utf8'));
      if (report.runtimeError) throw new Error(report.runtimeError.message);
      results.push({
        route,
        run,
        version: report.lighthouseVersion,
        fetchedAt: report.fetchTime,
        browser: report.environment.hostUserAgent,
        settings: report.configSettings,
        scores: Object.fromEntries(
          Object.entries(report.categories).map(([key, value]) => [
            key,
            Math.round(value.score * 100),
          ]),
        ),
        lcpMs: report.audits['largest-contentful-paint'].numericValue,
        cls: report.audits['cumulative-layout-shift'].numericValue,
        failures: Object.values(report.audits)
          .filter((audit) => audit.score === 0 && audit.scoreDisplayMode === 'binary')
          .map((audit) => ({ id: audit.id, title: audit.title, details: audit.details?.items })),
      });
      await writeFile(path.join(output, 'summary.json'), JSON.stringify(results, null, 2) + '\n');
    }
  }
  const median = (values) => [...values].sort((a, b) => a - b)[Math.floor(values.length / 2)];
  console.table(
    routes.map((route) => {
      const group = results.filter((item) => item.route === route);
      return {
        route,
        runs: group.length,
        performance: median(group.map((item) => item.scores.performance)),
        accessibility: median(group.map((item) => item.scores.accessibility)),
        seo: median(group.map((item) => item.scores.seo)),
        lcpMs: Math.round(median(group.map((item) => item.lcpMs))),
        cls: median(group.map((item) => item.cls)),
      };
    }),
  );
  console.log(`Raw reports and summary: ${output}`);
  console.log(
    'Preview noindex is expected to fail crawlability; do not enable indexing for a lab score.',
  );
}
main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
