import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8');
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

const httpSource = read('src/api/http.ts');
const httpErrorSource = read('src/api/http-error.ts');
const mainSource = read('src/main.ts');
const messagesSource = read('src/i18n/messages.ts');

assertIncludes(httpSource, "'Accept-Language'", 'HTTP requests should send the current locale with Accept-Language.');
assertIncludes(httpSource, 'getCurrentLocale', 'HTTP layer should read the current frontend locale.');
assertIncludes(httpErrorSource, 'translateErrorCode', 'HTTP errors should support frontend translation by backend error code.');
assertIncludes(httpErrorSource, 'getHttpErrorCode', 'HTTP errors should extract backend response code.');
assertIncludes(mainSource, 'translateApiErrorCode', 'main.ts should install API error-code translation.');
assertIncludes(messagesSource, 'apiError:', 'i18n messages should include API error translations.');
assertIncludes(messagesSource, 'IAM_UNAUTHORIZED', 'API error translations should cover IAM_UNAUTHORIZED.');

console.log('Verified backend i18n contract: Accept-Language header and API error-code translation.');
