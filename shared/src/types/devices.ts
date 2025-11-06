/**
 * Device and Remote Control Types
 *
 * Types related to device management, remote desktop,
 * device discovery, and cross-device communication.
 */

import { z } from 'zod';

// ============================================================================
// Device Discovery and Pairing
// ============================================================================

/**
 * Device discovery request
 */
export interface DeviceDiscoveryRequest {
  enabled: boolean;
  scanInterval: number; // seconds
  broadcastEnabled: boolean;
  filterCriteria?: DeviceFilterCriteria;
}

export interface DeviceFilterCriteria {
  deviceTypes?: DeviceType[];
  platforms?: string[];
  capabilities?: string[];
  maxDistance?: number; // meters
}

/**
 * Pairing request and response
 */
export interface PairingRequest {
  method: PairingMethod;
  qrToken?: string;
  deviceId?: string;
  pairingCode?: string;
  biometricData?: any;
}

export interface PairingResponse {
  success: boolean;
  deviceId?: string;
  deviceName?: string;
  requiresConfirmation?: boolean;
  expiresAt?: Date;
  error?: string;
}

export type PairingMethod = 'qr_code' | 'nfc' | 'bluetooth' | 'manual' | 'biometric';

/**
 * Device trust level
 */
export interface DeviceTrustLevel {
  level: TrustLevel;
  verified: boolean;
  trustedAt?: Date;
  expiresAt?: Date;
  restrictions: DeviceRestrictions;
}

export type TrustLevel = 'unknown' | 'untrusted' | 'limited' | 'trusted' | 'managed';

export interface DeviceRestrictions {
  allowRemoteControl: boolean;
  allowFileTransfer: boolean;
  allowScreenCapture: boolean;
  allowSystemCommands: boolean;
  dataAccessLimit: DataAccessLevel;
}

export type DataAccessLevel = 'none' | 'basic' | 'standard' | 'full';

// ============================================================================
// Remote Desktop
// ============================================================================

/**
 * Remote desktop session
 */
export interface RemoteDesktopSession {
  sessionId: string;
  hostDeviceId: string;
  clientDeviceId: string;
  protocol: RemoteDesktopProtocol;
  resolution: ScreenResolution;
  quality: VideoQuality;
  permissions: RemoteDesktopPermissions;
  status: SessionStatus;
  startedAt: Date;
  lastActivity: Date;
  duration: number; // seconds
  screenRecording: boolean;
  recordingPath?: string;
}

export type RemoteDesktopProtocol = 'vnc' | 'rdp' | 'webrtc' | 'custom';

export interface ScreenResolution {
  width: number;
  height: number;
  refreshRate?: number;
  colorDepth?: number;
}

export type VideoQuality = 'low' | 'medium' | 'high' | 'ultra';

export interface RemoteDesktopPermissions {
  viewScreen: boolean;
  controlMouse: boolean;
  controlKeyboard: boolean;
  transferFiles: boolean;
  accessClipboard: boolean;
  recordSession: boolean;
  systemCommands: boolean;
  audioAccess: boolean;
  microphoneAccess: boolean;
}

export type SessionStatus = 'connecting' | 'connected' | 'disconnected' | 'error' | 'paused';

/**
 * Remote desktop control command
 */
export interface RemoteDesktopCommand {
  sessionId: string;
  type: CommandType;
  parameters: Record<string, any>;
  timestamp: Date;
}

export type CommandType =
  | 'mouse_move'
  | 'mouse_click'
  | 'mouse_scroll'
  | 'key_press'
  | 'key_release'
  | 'text_input'
  | 'clipboard_copy'
  | 'clipboard_paste'
  | 'file_upload'
  | 'file_download'
  | 'screenshot'
  | 'audio_toggle'
  | 'video_quality';

// ============================================================================
// File Transfer
// ============================================================================

/**
 * File transfer request
 */
export interface FileTransferRequest {
  sourceDeviceId: string;
  targetDeviceId: string;
  filePath: string;
  fileName?: string;
  encryptionEnabled: boolean;
  compressionEnabled: boolean;
  resumeSupported: boolean;
}

/**
 * File transfer status
 */
export interface FileTransferStatus {
  transferId: string;
  sourceDeviceId: string;
  targetDeviceId: string;
  fileName: string;
  fileSize: number;
  bytesTransferred: number;
  transferRate: number; // bytes per second
  status: TransferStatus;
  startedAt: Date;
  estimatedCompletion?: Date;
  error?: string;
  encryptionKey?: string;
}

export type TransferStatus = 'pending' | 'connecting' | 'transferring' | 'completed' | 'failed' | 'paused' | 'cancelled';

/**
 * File transfer chunk for large files
 */
export interface FileTransferChunk {
  transferId: string;
  chunkIndex: number;
  totalChunks: number;
  data: string; // Base64 encoded
  checksum: string;
}

// ============================================================================
// Clipboard Synchronization
// ============================================================================

/**
 * Clipboard sync settings
 */
export interface ClipboardSyncSettings {
  enabled: boolean;
  encryptionEnabled: boolean;
  syncImages: boolean;
  syncFiles: boolean;
  maxSize: number; // bytes
  allowedFormats: string[];
  blockedFormats: string[];
  syncInterval: number; // seconds
  confirmationRequired: boolean;
}

/**
 * Clipboard data
 */
export interface ClipboardData {
  type: ClipboardDataType;
  content: string;
  metadata: ClipboardMetadata;
  timestamp: Date;
  sourceDeviceId: string;
  encrypted: boolean;
}

export type ClipboardDataType = 'text' | 'image' | 'file' | 'html' | 'rtf';

export interface ClipboardMetadata {
  size: number;
  format: string;
  filename?: string;
  dimensions?: { width: number; height: number };
  mimeType?: string;
}

// ============================================================================
// Device Communication
// ============================================================================

/**
 * Inter-device message
 */
export interface DeviceMessage {
  messageId: string;
  sourceDeviceId: string;
  targetDeviceId?: string; // undefined for broadcast
  messageType: DeviceMessageType;
  payload: any;
  priority: MessagePriority;
  timestamp: Date;
  encrypted: boolean;
  requiresConfirmation: boolean;
  expiresAt?: Date;
}

export type DeviceMessageType =
  | 'heartbeat'
  | 'system_info'
  | 'task_request'
  | 'task_response'
  | 'remote_desktop_request'
  | 'remote_desktop_data'
  | 'file_transfer_request'
  | 'file_transfer_data'
  | 'clipboard_sync'
  | 'device_discovery'
  | 'pairing_request'
  | 'pairing_response'
  | 'control_command'
  | 'notification';

export type MessagePriority = 'low' | 'normal' | 'high' | 'critical';

/**
 * Device health and monitoring
 */
export interface DeviceHealth {
  deviceId: string;
  status: DeviceStatus;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  batteryLevel?: number;
  temperature?: number;
  networkQuality: NetworkQuality;
  lastHealthCheck: Date;
  issues: DeviceIssue[];
}

export type DeviceStatus = 'online' | 'offline' | 'busy' | 'sleep' | 'error' | 'maintenance';

export interface NetworkQuality {
  connected: boolean;
  connectionType: ConnectionType;
  signalStrength?: number; // 0-100
  bandwidthMbps?: number;
  latency: number; // milliseconds
  packetLoss: number; // percentage
}

export type ConnectionType = 'wifi' | 'ethernet' | 'cellular' | 'bluetooth' | 'unknown';

export interface DeviceIssue {
  type: IssueType;
  severity: IssueSeverity;
  description: string;
  detectedAt: Date;
  resolvedAt?: Date;
}

export type IssueType = 'performance' | 'connectivity' | 'battery' | 'storage' | 'security' | 'hardware';
export type IssueSeverity = 'low' | 'medium' | 'high' | 'critical';

// ============================================================================
// Device Profiles and Templates
// ============================================================================

/**
 * Device profile
 */
export interface DeviceProfile {
  profileId: string;
  name: string;
  description: string;
  deviceType: DeviceType;
  capabilities: DeviceCapabilities;
  defaultSettings: DeviceSettings;
  restrictions: DeviceRestrictions;
  isPublic: boolean;
  createdBy: string;
  createdAt: Date;
}

export interface DeviceSettings {
  autoConnect: boolean;
  remoteControlEnabled: boolean;
  fileTransferEnabled: boolean;
  clipboardSyncEnabled: boolean;
  screenCaptureEnabled: boolean;
  microphoneAccess: boolean;
  cameraAccess: boolean;
  powerManagement: PowerManagementSettings;
  securitySettings: DeviceSecuritySettings;
}

export interface PowerManagementSettings {
  sleepOnIdle: boolean;
  idleTimeout: number; // minutes
  wakeOnLan: boolean;
  powerSavingMode: boolean;
  thermalThrottling: boolean;
}

export interface DeviceSecuritySettings {
  requireAuthentication: boolean;
  encryptionRequired: boolean;
  sessionTimeout: number; // minutes
  maxFailedAttempts: number;
  allowedNetworks: string[];
  blockedApplications: string[];
}

// ============================================================================
// Zod Schemas
// ============================================================================

export const DeviceDiscoveryRequestSchema = z.object({
  enabled: z.boolean(),
  scanInterval: z.number().positive(),
  broadcastEnabled: z.boolean(),
  filterCriteria: z.object({
    deviceTypes: z.array(z.enum(['desktop', 'laptop', 'phone', 'tablet', 'server', 'embedded'])).optional(),
    platforms: z.array(z.string()).optional(),
    capabilities: z.array(z.string()).optional(),
    maxDistance: z.number().positive().optional()
  }).optional()
});

export const PairingRequestSchema = z.object({
  method: z.enum(['qr_code', 'nfc', 'bluetooth', 'manual', 'biometric']),
  qrToken: z.string().optional(),
  deviceId: z.string().optional(),
  pairingCode: z.string().optional(),
  biometricData: z.any().optional()
});

export const PairingResponseSchema = z.object({
  success: z.boolean(),
  deviceId: z.string().optional(),
  deviceName: z.string().optional(),
  requiresConfirmation: z.boolean().optional(),
  expiresAt: z.date().optional(),
  error: z.string().optional()
});

export const RemoteDesktopSessionSchema = z.object({
  sessionId: z.string(),
  hostDeviceId: z.string(),
  clientDeviceId: z.string(),
  protocol: z.enum(['vnc', 'rdp', 'webrtc', 'custom']),
  resolution: z.object({
    width: z.number(),
    height: z.number(),
    refreshRate: z.number().optional(),
    colorDepth: z.number().optional()
  }),
  quality: z.enum(['low', 'medium', 'high', 'ultra']),
  permissions: z.object({
    viewScreen: z.boolean(),
    controlMouse: z.boolean(),
    controlKeyboard: z.boolean(),
    transferFiles: z.boolean(),
    accessClipboard: z.boolean(),
    recordSession: z.boolean(),
    systemCommands: z.boolean(),
    audioAccess: z.boolean(),
    microphoneAccess: z.boolean()
  }),
  status: z.enum(['connecting', 'connected', 'disconnected', 'error', 'paused']),
  startedAt: z.date(),
  lastActivity: z.date(),
  duration: z.number(),
  screenRecording: z.boolean(),
  recordingPath: z.string().optional()
});

export const FileTransferRequestSchema = z.object({
  sourceDeviceId: z.string(),
  targetDeviceId: z.string(),
  filePath: z.string(),
  fileName: z.string().optional(),
  encryptionEnabled: z.boolean(),
  compressionEnabled: z.boolean(),
  resumeSupported: z.boolean()
});

export const FileTransferStatusSchema = z.object({
  transferId: z.string(),
  sourceDeviceId: z.string(),
  targetDeviceId: z.string(),
  fileName: z.string(),
  fileSize: z.number(),
  bytesTransferred: z.number(),
  transferRate: z.number(),
  status: z.enum(['pending', 'connecting', 'transferring', 'completed', 'failed', 'paused', 'cancelled']),
  startedAt: z.date(),
  estimatedCompletion: z.date().optional(),
  error: z.string().optional(),
  encryptionKey: z.string().optional()
});

export const ClipboardSyncSettingsSchema = z.object({
  enabled: z.boolean(),
  encryptionEnabled: z.boolean(),
  syncImages: z.boolean(),
  syncFiles: z.boolean(),
  maxSize: z.number(),
  allowedFormats: z.array(z.string()),
  blockedFormats: z.array(z.string()),
  syncInterval: z.number(),
  confirmationRequired: z.boolean()
});

export const DeviceMessageSchema = z.object({
  messageId: z.string(),
  sourceDeviceId: z.string(),
  targetDeviceId: z.string().optional(),
  messageType: z.enum([
    'heartbeat', 'system_info', 'task_request', 'task_response',
    'remote_desktop_request', 'remote_desktop_data', 'file_transfer_request',
    'file_transfer_data', 'clipboard_sync', 'device_discovery',
    'pairing_request', 'pairing_response', 'control_command', 'notification'
  ]),
  payload: z.any(),
  priority: z.enum(['low', 'normal', 'high', 'critical']),
  timestamp: z.date(),
  encrypted: z.boolean(),
  requiresConfirmation: z.boolean(),
  expiresAt: z.date().optional()
});

export const DeviceHealthSchema = z.object({
  deviceId: z.string(),
  status: z.enum(['online', 'offline', 'busy', 'sleep', 'error', 'maintenance']),
  cpuUsage: z.number().min(0).max(100),
  memoryUsage: z.number().min(0).max(100),
  diskUsage: z.number().min(0).max(100),
  batteryLevel: z.number().min(0).max(100).optional(),
  temperature: z.number().optional(),
  networkQuality: z.object({
    connected: z.boolean(),
    connectionType: z.enum(['wifi', 'ethernet', 'cellular', 'bluetooth', 'unknown']),
    signalStrength: z.number().min(0).max(100).optional(),
    bandwidthMbps: z.number().positive().optional(),
    latency: z.number(),
    packetLoss: z.number().min(0).max(100)
  }),
  lastHealthCheck: z.date(),
  issues: z.array(z.object({
    type: z.enum(['performance', 'connectivity', 'battery', 'storage', 'security', 'hardware']),
    severity: z.enum(['low', 'medium', 'high', 'critical']),
    description: z.string(),
    detectedAt: z.date(),
    resolvedAt: z.date().optional()
  }))
});

// ============================================================================
// Upgrade Coordination Types Extension
// ============================================================================

/**
 * Device upgrade capability information
 */
export interface UpgradeCapability {
  supportedUpgradeTypes: UpgradeType[];
  maxConcurrentUpgrades: number;
  upgradeBandwidthLimit?: number; // bytes per second
  supportedMigrationTargets: Platform[];
  rollbackSupport: boolean;
  backupCapability: BackupCapability;
}

/**
 * Migration status for device upgrades
 */
export interface MigrationStatus {
  currentMigrationId?: string;
  migrationState: MigrationState;
  progress: MigrationProgress;
  lastMigrationDate?: Date;
  migrationHistory: MigrationRecord[];
  supportedSourcePlatforms: Platform[];
  supportedTargetPlatforms: Platform[];
}

/**
 * Cross-platform sync settings for device configurations
 */
export interface CrossPlatformSync {
  enabled: boolean;
  syncInterval: number; // seconds
  autoSyncCategories: SyncCategory[];
  conflictResolution: ConflictResolutionStrategy;
  bandwidthLimit?: number;
  encryptionRequired: boolean;
  compressionEnabled: boolean;
  lastSyncDate?: Date;
  syncStatistics: SyncStatistics;
}

/**
 * Device upgrade coordination settings
 */
export interface DeviceUpgradeSettings {
  autoUpgradeEnabled: boolean;
  upgradeSchedule?: UpgradeSchedule;
  preferredUpgradeTime: TimeWindow;
  backupBeforeUpgrade: boolean;
  notifyBeforeUpgrade: boolean;
  upgradeWindowMinutes: number;
  maxConcurrentUpgrades: number;
  allowedUpgradeTypes: UpgradeType[];
  rollbacksEnabled: boolean;
}

/**
 * Device message types for upgrade coordination
 */
export type UpgradeDeviceMessageType =
  | DeviceMessageType // Inherit all existing types
  | 'upgrade_request'
  | 'upgrade_response'
  | 'migration_request'
  | 'migration_response'
  | 'sync_request'
  | 'sync_response'
  | 'backup_request'
  | 'backup_response'
  | 'rollback_request'
  | 'rollback_response'
  | 'upgrade_progress'
  | 'migration_progress'
  | 'sync_progress'
  | 'upgrade_verification'
  | 'migration_verification'
  | 'compatibility_check'
  | 'device_capabilities_query'
  | 'device_capabilities_response';

/**
 * Enhanced device message with upgrade support
 */
export interface UpgradeDeviceMessage extends DeviceMessage {
  messageType: UpgradeDeviceMessageType;
  upgradeData?: UpgradeMessageData;
}

/**
 * Upgrade-specific message data
 */
export interface UpgradeMessageData {
  upgradeId?: string;
  migrationId?: string;
  syncId?: string;
  upgradeType?: UpgradeType;
  sourcePlatform?: Platform;
  targetPlatform?: Platform;
  upgradeProgress?: UpgradeProgress;
  migrationProgress?: MigrationProgress;
  syncProgress?: SyncProgress;
  compatibilityInfo?: CompatibilityInfo;
  deviceCapabilities?: DeviceCapabilities;
  backupInfo?: BackupInfo;
  rollbackInfo?: RollbackInfo;
}

// ============================================================================
// Upgrade-Related Enums and Interfaces
// ============================================================================

export type UpgradeType =
  | 'major'
  | 'minor'
  | 'patch'
  | 'security'
  | 'platform_migration'
  | 'data_migration'
  | 'configuration_sync';

export type MigrationState =
  | 'idle'
  | 'preparing'
  | 'in_progress'
  | 'verifying'
  | 'completing'
  | 'failed'
  | 'rolled_back';

export type Platform =
  | 'windows'
  | 'macos'
  | 'linux'
  | 'android'
  | 'ios'
  | 'web';

export type SyncCategory =
  | 'user_settings'
  | 'ai_models'
  | 'application_data'
  | 'device_history'
  | 'security_settings'
  | 'preferences'
  | 'logs'
  | 'cache';

export type ConflictResolutionStrategy =
  | 'source_wins'
  | 'target_wins'
  | 'merge'
  | 'manual'
  | 'timestamp_wins'
  | 'ask_user';

export interface BackupCapability {
  automaticBackup: boolean;
  manualBackup: boolean;
  cloudBackup: boolean;
  localBackup: boolean;
  compressionSupport: boolean;
  encryptionSupport: boolean;
  maxBackupSize: number; // bytes
  backupRetentionDays: number;
}

export interface MigrationProgress {
  phase: MigrationPhase;
  percentage: number; // 0-100
  currentStep: string;
  totalSteps: number;
  completedSteps: number;
  bytesTransferred: number;
  totalBytes: number;
  estimatedTimeRemaining: number; // seconds
  error?: string;
}

export type MigrationPhase =
  | 'pre_migration'
  | 'data_export'
  | 'platform_setup'
  | 'data_import'
  | 'configuration'
  | 'validation'
  | 'post_migration';

export interface MigrationRecord {
  migrationId: string;
  sourcePlatform: Platform;
  targetPlatform: Platform;
  startTime: Date;
  endTime?: Date;
  status: MigrationState;
  dataCategories: SyncCategory[];
  bytesTransferred: number;
  filesTransferred: number;
  success: boolean;
  error?: string;
}

export interface SyncStatistics {
  totalSyncs: number;
  successfulSyncs: number;
  failedSyncs: number;
  lastSyncSuccess: boolean;
  lastSyncDate?: Date;
  averageSyncTime: number; // seconds
  totalDataTransferred: number; // bytes
  syncErrorRate: number; // percentage
}

export interface UpgradeSchedule {
  enabled: boolean;
  frequency: 'daily' | 'weekly' | 'monthly';
  dayOfWeek?: number; // 0-6 (Sunday-Saturday)
  dayOfMonth?: number; // 1-31
  time: string; // HH:MM
  timezone: string;
  lastScheduledDate?: Date;
  nextScheduledDate?: Date;
}

export interface TimeWindow {
  start: string; // HH:MM
  end: string; // HH:MM
  timezone: string;
  days: number[]; // 0-6 (Sunday-Saturday)
}

export interface UpgradeProgress {
  phase: string;
  percentage: number; // 0-100
  currentStep: string;
  totalSteps: number;
  completedSteps: number;
  bytesTransferred: number;
  totalBytes: number;
  estimatedTimeRemaining: number; // seconds
  error?: string;
}

export interface SyncProgress {
  phase: string;
  percentage: number; // 0-100
  currentStep: string;
  totalItems: number;
  completedItems: number;
  bytesTransferred: number;
  totalBytes: number;
  transferRate: number; // bytes per second
  estimatedTimeRemaining: number; // seconds
  error?: string;
}

export interface CompatibilityInfo {
  isCompatible: boolean;
  compatibilityScore: number; // 0-100
  issues: CompatibilityIssue[];
  recommendations: string[];
  requiredActions: string[];
  migrationComplexity: 'low' | 'medium' | 'high';
  estimatedDuration: number; // minutes
}

export interface CompatibilityIssue {
  type: CompatibilityIssueType;
  severity: 'low' | 'medium' | 'high' | 'critical';
  description: string;
  impact: string;
  resolution?: string;
  userActionRequired: boolean;
}

export type CompatibilityIssueType =
  | 'version_mismatch'
  | 'platform_incompatibility'
  | 'data_format_conflict'
  | 'dependency_missing'
  | 'hardware_requirement'
  | 'permission_required'
  | 'storage_insufficient'
  | 'network_incompatible';

export interface BackupInfo {
  backupId: string;
  createdDate: Date;
  size: number; // bytes
  compressed: boolean;
  encrypted: boolean;
  backupType: 'full' | 'incremental' | 'differential';
  location: string;
  checksum: string;
  verificationStatus: 'pending' | 'verified' | 'failed';
}

export interface RollbackInfo {
  rollbackId: string;
  originalVersion: string;
  targetVersion: string;
  rollbackDate: Date;
  status: 'pending' | 'in_progress' | 'completed' | 'failed';
  dataRestored: boolean;
  rollbackReason: string;
  estimatedDuration: number; // minutes
}

// ============================================================================
// Re-exports
// ============================================================================

export * from './common'; // Re-export Device, DeviceType, DeviceCapabilities, etc.