import { mkdir, copyFile } from 'node:fs/promises';
await mkdir('www/icons', { recursive: true });
await copyFile('index.html', 'www/index.html');
await copyFile('manifest.webmanifest', 'www/manifest.webmanifest');
await copyFile('service-worker.js', 'www/service-worker.js');
await copyFile('icons/icon.svg', 'www/icons/icon.svg');
console.log('Hesabati mobile web assets prepared.');
