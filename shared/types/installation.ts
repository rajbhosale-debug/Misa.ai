// Shared types and constants for MISA.AI installation and setup
// Used across web, desktop, and mobile platforms

export interface InstallationConfig {
  version: string;
  platform: Platform;
  architecture: Architecture;
  installPath: string;
  silentMode: boolean;
  autoPrerequisites: boolean;
  backgroundInstall: boolean;
  progressCallback?: (progress: number, message: string) => void;
}

export interface DeviceInfo {
  id: string;
  name: string;
  type: DeviceType;
  platform: Platform;
  architecture: Architecture;
  capabilities: DeviceCapability[];
  address?: string;
  port?: number;
  status: DeviceStatus;
  lastSeen: Date;
  batteryLevel?: number;
  connectionStrength?: number;
}

export interface DiscoverySession {
  sessionId: string;
  deviceId: string;
  startedAt: Date;
  qrToken: string;
  pairingStatus: PairingStatus;
  autoPairEnabled: boolean;
  connectionStrength: number;
  timeout?: Date;
}

export interface DeviceHistory {
  deviceId: string;
  lastConnected: Date;
  connectionCount: number;
  averageSignalStrength: number;
  successRate: number;
  preferredForTasks: string[];
  deviceType: DeviceType;
  trustLevel: TrustLevel;
}

export interface ConnectionQuality {
  deviceId: string;
  latencyMs: number;
  bandwidthMbps: number;
  signalStrength: number;
  stabilityScore: number;
  lastUpdated: Date;
  uptimePercentage: number;
  packetLoss?: number;
}

export interface QualityMeasurement {
  deviceId: string;
  timestamp: Date;
  latencyMs: number;
  packetLoss: number;
  jitterMs: number;
}

export interface ExpressSetupData {
  deviceName: string;
  privacy: PrivacyConfig;
  device: DeviceConfig;
  applications: ApplicationConfig;
  detectedDevices: DeviceInfo[];
  setupCompletedAt: Date;
}

export interface PrivacyConfig {
  dataCollection: boolean;
  crashReports: boolean;
  analytics: boolean;
  localOnlyProcessing: boolean;
  telemetryEnabled: boolean;
}

export interface DeviceConfig {
  deviceName: string;
  enableSync: boolean;
  remoteDesktop: boolean;
  autoDiscovery: boolean;
  maxDevices: number;
  autoPairing: boolean;
}

export interface ApplicationConfig {
  enabledApps: string[];
  defaultModel: string;
  voiceEnabled: boolean;
  autoDownloadModels: boolean;
  theme: 'light' | 'dark' | 'auto';
}

export interface InstallationProgress {
  currentStep: number;
  totalSteps: number;
  message: string;
  percentage: number;
  timestamp: Date;
}

export interface InstallationState {
  status: 'started' | 'in_progress' | 'completed' | 'failed' | 'rolled_back';
  step: string;
  timestamp: Date;
  error?: string;
  rollbackAvailable: boolean;
}

export interface SystemTrayConfig {
  enabled: boolean;
  minimizeToTray: boolean;
  startMinimized: boolean;
  showNotifications: boolean;
  quickActions: QuickAction[];
}

export interface QuickAction {
  id: string;
  label: string;
  icon: string;
  action: string;
  enabled: boolean;
}

export interface NotificationConfig {
  enabled: boolean;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  actions?: NotificationAction[];
}

export interface NotificationAction {
  id: string;
  label: string;
  action: string;
  primary?: boolean;
}

// Enums
export enum Platform {
  WINDOWS = 'windows',
  MACOS = 'macos',
  LINUX = 'linux',
  ANDROID = 'android',
  IOS = 'ios'
}

export enum Architecture {
  X64 = 'x64',
  X86 = 'x86',
  ARM64 = 'arm64',
  ARM = 'arm'
}

export enum DeviceType {
  DESKTOP = 'desktop',
  LAPTOP = 'laptop',
  PHONE = 'phone',
  TABLET = 'tablet',
  SERVER = 'server'
}

export enum DeviceCapability {
  GPU = 'gpu',
  VISION = 'vision',
  AUDIO = 'audio',
  REMOTE_DESKTOP = 'remote_desktop',
  BACKGROUND_DISCOVERY = 'background_discovery',
  VOICE_CONTROL = 'voice_control',
  CAMERA = 'camera',
  SENSORS = 'sensors',
  NFC = 'nfc',
  BLUETOOTH = 'bluetooth',
  WIFI = 'wifi',
  GPS = 'gps'
}

export enum DeviceStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
  BUSY = 'busy',
  CONNECTING = 'connecting',
  PAIRED = 'paired',
  FAILED = 'failed'
}

export enum PairingStatus {
  INITIATED = 'initiated',
  PENDING_CONFIRMATION = 'pending_confirmation',
  PAIRING = 'pairing',
  COMPLETED = 'completed',
  FAILED = 'failed',
  CANCELLED = 'cancelled'
}

export enum TrustLevel {
  UNKNOWN = 'unknown',
  UNTRUSTED = 'untrusted',
  KNOWN = 'known',
  TRUSTED = 'trusted',
  VERIFIED = 'verified'
}

export enum NotificationType {
  INFO = 'info',
  SUCCESS = 'success',
  WARNING = 'warning',
  ERROR = 'error',
  DEVICE_CONNECTED = 'device_connected',
  DEVICE_DISCONNECTED = 'device_disconnected',
  UPDATE_AVAILABLE = 'update_available',
  PAIRING_REQUEST = 'pairing_request'
}

export enum InstallationMode {
  INTERACTIVE = 'interactive',
  SILENT = 'silent',
  BACKGROUND = 'background',
  EXPRESS = 'express'
}

// Constants
export const INSTALLATION_CONSTANTS = {
  // Network configuration
  DISCOVERY_PORT: 8081,
  KERNEL_PORT: 8080,
  WEB_PORT: 3000,
  MULTICAST_ADDRESS: '239.255.0.1',
  BROADCAST_INTERVAL: 30000, // 30 seconds
  QR_EXPIRY: 300000, // 5 minutes in milliseconds

  // Installation paths
  DEFAULT_INSTALL_PATHS: {
    [Platform.WINDOWS]: '%PROGRAMFILES%\\MISA.AI',
    [Platform.MACOS]: '/Applications/MISA.AI',
    [Platform.LINUX]: '~/.misa-ai',
    [Platform.ANDROID]: '/data/data/com.misa.ai',
    [Platform.IOS]: '/var/mobile/Containers/Data/Application/com.misa.ai'
  },

  // Configuration defaults
  DEFAULT_PRIVACY_CONFIG: {
    dataCollection: false,
    crashReports: false,
    analytics: false,
    localOnlyProcessing: true,
    telemetryEnabled: false
  } as PrivacyConfig,

  DEFAULT_DEVICE_CONFIG: {
    deviceName: 'MISA Device',
    enableSync: true,
    remoteDesktop: true,
    autoDiscovery: true,
    maxDevices: 10,
    autoPairing: true
  } as DeviceConfig,

  DEFAULT_APPLICATION_CONFIG: {
    enabledApps: ['notes', 'tasks', 'calendar'],
    defaultModel: 'mixtral',
    voiceEnabled: true,
    autoDownloadModels: true,
    theme: 'auto'
  } as ApplicationConfig,

  // System tray defaults
  DEFAULT_QUICK_ACTIONS: [
    { id: 'open', label: 'Open', icon: 'home', action: 'open', enabled: true },
    { id: 'voice', label: 'Voice', icon: 'mic', action: 'toggle-mic', enabled: true },
    { id: 'mute', label: 'Sound', icon: 'volume', action: 'toggle-mute', enabled: true },
    { id: 'notifications', label: 'Alerts', icon: 'bell', action: 'toggle-notifications', enabled: true },
    { id: 'status', label: 'Status', icon: 'refresh', action: 'set-status', enabled: true },
    { id: 'minimize', label: 'Hide', icon: 'minimize', action: 'toggle-minimize', enabled: true }
  ] as QuickAction[],

  // Quality thresholds
  QUALITY_THRESHOLDS: {
    EXCELLENT: 0.9,
    GOOD: 0.7,
    FAIR: 0.5,
    POOR: 0.3
  },

  // Battery optimization
  SCAN_INTERVALS: {
    ACTIVE: 10000,    // 10 seconds when active
    BACKGROUND: 30000, // 30 seconds when background
    POWER_SAVE: 60000 // 1 minute when power saving
  },

  // Auto-pairing criteria
  AUTO_PAIR_CRITERIA: {
    MIN_SUCCESS_RATE: 0.8,
    MIN_CONNECTION_COUNT: 3,
    MAX_TIME_SINCE_LAST_SEEN: 7 * 24 * 60 * 60 * 1000, // 7 days in milliseconds
    TRUSTED_CAPABILITIES: [DeviceCapability.TRUSTED_SOURCE]
  }
};

// Utility functions
export function detectPlatform(): Platform {
  if (typeof window !== 'undefined') {
    const userAgent = navigator.userAgent;
    if (/android/i.test(userAgent)) return Platform.ANDROID;
    if (/iphone|ipad|ipod/i.test(userAgent)) return Platform.IOS;
    if (/win/i.test(userAgent)) return Platform.WINDOWS;
    if (/mac/i.test(userAgent)) return Platform.MACOS;
    if (/linux/i.test(userAgent)) return Platform.LINUX;
  }
  return Platform.LINUX; // Default
}

export function detectArchitecture(): Architecture {
  if (typeof navigator !== 'undefined') {
    const userAgent = navigator.userAgent;
    if (/arm64|aarch64/i.test(userAgent)) return Architecture.ARM64;
    if (/arm/i.test(userAgent)) return Architecture.ARM;
    if (/x64|x86_64|win64|wows64/i.test(userAgent)) return Architecture.X64;
    if (/x86|i386|i686/i.test(userAgent)) return Architecture.X86;
  }
  return Architecture.X64; // Default
}

export function generateDeviceId(): string {
  const platform = detectPlatform();
  const timestamp = Date.now();
  const random = Math.random().toString(36).substr(2, 9);
  return `${platform}-${timestamp}-${random}`;
}

export function generateQRToken(deviceId: string, deviceName: string): string {
  const timestamp = Date.now();
  const signature = generateSignature(deviceId, timestamp);
  return `misa://pair/${deviceId}/${timestamp}/${signature}/${encodeURIComponent(deviceName)}`;
}

export function generateSignature(deviceId: string, timestamp: number): string {
  const data = `${deviceId}:${timestamp}`;
  let hash = 0;
  for (let i = 0; i < data.length; i++) {
    const char = data.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  return Math.abs(hash).toString(16);
}

export function parseQRToken(qrToken: string): { deviceId: string; timestamp: number; signature: string; deviceName: string } | null {
  const match = qrToken.match(/^misa:\/\/pair\/([^\/]+)\/([^\/]+)\/([^\/]+)\/(.*)$/);
  if (!match) return null;

  const [, deviceId, timestampStr, signature, deviceName] = match;
  return {
    deviceId,
    timestamp: parseInt(timestampStr),
    signature,
    deviceName: decodeURIComponent(deviceName)
  };
}

export function isQRTokenValid(parsedToken: { timestamp: number }, maxAge: number = 5 * 60 * 1000): boolean {
  const now = Date.now();
  const tokenAge = now - parsedToken.timestamp;
  return tokenAge <= maxAge;
}

export function estimateSignalStrength(address: string): number {
  // Simple signal strength estimation based on address type
  if (address.startsWith('127.') || address.startsWith('localhost')) {
    return 1.0; // Loopback - excellent
  } else if (address.startsWith('192.168.') || address.startsWith('10.') || address.startsWith('172.')) {
    return 0.8; // Private network - good
  } else if (address.startsWith('169.254.')) {
    return 0.6; // Link-local - fair
  } else {
    return 0.5; // Public - fair to poor
  }
}

export function shouldAutoPair(device: DeviceInfo, history: DeviceHistory): boolean {
  // Check if device meets auto-pairing criteria
  if (history.successRate >= INSTALLATION_CONSTANTS.AUTO_PAIR_CRITERIA.MIN_SUCCESS_RATE &&
      history.connectionCount >= INSTALLATION_CONSTANTS.AUTO_PAIR_CRITERIA.MIN_CONNECTION_COUNT) {
    return true;
  }

  // Check if device has trusted capabilities
  const hasTrustedCapability = device.capabilities.some(cap =>
    INSTALLATION_CONSTANTS.AUTO_PAIR_CRITERIA.TRUSTED_CAPABILITIES.includes(cap)
  );

  return hasTrustedCapability && history.trustLevel === TrustLevel.TRUSTED;
}

export function calculateConnectionQuality(measurements: QualityMeasurement[]): number {
  if (measurements.length === 0) return 0;

  const avgLatency = measurements.reduce((sum, m) => sum + m.latencyMs, 0) / measurements.length;
  const avgPacketLoss = measurements.reduce((sum, m) => sum + m.packetLoss, 0) / measurements.length;
  const avgJitter = measurements.reduce((sum, m) => sum + m.jitterMs, 0) / measurements.length;

  // Calculate quality score (0-1)
  let score = 1.0;

  // Penalty for high latency
  if (avgLatency > 200) score -= 0.3;
  else if (avgLatency > 100) score -= 0.2;
  else if (avgLatency > 50) score -= 0.1;

  // Penalty for packet loss
  if (avgPacketLoss > 5) score -= 0.4;
  else if (avgPacketLoss > 2) score -= 0.2;
  else if (avgPacketLoss > 1) score -= 0.1;

  // Penalty for high jitter
  if (avgJitter > 50) score -= 0.2;
  else if (avgJitter > 20) score -= 0.1;

  return Math.max(0, Math.min(1, score));
}

export function formatFileSize(bytes: number): string {
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) return '0 B';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${Math.round(bytes / Math.pow(1024, i) * 100) / 100} ${sizes[i]}`;
}

export function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
}