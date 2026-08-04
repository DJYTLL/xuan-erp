import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const viewsRoot = path.join(root, 'src/views');

const legacyRawNumberInputPages = new Set([
  'TenantManagementView.vue',
]);

function readVueFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const absolutePath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return readVueFiles(absolutePath);
    }
    return entry.name.endsWith('.vue') ? [absolutePath] : [];
  });
}

for (const file of readVueFiles(viewsRoot)) {
  const fileName = path.basename(file);
  if (legacyRawNumberInputPages.has(fileName)) {
    continue;
  }
  const source = fs.readFileSync(file, 'utf8');
  if (source.includes('<el-input-number')) {
    throw new Error(`${path.relative(root, file)} should use XuanDecimalInput from the component center instead of raw el-input-number.`);
  }
}

console.log('Verified pages prefer component-center numeric input components.');
