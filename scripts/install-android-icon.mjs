import { mkdir, readFile, writeFile } from 'node:fs/promises';
import sharp from 'sharp';
import { join } from 'node:path';

const encoded = (await readFile('assets/hesabati-icon.jpg.b64', 'utf8')).trim();
const source = Buffer.from(encoded, 'base64');
const sizes = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [density, size] of Object.entries(sizes)) {
  const dir = join('android/app/src/main/res', `mipmap-${density}`);
  await mkdir(dir, { recursive: true });
  const png = await sharp(source).resize(size, size, { fit: 'cover' }).png().toBuffer();
  await writeFile(join(dir, 'ic_launcher.png'), png);
  await writeFile(join(dir, 'ic_launcher_round.png'), png);
}
const adaptiveDir = 'android/app/src/main/res/mipmap-xxxhdpi';
const foreground = await sharp({ create: { width: 432, height: 432, channels: 4, background: '#006b32' } })
  .composite([{ input: await sharp(source).resize(324, 324, { fit: 'contain' }).png().toBuffer(), gravity: 'centre' }])
  .png().toBuffer();
await writeFile(join(adaptiveDir, 'ic_launcher_foreground.png'), foreground);
const anydpi = 'android/app/src/main/res/mipmap-anydpi-v26';
await mkdir(anydpi, { recursive: true });
const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>`;
await writeFile(join(anydpi, 'ic_launcher.xml'), adaptiveXml);
await writeFile(join(anydpi, 'ic_launcher_round.xml'), adaptiveXml);
console.log('Approved Hesabati launcher icons installed.');
