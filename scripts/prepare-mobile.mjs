import { mkdir, copyFile, readFile, writeFile } from 'node:fs/promises';
import sharp from 'sharp';
import { join } from 'node:path';

await mkdir('www/icons', { recursive: true });
await copyFile('index.html', 'www/index.html');
await copyFile('manifest.webmanifest', 'www/manifest.webmanifest');
await copyFile('service-worker.js', 'www/service-worker.js');
await copyFile('icons/icon.svg', 'www/icons/icon.svg');

// Decode the approved artwork (including "المستشار ود كنانة") for Android launcher icons.
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

// Adaptive icon: keep the full square artwork inside the Android safe zone.
const adaptiveDir = 'android/app/src/main/res/mipmap-xxxhdpi';
const foreground = await sharp({
  create: { width: 432, height: 432, channels: 4, background: '#006b32' }
}).composite([{
  input: await sharp(source).resize(324, 324, { fit: 'contain' }).png().toBuffer(),
  gravity: 'centre'
}]).png().toBuffer();
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
const values = 'android/app/src/main/res/values';
await mkdir(values, { recursive: true });
await writeFile(join(values, 'ic_launcher_colors.xml'),
  '<resources><color name="ic_launcher_background">#006b32</color></resources>');
console.log('Hesabati web assets and approved Android launcher icons prepared.');
