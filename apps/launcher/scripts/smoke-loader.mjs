import { LOADER_LIBRARIES } from '../src/main/loader-libraries.ts';
import { downloadPlan } from '../src/main/download-queue.ts';
import { extractNatives } from '../src/main/native-extraction.ts';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve, join, delimiter } from 'node:path';
import { openPromise } from 'yauzl';

const repository = resolve(import.meta.dirname, '../../..');
const root = join(repository, '.reports/launcher-loader-smoke');
await mkdir(root, { recursive: true });
await downloadPlan(
  root,
  LOADER_LIBRARIES.map((artifact) => ({ ...artifact })),
);
const extracted = await extractNatives(
  root,
  LOADER_LIBRARIES.filter((artifact) => artifact.native),
);
const mapping = LOADER_LIBRARIES.find((artifact) => artifact.url.includes('/intermediary/'));
const mappingZip = await openPromise(join(root, mapping.relativePath), {
  lazyEntries: true,
  strictFileNames: true,
});
let mappingHeader;
try {
  for await (const entry of mappingZip.eachEntry()) {
    if (entry.fileName !== 'mappings/mappings.tiny') continue;
    if (entry.uncompressedSize > 2 * 1024 * 1024) throw new Error('Mapping too large');
    const stream = await mappingZip.openReadStreamPromise(entry);
    const chunks = [];
    let size = 0;
    for await (const chunk of stream) {
      size += chunk.length;
      if (size > 2 * 1024 * 1024) throw new Error('Mapping stream too large');
      chunks.push(chunk);
    }
    mappingHeader = Buffer.concat(chunks).toString('utf8').split('\n')[0];
  }
} finally {
  mappingZip.close();
}
if (mappingHeader !== 'v1\tofficial\tintermediary')
  throw new Error('Unexpected mapping namespaces');
const classes = [
  'org.objectweb.asm.ClassReader',
  'org.spongepowered.asm.launch.MixinBootstrap',
  'net.fabricmc.loader.api.FabricLoader',
  'net.fabricmc.loader.impl.launch.knot.KnotClient',
];
const source = join(root, 'SpaceLoaderSmoke.java');
await writeFile(
  source,
  `public class SpaceLoaderSmoke {
  public static void main(String[] args) throws Exception {
    String[] names = {${classes.map((name) => JSON.stringify(name)).join(',')}};
    for (String name : names) {
      Class.forName(name, false, Thread.currentThread().getContextClassLoader());
      System.out.println("loaded " + name);
    }
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
await execute(
  join(repository, '.tools/jdk-21/bin/javac.exe'),
  ['-proc:none', '-d', root, source],
  options,
);
const classpath = [
  root,
  ...LOADER_LIBRARIES.filter((artifact) => !artifact.native).map((artifact) =>
    join(root, artifact.relativePath),
  ),
].join(delimiter);
const execution = await execute(
  join(repository, '.tools/jdk-21/bin/java.exe'),
  ['-cp', classpath, 'SpaceLoaderSmoke'],
  options,
);
if (!classes.every((name) => execution.stdout.includes('loaded ' + name)))
  throw new Error('Missing loader classes');
const result = {
  checkedAt: new Date().toISOString(),
  version: '1.8.9',
  loaderVersion: '0.18.3',
  lwjglVersion: '2.9.4+legacyfabric.17',
  javaMajor: 21,
  artifactCount: LOADER_LIBRARIES.length,
  verifiedBytes: LOADER_LIBRARIES.reduce((sum, artifact) => sum + artifact.size, 0),
  classesLoadedWithoutInitialization: classes,
  mappingHeader,
  nativeFiles: extracted.files,
  note: 'Pinned artifacts verified by both digests. Class loading used initialize=false; loader was not started, DLLs not loaded, and Minecraft was not launched.',
};
await writeFile(join(root, 'result.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));
