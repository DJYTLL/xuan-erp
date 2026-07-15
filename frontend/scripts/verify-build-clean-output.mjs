import { spawnSync } from 'node:child_process';

const npmExecPath = process.env.npm_execpath;
const command = npmExecPath ? process.execPath : process.platform === 'win32' ? 'npm.cmd' : 'npm';
const args = npmExecPath ? [npmExecPath, 'run', 'build'] : ['run', 'build'];
const result = spawnSync(command, args, {
  cwd: process.cwd(),
  encoding: 'utf8',
  env: {
    ...process.env,
    npm_config_update_notifier: 'false',
    NO_UPDATE_NOTIFIER: '1',
  },
});

const output = stripAnsi(`${result.stdout || ''}\n${result.stderr || ''}`);
const forbiddenWarnings = [
  {
    marker: 'Some chunks are larger than',
    message: 'Vite build should not emit large chunk warnings.',
  },
  {
    marker: 'contains an annotation that Rollup cannot interpret',
    message: 'Vite build should not emit third-party pure annotation warnings.',
  },
  {
    marker: '/* #__PURE__ */',
    message: 'Vite build should not expose unresolved pure annotation warnings.',
  },
  {
    marker: 'Circular chunk:',
    message: 'Vite build should not emit circular manual chunk warnings.',
  },
];

if (result.error) {
  throw result.error;
}

if (result.status !== 0) {
  process.stdout.write(output);
  process.exit(result.status || 1);
}

for (const warning of forbiddenWarnings) {
  if (output.includes(warning.marker)) {
    throw new Error(warning.message);
  }
}

console.log('Verified Vite build output is clean: no large chunk or pure annotation warnings.');

function stripAnsi(value) {
  return value.replace(/\u001b\[[0-9;]*m/g, '');
}
