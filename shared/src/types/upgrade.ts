/**
 * Upgrade Synchronization Types
 *
 * Types for cross-platform upgrade coordination, data migration,
 * and upgrade pairing between devices.
 */

import { z } from 'zod';

// ============================================================================
// Upgrade Synchronization Core Types
// ============================================================================

/**
 * Main upgrade synchronization request interface
 */
export interface UpgradeSyncRequest {
  sourceDeviceId: string;
  targetDeviceId: string;
  upgradeType: UpgradeType;
  dataTransferOptions: DataTransferOptions;
  syncSettings: SyncSettings;
  priority: SyncPriority;
  metadata: UpgradeMetadata;
}

/**
 * Data transfer options for upgrade synchronization
 */
export interface DataTransferOptions {
  transferUserSettings: boolean;
  transferAIModels: boolean;
  transferApplicationData: boolean;
  transferDeviceHistory: boolean;
  transferSecuritySettings: boolean;
  transferPreferences: boolean;
  compressionEnabled: boolean;
  encryptionEnabled: boolean;
  verificationEnabled: boolean;
  excludePatterns: string[];
  includeOnlyPatterns: string[];
}

/**
 * Platform migration interface for cross-platform upgrades
 */
export interface PlatformMigration {
  sourcePlatform: Platform;
  targetPlatform: Platform;
  migrationType: MigrationType;
  compatibilityCheck: CompatibilityCheck;
  platformSpecificData: PlatformSpecificData;
  migrationSteps: MigrationStep[];
}

/**
 * Data sync status tracking interface
 */
export interface DataSyncStatus {
  syncId: string;
  deviceId: string;
  status: SyncStatus;
  progress: SyncProgress;
  transferredData: TransferredDataSummary;
  errors: SyncError[];
  startTime: Date;
  estimatedCompletion?: Date;
  lastActivity: Date;
  retryCount: number;
}

/**
 * Sync progress tracking
 */
export interface SyncProgress {
  totalItems: number;
  completedItems: number;
  currentPhase: SyncPhase;
  bytesTransferred: number;
  totalBytes: number;
  transferRate: number; // bytes per second
  estimatedTimeRemaining: number; // seconds
}

/**
 * Summary of transferred data
 */
export interface TransferredDataSummary {
  categories: DataCategorySummary[];
  totalSize: number; // bytes
  totalFiles: number;
  checksums: ChecksumSummary;
}

export interface DataCategorySummary {
  category: DataCategory;
  size: number; // bytes
  files: number;
  success: boolean;
  error?: string;
}

export interface ChecksumSummary {
  algorithm: string;
  sourceChecksum: string;
  targetChecksum: string;
  verified: boolean;
}

/**
 * Migration step for platform-specific processes
 */
export interface MigrationStep {
  stepId: string;
  name: string;
  description: string;
  phase: MigrationPhase;
  required: boolean;
  estimatedDuration: number; // seconds
  dependencies: string[];
  status: StepStatus;
  progress: number; // 0-100
  startTime?: Date;
  completionTime?: Date;
  error?: string;
}

// ============================================================================
// Enum Definitions
// ============================================================================

export type UpgradeType =
  | 'major'
  | 'minor'
  | 'patch'
  | 'security'
  | 'platform_migration'
  | 'data_migration'
  | 'configuration_sync';

export type SyncPriority =
  | 'low'
  | 'normal'
  | 'high'
  | 'critical';

export type Platform =
  | 'windows'
  | 'macos'
  | 'linux'
  | 'android'
  | 'ios'
  | 'web';

export type MigrationType =
  | 'same_platform_upgrade'
  | 'cross_platform_migration'
  | 'data_only_transfer'
  | 'settings_only_transfer'
  | 'full_migration';

export type SyncStatus =
  | 'pending'
  | 'connecting'
  | 'analyzing'
  | 'preparing'
  | 'transferring'
  | 'verifying'
  | 'applying'
  | 'completed'
  | 'failed'
  | 'cancelled'
  | 'paused';

export type SyncPhase =
  | 'initialization'
  | 'discovery'
  | 'analysis'
  | 'preparation'
  | 'transfer'
  | 'verification'
  | 'application'
  | 'cleanup'
  | 'completion';

export type DataCategory =
  | 'user_settings'
  | 'ai_models'
  | 'application_data'
  | 'device_history'
  | 'security_settings'
  | 'preferences'
  | 'logs'
  | 'cache'
  | 'temp_files';

export type MigrationPhase =
  | 'pre_migration'
  | 'data_export'
  | 'platform_setup'
  | 'data_import'
  | 'configuration'
  | 'validation'
  | 'post_migration';

export type StepStatus =
  | 'pending'
  | 'running'
  | 'completed'
  | 'failed'
  | 'skipped';

// ============================================================================
// Configuration and Settings
// ============================================================================

export interface SyncSettings {
  bandwidthLimit?: number; // bytes per second
  retryAttempts: number;
  retryDelay: number; // seconds
  timeout: number; // seconds
  verifyIntegrity: boolean;
  createBackup: boolean;
  compressionLevel: number; // 0-9
  encryptionAlgorithm: EncryptionAlgorithm;
  conflictResolution: ConflictResolutionStrategy;
  notifications: NotificationSettings;
}

export type EncryptionAlgorithm =
  | 'AES256-GCM'
  | 'ChaCha20-Poly1305'
  | 'none';

export type ConflictResolutionStrategy =
  | 'source_wins'
  | 'target_wins'
  | 'merge'
  | 'manual'
  | 'timestamp_wins'
  | 'ask_user';

export interface NotificationSettings {
  progressUpdates: boolean;
  completionAlert: boolean;
  errorAlerts: boolean;
  emailNotifications: boolean;
  pushNotifications: boolean;
}

// ============================================================================
// Metadata and Context
// ============================================================================

export interface UpgradeMetadata {
  version: string;
  buildNumber: string;
  releaseDate: Date;
  changelog: string;
  requirements: SystemRequirements;
  knownIssues: KnownIssue[];
  rollbackAvailable: boolean;
  estimatedDowntime: number; // seconds
}

export interface SystemRequirements {
  minimumRAM: number; // GB
  recommendedRAM: number; // GB
  minimumStorage: number; // GB
  recommendedStorage: number; // GB
  requiredOSVersion: string;
  supportedArchitectures: string[];
  optionalDependencies: string[];
}

export interface KnownIssue {
  id: string;
  title: string;
  description: string;
  severity: IssueSeverity;
  workaround?: string;
  platformsAffected: Platform[];
}

export type IssueSeverity =
  | 'low'
  | 'medium'
  | 'high'
  | 'critical';

export interface CompatibilityCheck {
  isCompatible: boolean;
  issues: CompatibilityIssue[];
  recommendations: string[];
  requiredActions: string[];
}

export interface CompatibilityIssue {
  type: CompatibilityIssueType;
  severity: IssueSeverity;
  description: string;
  impact: string;
  resolution?: string;
}

export type CompatibilityIssueType =
  | 'version_mismatch'
  | 'platform_incompatibility'
  | 'data_format_conflict'
  | 'dependency_missing'
  | 'hardware_requirement'
  | 'permission_required';

// ============================================================================
// Platform-Specific Data
// ============================================================================

export interface PlatformSpecificData {
  [K in Platform]: PlatformData;
}

export interface PlatformData {
  platform: Platform;
  registryKeys?: string[];
  configFiles?: string[];
  environmentVariables?: Record<string, string>;
  services?: string[];
  permissions?: string[];
  customMigrations?: CustomMigration[];
}

export interface CustomMigration {
  name: string;
  description: string;
  script: string;
  parameters?: Record<string, any>;
  dependencies: string[];
}

// ============================================================================
// Error Handling
// ============================================================================

export interface SyncError {
  code: string;
  type: ErrorType;
  message: string;
  details?: any;
  phase: SyncPhase;
  retryable: boolean;
  suggestedAction?: string;
  timestamp: Date;
}

export type ErrorType =
  | 'connection_error'
  | 'authentication_error'
  | 'permission_error'
  | 'storage_error'
  | 'network_error'
  | 'validation_error'
  | 'compatibility_error'
  | 'timeout_error'
  | 'checksum_mismatch'
  | 'encryption_error';

// ============================================================================
// Advanced Upgrade Features
// ============================================================================

/**
 * Rollback configuration for failed upgrades
 */
export interface RollbackConfiguration {
  enabled: boolean;
  autoRollback: boolean;
  rollbackConditions: RollbackCondition[];
  backupLocation: string;
  retentionPeriod: number; // days
}

export interface RollbackCondition {
  condition: string;
  action: 'rollback' | 'pause' | 'alert' | 'retry';
  threshold?: number;
  timeframe?: number; // seconds
}

/**
 * Batch upgrade coordination for multiple devices
 */
export interface BatchUpgrade {
  batchId: string;
  name: string;
  description: string;
  devices: string[];
  strategy: BatchStrategy;
  schedule: BatchSchedule;
  progress: BatchProgress;
  settings: BatchSettings;
}

export type BatchStrategy =
  | 'sequential'
  | 'parallel'
  | 'wave_based'
  | 'staggered';

export interface BatchSchedule {
  startTime: Date;
  maxConcurrency: number;
  waveDelay: number; // seconds between waves
  maintenanceWindow?: TimeWindow;
}

export interface TimeWindow {
  start: string; // HH:MM
  end: string; // HH:MM
  timezone: string;
  days: number[]; // 0-6 (Sunday-Saturday)
}

export interface BatchProgress {
  totalDevices: number;
  completedDevices: number;
  failedDevices: number;
  inProgressDevices: number;
  currentWave: number;
  totalWaves: number;
  estimatedCompletion: Date;
}

export interface BatchSettings {
  allowPartialFailure: boolean;
  failureThreshold: number; // percentage
  autoRetry: boolean;
  maxRetries: number;
  notifications: boolean;
}

// ============================================================================
// Zod Schemas for Validation
// ============================================================================

export const UpgradeSyncRequestSchema = z.object({
  sourceDeviceId: z.string(),
  targetDeviceId: z.string(),
  upgradeType: z.enum(['major', 'minor', 'patch', 'security', 'platform_migration', 'data_migration', 'configuration_sync']),
  dataTransferOptions: z.object({
    transferUserSettings: z.boolean(),
    transferAIModels: z.boolean(),
    transferApplicationData: z.boolean(),
    transferDeviceHistory: z.boolean(),
    transferSecuritySettings: z.boolean(),
    transferPreferences: z.boolean(),
    compressionEnabled: z.boolean(),
    encryptionEnabled: z.boolean(),
    verificationEnabled: z.boolean(),
    excludePatterns: z.array(z.string()),
    includeOnlyPatterns: z.array(z.string()),
  }),
  syncSettings: z.object({
    bandwidthLimit: z.number().positive().optional(),
    retryAttempts: z.number().min(0).max(10),
    retryDelay: z.number().min(0),
    timeout: z.number().positive(),
    verifyIntegrity: z.boolean(),
    createBackup: z.boolean(),
    compressionLevel: z.number().min(0).max(9),
    encryptionAlgorithm: z.enum(['AES256-GCM', 'ChaCha20-Poly1305', 'none']),
    conflictResolution: z.enum(['source_wins', 'target_wins', 'merge', 'manual', 'timestamp_wins', 'ask_user']),
    notifications: z.object({
      progressUpdates: z.boolean(),
      completionAlert: z.boolean(),
      errorAlerts: z.boolean(),
      emailNotifications: z.boolean(),
      pushNotifications: z.boolean(),
    }),
  }),
  priority: z.enum(['low', 'normal', 'high', 'critical']),
  metadata: z.object({
    version: z.string(),
    buildNumber: z.string(),
    releaseDate: z.date(),
    changelog: z.string(),
    requirements: z.object({
      minimumRAM: z.number().positive(),
      recommendedRAM: z.number().positive(),
      minimumStorage: z.number().positive(),
      recommendedStorage: z.number().positive(),
      requiredOSVersion: z.string(),
      supportedArchitectures: z.array(z.string()),
      optionalDependencies: z.array(z.string()),
    }),
    knownIssues: z.array(z.object({
      id: z.string(),
      title: z.string(),
      description: z.string(),
      severity: z.enum(['low', 'medium', 'high', 'critical']),
      workaround: z.string().optional(),
      platformsAffected: z.array(z.enum(['windows', 'macos', 'linux', 'android', 'ios', 'web'])),
    })),
    rollbackAvailable: z.boolean(),
    estimatedDowntime: z.number().min(0),
  }),
});

export const PlatformMigrationSchema = z.object({
  sourcePlatform: z.enum(['windows', 'macos', 'linux', 'android', 'ios', 'web']),
  targetPlatform: z.enum(['windows', 'macos', 'linux', 'android', 'ios', 'web']),
  migrationType: z.enum(['same_platform_upgrade', 'cross_platform_migration', 'data_only_transfer', 'settings_only_transfer', 'full_migration']),
  compatibilityCheck: z.object({
    isCompatible: z.boolean(),
    issues: z.array(z.object({
      type: z.enum(['version_mismatch', 'platform_incompatibility', 'data_format_conflict', 'dependency_missing', 'hardware_requirement', 'permission_required']),
      severity: z.enum(['low', 'medium', 'high', 'critical']),
      description: z.string(),
      impact: z.string(),
      resolution: z.string().optional(),
    })),
    recommendations: z.array(z.string()),
    requiredActions: z.array(z.string()),
  }),
  platformSpecificData: z.record(z.object({
    platform: z.enum(['windows', 'macos', 'linux', 'android', 'ios', 'web']),
    registryKeys: z.array(z.string()).optional(),
    configFiles: z.array(z.string()).optional(),
    environmentVariables: z.record(z.string()).optional(),
    services: z.array(z.string()).optional(),
    permissions: z.array(z.string()).optional(),
    customMigrations: z.array(z.object({
      name: z.string(),
      description: z.string(),
      script: z.string(),
      parameters: z.record(z.any()).optional(),
      dependencies: z.array(z.string()),
    })).optional(),
  })),
  migrationSteps: z.array(z.object({
    stepId: z.string(),
    name: z.string(),
    description: z.string(),
    phase: z.enum(['pre_migration', 'data_export', 'platform_setup', 'data_import', 'configuration', 'validation', 'post_migration']),
    required: z.boolean(),
    estimatedDuration: z.number().min(0),
    dependencies: z.array(z.string()),
    status: z.enum(['pending', 'running', 'completed', 'failed', 'skipped']),
    progress: z.number().min(0).max(100),
    startTime: z.date().optional(),
    completionTime: z.date().optional(),
    error: z.string().optional(),
  })),
});

export const DataSyncStatusSchema = z.object({
  syncId: z.string(),
  deviceId: z.string(),
  status: z.enum(['pending', 'connecting', 'analyzing', 'preparing', 'transferring', 'verifying', 'applying', 'completed', 'failed', 'cancelled', 'paused']),
  progress: z.object({
    totalItems: z.number().min(0),
    completedItems: z.number().min(0),
    currentPhase: z.enum(['initialization', 'discovery', 'analysis', 'preparation', 'transfer', 'verification', 'application', 'cleanup', 'completion']),
    bytesTransferred: z.number().min(0),
    totalBytes: z.number().min(0),
    transferRate: z.number().min(0),
    estimatedTimeRemaining: z.number().min(0),
  }),
  transferredData: z.object({
    categories: z.array(z.object({
      category: z.enum(['user_settings', 'ai_models', 'application_data', 'device_history', 'security_settings', 'preferences', 'logs', 'cache', 'temp_files']),
      size: z.number().min(0),
      files: z.number().min(0),
      success: z.boolean(),
      error: z.string().optional(),
    })),
    totalSize: z.number().min(0),
    totalFiles: z.number().min(0),
    checksums: z.object({
      algorithm: z.string(),
      sourceChecksum: z.string(),
      targetChecksum: z.string(),
      verified: z.boolean(),
    }),
  }),
  errors: z.array(z.object({
    code: z.string(),
    type: z.enum(['connection_error', 'authentication_error', 'permission_error', 'storage_error', 'network_error', 'validation_error', 'compatibility_error', 'timeout_error', 'checksum_mismatch', 'encryption_error']),
    message: z.string(),
    details: z.any().optional(),
    phase: z.enum(['initialization', 'discovery', 'analysis', 'preparation', 'transfer', 'verification', 'application', 'cleanup', 'completion']),
    retryable: z.boolean(),
    suggestedAction: z.string().optional(),
    timestamp: z.date(),
  })),
  startTime: z.date(),
  estimatedCompletion: z.date().optional(),
  lastActivity: z.date(),
  retryCount: z.number().min(0),
});

export const BatchUpgradeSchema = z.object({
  batchId: z.string(),
  name: z.string(),
  description: z.string(),
  devices: z.array(z.string()),
  strategy: z.enum(['sequential', 'parallel', 'wave_based', 'staggered']),
  schedule: z.object({
    startTime: z.date(),
    maxConcurrency: z.number().min(1),
    waveDelay: z.number().min(0),
    maintenanceWindow: z.object({
      start: z.string(),
      end: z.string(),
      timezone: z.string(),
      days: z.array(z.number().min(0).max(6)),
    }).optional(),
  }),
  progress: z.object({
    totalDevices: z.number().min(0),
    completedDevices: z.number().min(0),
    failedDevices: z.number().min(0),
    inProgressDevices: z.number().min(0),
    currentWave: z.number().min(0),
    totalWaves: z.number().min(0),
    estimatedCompletion: z.date(),
  }),
  settings: z.object({
    allowPartialFailure: z.boolean(),
    failureThreshold: z.number().min(0).max(100),
    autoRetry: z.boolean(),
    maxRetries: z.number().min(0),
    notifications: z.boolean(),
  }),
});

// ============================================================================
// Type Guards and Utilities
// ============================================================================

export function isUpgradeSyncRequest(obj: any): obj is UpgradeSyncRequest {
  return UpgradeSyncRequestSchema.safeParse(obj).success;
}

export function isPlatformMigration(obj: any): obj is PlatformMigration {
  return PlatformMigrationSchema.safeParse(obj).success;
}

export function isDataSyncStatus(obj: any): obj is DataSyncStatus {
  return DataSyncStatusSchema.safeParse(obj).success;
}

export function createDefaultSyncSettings(): SyncSettings {
  return {
    retryAttempts: 3,
    retryDelay: 30,
    timeout: 3600, // 1 hour
    verifyIntegrity: true,
    createBackup: true,
    compressionLevel: 6,
    encryptionAlgorithm: 'AES256-GCM',
    conflictResolution: 'timestamp_wins',
    notifications: {
      progressUpdates: true,
      completionAlert: true,
      errorAlerts: true,
      emailNotifications: false,
      pushNotifications: true,
    },
  };
}

export function createDefaultDataTransferOptions(): DataTransferOptions {
  return {
    transferUserSettings: true,
    transferAIModels: true,
    transferApplicationData: true,
    transferDeviceHistory: true,
    transferSecuritySettings: true,
    transferPreferences: true,
    compressionEnabled: true,
    encryptionEnabled: true,
    verificationEnabled: true,
    excludePatterns: ['*.tmp', '*.cache', '*.log'],
    includeOnlyPatterns: [],
  };
}

export function calculateSyncProgress(status: DataSyncStatus): number {
  if (status.progress.totalItems === 0) return 0;
  return Math.round((status.progress.completedItems / status.progress.totalItems) * 100);
}

export function estimateTransferDuration(
  totalBytes: number,
  transferRate: number,
  overheadFactor: number = 1.2
): number {
  return Math.ceil((totalBytes / transferRate) * overheadFactor);
}