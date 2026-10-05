#!/usr/bin/env node

/**
 * Apache RocketMQ 5 MCP 控制面服务端跨平台 Node.js 启动调度器。
 * 负责解析当前运行平台、寻址目标平台原生可执行二进制文件，
 * 并通过 stdio: inherit 启动子进程，守卫 Stdio 传输纯净性红线 (ADR 0003)。
 *
 * @author Ateng
 * @since 2026-10-05
 */

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const SUPPORTED_PLATFORMS = {
  'win32-x64': {
    pkg: '@atengk/mcp-server-rocketmq-win32-x64',
    binary: 'mcp-server-rocketmq.exe'
  },
  'linux-x64': {
    pkg: '@atengk/mcp-server-rocketmq-linux-x64',
    binary: 'mcp-server-rocketmq'
  },
  'darwin-arm64': {
    pkg: '@atengk/mcp-server-rocketmq-darwin-arm64',
    binary: 'mcp-server-rocketmq'
  }
};

/**
 * 获取当前平台的识别标识符。
 *
 * @param {string} [platform=process.platform] 操作系统标识
 * @param {string} [arch=process.arch] CPU 架构标识
 * @returns {string} 组合平台标识（例如 win32-x64）
 */
function getPlatformKey(platform = process.platform, arch = process.arch) {
  return `${platform}-${arch}`;
}

/**
 * 寻址指定平台对应的原生可执行文件物理路径。
 *
 * @param {string} platformKey 平台标识
 * @param {object} [options={}] 自定义查找参数
 * @returns {string|null} 解析出的可执行文件绝对路径，若未找到则返回 null
 */
function resolveBinaryPath(platformKey, options = {}) {
  const platformInfo = SUPPORTED_PLATFORMS[platformKey];
  if (!platformInfo) {
    return null;
  }

  const { pkg, binary } = platformInfo;
  const customSearchPaths = options.customSearchPaths || [];

  const candidates = [
    ...customSearchPaths,
    // 1. 从 Node.js 模块路径解析
    () => {
      try {
        const pkgJsonPath = require.resolve(`${pkg}/package.json`, { paths: [__dirname, process.cwd()] });
        return path.resolve(path.dirname(pkgJsonPath), 'bin', binary);
      } catch {
        return null;
      }
    },
    // 2. 从本地同级仓库目录结构中解析 (支持本地开发与测试场景)
    () => {
      const scopeSubDir = pkg.includes('/') ? pkg.split('/')[1] : pkg;
      const localDevPath = path.resolve(__dirname, '..', '..', scopeSubDir, 'bin', binary);
      return localDevPath;
    },
    // 3. 从当前工程目录的 node_modules 中直接推断
    () => {
      const localNodeModules = path.resolve(process.cwd(), 'node_modules', pkg, 'bin', binary);
      return localNodeModules;
    }
  ];

  for (const candidate of candidates) {
    const candidatePath = typeof candidate === 'function' ? candidate() : candidate;
    if (candidatePath && fs.existsSync(candidatePath)) {
      try {
        fs.accessSync(candidatePath, fs.constants.F_OK);
        return candidatePath;
      } catch {
        // 权限或访问异常则尝试后续候选
      }
    }
  }

  return null;
}

/**
 * 组装子进程启动参数，未指定时默认补齐 stdio 传输参数。
 *
 * @param {string[]} inputArgs 输入的原始命令行参数
 * @returns {string[]} 经规范化补充后的参数列表
 */
function buildArguments(inputArgs) {
  const args = [...inputArgs];
  const hasTransport = args.some(arg => arg.startsWith('--mcp.transport='));
  const hasPort = args.some(arg => arg.startsWith('--server.port='));

  // 默认在未指定网络端口且未指定传输协议时，注入 Stdio 传输参数
  if (!hasTransport && !hasPort) {
    args.push('--mcp.transport=stdio');
  }

  return args;
}

/**
 * 打印面向用户的结构化错误诊断至 stderr，杜绝标准输出污染。
 *
 * @param {string} title 错误标题
 * @param {string[]} details 详细说明信息列表
 */
function logErrorToStderr(title, details = []) {
  process.stderr.write(`\n[mcp-server-rocketmq] 错误: ${title}\n`);
  for (const detail of details) {
    process.stderr.write(`  - ${detail}\n`);
  }
  process.stderr.write('\n');
}

/**
 * 执行主调度逻辑。
 */
function run() {
  const currentKey = getPlatformKey();
  const platformInfo = SUPPORTED_PLATFORMS[currentKey];

  if (!platformInfo) {
    logErrorToStderr(`当前操作系统环境不受原生预编译二进制支持: ${currentKey}`, [
      `支持的操作系统平台与架构包括: ${Object.keys(SUPPORTED_PLATFORMS).join(', ')}`,
      '替代方案 1: 使用轻量 Docker 容器运行 (推荐):',
      '    docker run -i --rm -e MCP_ROCKETMQ_NAMESRV_ADDR="127.0.0.1:9876" ghcr.io/atengk/mcp-server-rocketmq:latest --mcp.transport=stdio',
      '替代方案 2: 使用预装 Java 21 运行标准 Fat Jar:',
      '    java -jar mcp-server-rocketmq.jar --mcp.transport=stdio'
    ]);
    process.exit(1);
  }

  const binaryPath = resolveBinaryPath(currentKey);
  if (!binaryPath) {
    logErrorToStderr(`未能找到平台原生可执行文件: ${platformInfo.binary}`, [
      `期望的平台安装包: ${platformInfo.pkg}`,
      '可能的原因: npm 安装时未能成功下载 optionalDependencies 平台包。',
      '请尝试重新安装: npm install -g @atengk/mcp-server-rocketmq',
      '或者使用 Docker 镜像执行: ghcr.io/atengk/mcp-server-rocketmq:latest'
    ]);
    process.exit(1);
  }

  const rawArgs = process.argv.slice(2);
  const finalArgs = buildArguments(rawArgs);

  // 启动子进程，继承标准输入输出流以保障 MCP 协议通道通畅
  const child = spawn(binaryPath, finalArgs, {
    stdio: 'inherit',
    windowsHide: true
  });

  // 转发进程终止信号以保证 RocketMQ 管理客户端连接优雅释放
  const forwardSignal = signal => {
    if (child && !child.killed) {
      child.kill(signal);
    }
  };

  process.on('SIGINT', () => forwardSignal('SIGINT'));
  process.on('SIGTERM', () => forwardSignal('SIGTERM'));

  child.on('error', err => {
    logErrorToStderr('启动原生二进制进程时发生底层系统调用错误', [
      `路径: ${binaryPath}`,
      `异常详情: ${err.message}`
    ]);
    process.exit(1);
  });

  child.on('exit', (code, signal) => {
    if (signal) {
      process.kill(process.pid, signal);
    } else {
      process.exit(code ?? 0);
    }
  });
}

// 仅在作为主模块直接执行时运行
if (require.main === module) {
  run();
}

module.exports = {
  SUPPORTED_PLATFORMS,
  getPlatformKey,
  resolveBinaryPath,
  buildArguments,
  logErrorToStderr,
  run
};
