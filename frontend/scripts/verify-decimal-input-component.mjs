import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function assertFile(relativePath) {
  if (!existsSync(resolve(frontendRoot, relativePath))) {
    throw new Error(`Missing file: ${relativePath}`);
  }
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

assertFile('src/framework/components/XuanDecimalInput.vue');

const packageSource = read('package.json');
const inputSource = read('src/framework/components/XuanDecimalInput.vue');
const componentCenterSource = read('src/views/ComponentCenterView.vue');

assertIncludes(packageSource, 'verify:decimal-input-component', 'package.json should expose decimal input verification.');
assertIncludes(inputSource, 'normalizeDecimalInput', 'Decimal input should normalize typed decimal values.');
assertIncludes(inputSource, 'allowNegative', 'Decimal input should support negative values for adjustment scenarios.');
assertIncludes(inputSource, 'scale', 'Decimal input should support precision scale.');
assertIncludes(inputSource, "inputMode", 'Decimal input should set mobile keyboard inputMode.');
assertIncludes(componentCenterSource, 'XuanDecimalInput', 'Component center should preview XuanDecimalInput.');
assertIncludes(componentCenterSource, 'decimalInputDemo', 'Component center should hold decimal input demo state.');
assertIncludes(componentCenterSource, '数字输入框', 'Component center should include a decimal input panel.');

console.log('Decimal input component verification passed.');
