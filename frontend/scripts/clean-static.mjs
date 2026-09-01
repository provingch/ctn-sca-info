import { readdir, rm } from 'node:fs/promises';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const staticDirectory = fileURLToPath(
  new URL('../../backend/src/main/resources/static/', import.meta.url),
);
const assetsDirectory = join(staticDirectory, 'assets');
const generatedExtensions = new Set(['.js', '.css', '.svg']);

await rm(join(staticDirectory, 'index.html'), { force: true });

try {
  const entries = await readdir(assetsDirectory, { withFileTypes: true });
  await Promise.all(
    entries
      .filter((entry) => entry.isFile() && generatedExtensions.has(extname(entry.name)))
      .map((entry) => rm(join(assetsDirectory, entry.name), { force: true })),
  );
} catch (error) {
  if (!(error instanceof Error && 'code' in error && error.code === 'ENOENT')) throw error;
}
