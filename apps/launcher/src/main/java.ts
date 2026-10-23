import { execFile } from 'node:child_process';
import { stat } from 'node:fs/promises';
import type { JavaInfo } from '../shared/contracts.ts';
import { javaPath } from './validation.ts';

export function parseJava(output: string): JavaInfo {
  const version = output.match(/java\.specification\.version\s*=\s*(\S+)/)?.[1];
  const major = version ? Number(version.startsWith('1.') ? version.slice(2) : version) : null;
  const bits = Number(output.match(/sun\.arch\.data\.model\s*=\s*(\d+)/)?.[1]) || null;
  if (!major || !bits)
    return {
      usable: false,
      major: null,
      bits: null,
      message: 'Не удалось определить версию Java. Выберите другой java.exe.',
    };
  const usable = major === 21 && bits === 64;
  return {
    usable,
    major,
    bits,
    message: usable
      ? 'Java 21 · 64 бита. Проверка пройдена.'
      : `Найдена Java ${major} · ${bits} бита. Для текущей сборки выберите Java 21 (64 бита).`,
  };
}
export async function inspectJava(path: string | null): Promise<JavaInfo> {
  if (!path) throw new Error('Сначала выберите файл java.exe.');
  const selected = javaPath(path);
  try {
    if (!(await stat(selected)).isFile()) throw new Error();
  } catch {
    throw new Error('Java не найдена по сохранённому пути. Выберите файл заново.');
  }
  const environment = { ...process.env };
  for (const name of Object.keys(environment))
    if (
      ['JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'CLASSPATH'].includes(
        name.toUpperCase(),
      )
    )
      delete environment[name];
  return new Promise((resolve, reject) => {
    execFile(
      selected,
      ['-XshowSettings:properties', '-version'],
      { timeout: 5000, maxBuffer: 65536, windowsHide: true, shell: false, env: environment },
      (error, stdout, stderr) => {
        if (error)
          reject(
            new Error(
              'Java не ответила на проверку. Проверьте установку или выберите другой java.exe.',
            ),
          );
        else resolve(parseJava(stdout + '\n' + stderr));
      },
    );
  });
}
