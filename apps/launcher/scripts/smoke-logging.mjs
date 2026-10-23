import { LOGGING_LIBRARIES } from '../src/main/logging-libraries.ts';
import { downloadArtifact } from '../src/main/downloads.ts';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { writeFile, mkdir } from 'node:fs/promises';
import { resolve, join, delimiter } from 'node:path';

const repository = resolve(import.meta.dirname, '../../..');
const root = join(repository, '.reports/launcher-log4j-smoke');
await mkdir(root, { recursive: true });
const files = [];
for (const artifact of LOGGING_LIBRARIES) files.push((await downloadArtifact(root, artifact)).path);
const source = join(root, 'SpaceLoggingSmoke.java');
await writeFile(
  source,
  `import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class SpaceLoggingSmoke {
  public static void main(String[] args) {
    Logger logger = LogManager.getLogger(SpaceLoggingSmoke.class);
    logger.error("SPACE_LOGGING_SMOKE_OK");
    System.out.println("api=" + Logger.class.getPackage().getImplementationVersion());
    System.out.println("core=" + org.apache.logging.log4j.core.LoggerContext.class.getPackage().getImplementationVersion());
  }
}
`,
);
const env = { ...process.env };
for (const key of Object.keys(env))
  if (
    ['JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'CLASSPATH'].includes(
      key.toUpperCase(),
    )
  )
    delete env[key];
const execute = promisify(execFile);
const options = { timeout: 15000, maxBuffer: 65536, windowsHide: true, shell: false, env };
const classpath = files.join(delimiter);
await execute(
  join(repository, '.tools/jdk-21/bin/javac.exe'),
  ['-proc:none', '-cp', classpath, '-d', root, source],
  options,
);
const result = await execute(
  join(repository, '.tools/jdk-21/bin/java.exe'),
  ['-cp', [root, ...files].join(delimiter), 'SpaceLoggingSmoke'],
  options,
);
if (
  !result.stdout.includes('api=2.26.1') ||
  !result.stdout.includes('core=2.26.1') ||
  !(result.stdout + result.stderr).includes('SPACE_LOGGING_SMOKE_OK')
)
  throw new Error('Log4j runtime smoke failed');
const report = {
  checkedAt: new Date().toISOString(),
  loggingVersion: '2.26.1',
  javaMajor: 21,
  artifacts: LOGGING_LIBRARIES,
  apiAndCoreLoaded: true,
  logEventObserved: true,
  note: 'Two pinned Maven Central jars verified by SHA-1 and SHA-256, compiled and loaded by the project JDK 21. This is an isolated logging smoke, not an authenticated game launch.',
};
await writeFile(join(root, 'result.json'), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
