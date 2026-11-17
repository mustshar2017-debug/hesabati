import { mkdir, readFile, writeFile } from 'node:fs/promises';
import sharp from 'sharp';
import { join } from 'node:path';

const encoded = (await readFile('assets/hesabati-icon.jpg.b64', 'utf8')).trim();
const source = Buffer.from(encoded, 'base64');
const root = 'android/app/src/main/res';
const sizes = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
const adaptiveSizes = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 };

for (const [density, size] of Object.entries(sizes)) {
  const dir = join(root, `mipmap-${density}`);
  await mkdir(dir, { recursive: true });
  const png = await sharp(source).resize(size, size, { fit: 'contain', background: '#006b32' }).png().toBuffer();
  await writeFile(join(dir, 'ic_launcher.png'), png);
  await writeFile(join(dir, 'ic_launcher_round.png'), png);
  const adaptiveSize = adaptiveSizes[density];
  const foreground = await sharp({ create: { width: adaptiveSize, height: adaptiveSize, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([{ input: await sharp(source).resize(Math.round(adaptiveSize * 0.76), Math.round(adaptiveSize * 0.76), { fit: 'contain' }).png().toBuffer(), gravity: 'centre' }])
    .png().toBuffer();
  await writeFile(join(dir, 'ic_launcher_foreground.png'), foreground);
}
const anydpi = join(root, 'mipmap-anydpi-v26');
await mkdir(anydpi, { recursive: true });
const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>`;
await writeFile(join(anydpi, 'ic_launcher.xml'), adaptiveXml);
await writeFile(join(anydpi, 'ic_launcher_round.xml'), adaptiveXml);
console.log('Installed official Hesabati launcher icon in all Android densities.');
