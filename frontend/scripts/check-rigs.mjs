import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
let failed=false;
for (const variant of ['male','female']) {
  const path=new URL(`../public/avatars/${variant}.riv`,import.meta.url);
  try {
    const bytes=await readFile(path);
    if (bytes.subarray(0,4).toString('ascii')!=='RIVE') throw new Error('Not a Rive runtime file (RIVE header missing)');
    console.log(`${variant}: ${bytes.length} bytes; sha256 ${createHash('sha256').update(bytes).digest('hex')}`);
  } catch (error) {
    failed=true;console.error(`${variant}: ${error.code==='ENOENT' ? 'Missing authored .riv export' : error.message}`);
  }
}
console.log('A file header check cannot verify state machines or smooth animation. Validate runtime playback in /avatar-lab.');
process.exitCode=failed ? 1 : 0;
