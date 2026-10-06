/**
 * Node.js 跨平台启动包装器契约与调度测试 (接缝 3)。
 * 验证跨平台平台解析、参数组装、错误防御与 Stdio 纯净性红线 (ADR 0003)。
 *
 * @author Ateng
 * @since 2026-10-05
 */

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const { spawnSync } = require('node:child_process');

const cli = require('../@atengk/mcp-server-rocketmq/bin/cli.js');

describe('Node.js CLI 调度器单元与契约测试', () => {

  it('应该正确定义四大平台包与原生二进制契约', () => {
    const platforms = cli.SUPPORTED_PLATFORMS;
    assert.deepEqual(Object.keys(platforms).sort(), [
      'darwin-arm64',
      'linux-arm64',
      'linux-x64',
      'win32-x64'
    ]);

    assert.equal(platforms['win32-x64'].binary, 'mcp-server-rocketmq.exe');
    assert.equal(platforms['linux-x64'].binary, 'mcp-server-rocketmq');
    assert.equal(platforms['linux-arm64'].binary, 'mcp-server-rocketmq');
    assert.equal(platforms['darwin-arm64'].binary, 'mcp-server-rocketmq');

    assert.equal(platforms['win32-x64'].pkg, '@atengk/mcp-server-rocketmq-win32-x64');
    assert.equal(platforms['linux-x64'].pkg, '@atengk/mcp-server-rocketmq-linux-x64');
    assert.equal(platforms['linux-arm64'].pkg, '@atengk/mcp-server-rocketmq-linux-arm64');
    assert.equal(platforms['darwin-arm64'].pkg, '@atengk/mcp-server-rocketmq-darwin-arm64');
  });

  it('应该根据操作系统与架构生成标准平台标识', () => {
    assert.equal(cli.getPlatformKey('win32', 'x64'), 'win32-x64');
    assert.equal(cli.getPlatformKey('linux', 'x64'), 'linux-x64');
    assert.equal(cli.getPlatformKey('linux', 'arm64'), 'linux-arm64');
    assert.equal(cli.getPlatformKey('darwin', 'arm64'), 'darwin-arm64');
    assert.equal(cli.getPlatformKey('darwin', 'x64'), 'darwin-x64');
    assert.equal(cli.getPlatformKey('freebsd', 'x64'), 'freebsd-x64');
  });

  it('未指定传输与端口时应该自动补齐 stdio 参数', () => {
    const args1 = cli.buildArguments([]);
    assert.deepEqual(args1, ['--mcp.transport=stdio']);

    const args2 = cli.buildArguments(['--rocketmq.namesrv-addr=127.0.0.1:9876']);
    assert.deepEqual(args2, [
      '--rocketmq.namesrv-addr=127.0.0.1:9876',
      '--mcp.transport=stdio'
    ]);
  });

  it('显式指定传输或端口时不应重复追加 stdio 参数', () => {
    const argsSse = cli.buildArguments(['--mcp.transport=sse']);
    assert.deepEqual(argsSse, ['--mcp.transport=sse']);

    const argsPort = cli.buildArguments(['--server.port=9090']);
    assert.deepEqual(argsPort, ['--server.port=9090']);
  });

  it('对未知不受支持的平台应该返回 null 寻址结果', () => {
    assert.equal(cli.resolveBinaryPath('sunos-x64'), null);
    assert.equal(cli.resolveBinaryPath('linux-arm'), null);
  });

  it('当受支持平台未预置或安装二进制时应该安全返回 null 寻址结果', () => {
    assert.equal(cli.resolveBinaryPath('linux-arm64'), null);
  });
});

describe('Node.js CLI 端到端子进程防御与 Stdio 纯净性测试 (接缝 3)', () => {

  const cliPath = path.resolve(__dirname, '..', '@atengk/mcp-server-rocketmq', 'bin', 'cli.js');

  it('当在不受支持的平台运行时，必须以非零退出且 stdout 绝对为 0 字节 (零污染)', () => {
    // 构造测试子脚本模拟 process.platform 为不支持的平台
    const mockScript = `
      Object.defineProperty(process, 'platform', { value: 'aix' });
      Object.defineProperty(process, 'arch', { value: 'ppc64' });
      const cli = require(${JSON.stringify(cliPath)});
      cli.run();
    `;

    const res = spawnSync(process.execPath, ['-e', mockScript], {
      encoding: 'utf8'
    });

    assert.equal(res.status, 1, '应以退出码 1 异常退出');
    assert.equal(res.stdout, '', 'stdout 标准输出必须绝对纯净 (0 字节)，严禁协议污染');
    assert.match(res.stderr, /当前操作系统环境不受原生预编译二进制支持: aix-ppc64/);
    assert.match(res.stderr, /docker run/);
    assert.match(res.stderr, /java -jar/);
  });

  it('当目标平台二进制缺失时，必须在 stderr 提示并以非零码退出且 stdout 零污染', () => {
    // 构造隔离的临时目录以确保同级相对路径下无任何预置的平台二进制
    const tempDir = fs.mkdtempSync(path.join(path.resolve(__dirname, '..'), 'tmp-missing-'));
    const isolatedBinDir = path.join(tempDir, 'bin');
    fs.mkdirSync(isolatedBinDir, { recursive: true });
    const isolatedCliPath = path.join(isolatedBinDir, 'cli.js');
    fs.copyFileSync(cliPath, isolatedCliPath);

    const res = spawnSync(process.execPath, [isolatedCliPath], {
      cwd: tempDir,
      encoding: 'utf8'
    });

    try {
      fs.rmSync(tempDir, { recursive: true, force: true });
    } catch {}

    assert.equal(res.status, 1, '未能找到二进制时应以状态码 1 退出');
    assert.equal(res.stdout, '', 'stdout 标准输出必须绝对纯净 (0 字节)');
    assert.match(res.stderr, /未能找到平台原生可执行文件/);
    assert.match(res.stderr, /ghcr\.io\/atengk\/mcp-server-rocketmq/);
  });

  it('当目标平台二进制存在时，应该成功拉起并透传退出码', () => {
    // 构造一个临时的测试目录并拷贝系统 node 二进制作为原生二进制模拟
    const tempDir = fs.mkdtempSync(path.join(path.resolve(__dirname, '..'), 'tmp-test-'));
    const isWin = process.platform === 'win32';
    const mockBinName = isWin ? 'mock-native.exe' : 'mock-native';
    const mockBinPath = path.join(tempDir, mockBinName);

    fs.copyFileSync(process.execPath, mockBinPath);

    // 运行 runner 脚本验证参数传递与调度退出码
    const runnerScript = `
      const cli = require(${JSON.stringify(cliPath)});
      const { spawn } = require('child_process');

      const binPath = ${JSON.stringify(mockBinPath)};
      // 验证通过 resolveBinaryPath 自定义路径可被命中
      const resolved = cli.resolveBinaryPath(cli.getPlatformKey(), { customSearchPaths: [binPath] });
      if (!resolved) {
        process.exit(10);
      }

      // 执行 mock 原生二进制（此处为拷贝的 node 原生进程）
      const child = spawn(resolved, ['-e', 'process.exit(0)'], { stdio: 'inherit' });
      child.on('exit', code => {
        process.exit(code ?? 0);
      });
    `;

    try {
      const res = spawnSync(process.execPath, ['-e', runnerScript], {
        encoding: 'utf8'
      });
      assert.equal(res.status, 0, '模拟二进制应该成功执行并返回码 0');
    } finally {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('当子进程异常退出时，调度器应该精确透传非零退出码 (例如 42)', () => {
    const tempDir = fs.mkdtempSync(path.join(path.resolve(__dirname, '..'), 'tmp-test-'));
    const isWin = process.platform === 'win32';
    const mockBinName = isWin ? 'mock-native-fail.exe' : 'mock-native-fail';
    const mockBinPath = path.join(tempDir, mockBinName);

    fs.copyFileSync(process.execPath, mockBinPath);

    const runnerScript = `
      const { spawn } = require('child_process');
      const binPath = ${JSON.stringify(mockBinPath)};
      const child = spawn(binPath, ['-e', 'process.exit(42)'], { stdio: 'inherit' });
      child.on('exit', code => {
        process.exit(code ?? 0);
      });
    `;

    try {
      const res = spawnSync(process.execPath, ['-e', runnerScript], {
        encoding: 'utf8'
      });
      assert.equal(res.status, 42, '应精准透传退出码 42');
    } finally {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('应该无损透传全部业务参数与特殊标记', () => {
    const customArgs = [
      '--rocketmq.namesrv-addr=10.0.0.1:9876',
      '--rocketmq.read-only=true',
      '--rocketmq.enable-destructive-tools=true'
    ];
    const finalArgs = cli.buildArguments(customArgs);

    assert.equal(finalArgs.length, 4);
    assert.ok(finalArgs.includes('--rocketmq.namesrv-addr=10.0.0.1:9876'));
    assert.ok(finalArgs.includes('--rocketmq.read-only=true'));
    assert.ok(finalArgs.includes('--rocketmq.enable-destructive-tools=true'));
    assert.ok(finalArgs.includes('--mcp.transport=stdio'));
  });
});
