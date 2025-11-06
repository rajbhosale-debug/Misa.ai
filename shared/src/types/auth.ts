/**
 * Authentication and Security Types
 *
 * Types related to authentication, authorization, sessions,
 * biometric authentication, and security features.
 */

import { z } from 'zod';

// ============================================================================
// Authentication Types
// ============================================================================

/**
 * Authentication session information
 */
export interface AuthSession {
  sessionId: string;
  userId: string;
  deviceId: string;
  token: string;
  refreshToken: string;
  createdAt: Date;
  expiresAt: Date;
  lastActivity: Date;
  ipAddress?: string;
  userAgent?: string;
  deviceInfo?: DeviceInfo;
  permissions: string[];
  mfaVerified: boolean;
}

/**
 * Device information for authentication
 */
export interface DeviceInfo {
  deviceId: string;
  name: string;
  type: DeviceType;
  platform: string;
  version: string;
  trusted: boolean;
  lastSeen: Date;
  location?: LocationInfo;
}

export type DeviceType = 'desktop' | 'laptop' | 'phone' | 'tablet' | 'unknown';

/**
 * Location information
 */
export interface LocationInfo {
  country?: string;
  city?: string;
  region?: string;
  timezone?: string;
  coordinates?: {
    latitude: number;
    longitude: number;
  };
}

/**
 * Authentication credentials
 */
export interface AuthCredentials {
  identifier: string; // email, username, or phone
  password?: string;
  mfaCode?: string;
  biometricData?: BiometricData;
  deviceId?: string;
  rememberMe: boolean;
}

/**
 * Biometric authentication data
 */
export interface BiometricData {
  type: BiometricType;
  data: string; // Base64 encoded biometric template
  metadata?: Record<string, any>;
}

export type BiometricType = 'fingerprint' | 'face' | 'voice' | 'iris' | 'palm';

/**
 * Multi-Factor Authentication (MFA) setup
 */
export interface MFASetup {
  userId: string;
  enabledMethods: MFAMethod[];
  backupCodes: string[];
  recoveryOptions: RecoveryOption[];
}

export type MFAMethod = 'totp' | 'sms' | 'email' | 'biometric' | 'hardware_key';

export interface RecoveryOption {
  type: RecoveryType;
  value: string;
  verified: boolean;
}

export type RecoveryType = 'backup_code' | 'recovery_email' | 'recovery_phone' | 'trusted_device';

/**
 * Authentication response
 */
export interface AuthResponse {
  success: boolean;
  session?: AuthSession;
  requiresMFA: boolean;
  mfaMethods?: MFAMethod[];
  error?: string;
  nextAction?: AuthNextAction;
}

export type AuthNextAction =
  | 'complete'
  | 'mfa_required'
  | 'device_verification'
  | 'password_reset'
  | 'account_locked'
  | 'email_verification';

// ============================================================================
// Authorization and Permissions
// ============================================================================

/**
 * Role-based access control
 */
export interface Role {
  id: string;
  name: string;
  description: string;
  permissions: Permission[];
  isSystem: boolean;
  createdAt: Date;
}

/**
 * Permission definition
 */
export interface Permission {
  id: string;
  name: string;
  description: string;
  category: PermissionCategory;
  scope: PermissionScope;
  granted: boolean;
  grantedAt?: Date;
  expiresAt?: Date;
  conditions: PermissionCondition[];
  delegatedFrom?: string; // User ID who delegated this permission
  delegationExpiresAt?: Date;
}

export type PermissionCategory =
  | 'system'
  | 'data'
  | 'device'
  | 'network'
  | 'privacy'
  | 'security'
  | 'billing'
  | 'user_management';

export type PermissionScope =
  | 'read'
  | 'write'
  | 'delete'
  | 'admin'
  | 'execute'
  | 'moderate'
  | 'audit'
  | 'delegate';

/**
 * Permission condition for fine-grained access control
 */
export interface PermissionCondition {
  type: ConditionType;
  operator: ConditionOperator;
  value: any;
  description?: string;
}

export type ConditionType =
  | 'time_range'
  | 'date_range'
  | 'location'
  | 'device_type'
  | 'network_type'
  | 'user_status'
  | 'resource_type'
  | 'custom';

export type ConditionOperator =
  | 'equals'
  | 'not_equals'
  | 'contains'
  | 'starts_with'
  | 'ends_with'
  | 'greater_than'
  | 'less_than'
  | 'in'
  | 'not_in'
  | 'regex';

/**
 * Permission check result
 */
export interface PermissionCheck {
  allowed: boolean;
  reason?: string;
  conditions?: PermissionCondition[];
  requiresExplicitApproval?: boolean;
  approvalRequiredFrom?: string[]; // User IDs
}

// ============================================================================
// Security Settings
// ============================================================================

/**
 * User security settings
 */
export interface SecuritySettings {
  passwordPolicy: PasswordPolicy;
  sessionSettings: SessionSettings;
  mfaSettings: MFASettings;
  deviceSettings: DeviceSettings;
  privacySettings: PrivacySecuritySettings;
  auditSettings: AuditSettings;
  alertsSettings: SecurityAlertSettings;
}

/**
 * Password policy configuration
 */
export interface PasswordPolicy {
  minLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireNumbers: boolean;
  requireSpecialChars: boolean;
  preventReuse: number; // Number of previous passwords to prevent reuse
  maxAge: number; // Days
  lockoutThreshold: number; // Failed attempts
  lockoutDuration: number; // Minutes
}

/**
 * Session management settings
 */
export interface SessionSettings {
  maxConcurrentSessions: number;
  sessionTimeoutMinutes: number;
  idleTimeoutMinutes: number;
  absoluteTimeoutHours: number;
  requireReauthForSensitiveActions: boolean;
  rememberDeviceDays: number;
  insecureConnections: 'blocked' | 'warn' | 'allowed';
}

/**
 * MFA configuration
 */
export interface MFASettings {
  required: boolean;
  gracePeriodDays: number;
  trustedDeviceDays: number;
  backupCodeCount: number;
  allowedMethods: MFAMethod[];
  sensitiveActionsOnly: boolean;
}

/**
 * Device security settings
 */
export interface DeviceSettings {
  requireDeviceApproval: boolean;
  maxTrustedDevices: number;
  deviceRotationDays: number;
  encryptDeviceData: boolean;
  remoteWipeEnabled: boolean;
  locationTracking: boolean;
  jailbreakDetection: boolean;
}

/**
 * Privacy-related security settings
 */
export interface PrivacySecuritySettings {
  dataEncryption: boolean;
  endToEndEncryption: boolean;
  secureDelete: boolean;
  anonymizeAnalytics: boolean;
  locationPrivacy: LocationPrivacySettings;
  biometricPrivacy: BiometricPrivacySettings;
}

export interface LocationPrivacySettings {
  preciseLocation: boolean;
  locationHistory: boolean;
  shareLocationWithApps: boolean;
  locationBasedAuth: boolean;
}

export interface BiometricPrivacySettings {
  storeBiometricData: boolean;
  shareBiometricData: boolean;
  biometricBackup: boolean;
}

/**
 * Audit and logging settings
 */
export interface AuditSettings {
  logAllActions: boolean;
  logFailedAttempts: boolean;
  logDataAccess: boolean;
  logLevelChanges: boolean;
  logPermissionChanges: boolean;
  retentionDays: number;
  exportEnabled: boolean;
}

/**
 * Security alert settings
 */
export interface SecurityAlertSettings {
  newDeviceAlert: boolean;
  failedLoginAlert: boolean;
  passwordChangeAlert: boolean;
  permissionChangeAlert: boolean;
  unusualActivityAlert: boolean;
  dataBreachAlert: boolean;
  alertChannels: AlertChannel[];
}

export interface AlertChannel {
  type: 'email' | 'sms' | 'push' | 'webhook';
  enabled: boolean;
  address: string;
  filters: AlertFilter[];
}

export interface AlertFilter {
  severity: AlertSeverity;
  conditions: Record<string, any>;
}

export type AlertSeverity = 'low' | 'medium' | 'high' | 'critical';

// ============================================================================
// Security Events and Auditing
// ============================================================================

/**
 * Security audit log entry
 */
export interface SecurityAuditLog {
  id: string;
  userId: string;
  sessionId?: string;
  deviceId?: string;
  event: SecurityEvent;
  timestamp: Date;
  ipAddress?: string;
  userAgent?: string;
  location?: LocationInfo;
  result: AuditResult;
  details: Record<string, any>;
  risk: RiskLevel;
}

export type SecurityEvent =
  | 'login_attempt'
  | 'login_success'
  | 'login_failure'
  | 'logout'
  | 'password_change'
  | 'mfa_enabled'
  | 'mfa_disabled'
  | 'permission_granted'
  | 'permission_revoked'
  | 'data_access'
  | 'data_modification'
  | 'device_trusted'
  | 'device_untrusted'
  | 'security_setting_change'
  | 'suspicious_activity'
  | 'data_breach'
  | 'account_locked'
  | 'account_unlocked';

export type AuditResult = 'success' | 'failure' | 'error' | 'blocked';
export type RiskLevel = 'low' | 'medium' | 'high' | 'critical';

/**
 * Security alert
 */
export interface SecurityAlert {
  id: string;
  userId: string;
  type: SecurityAlertType;
  severity: AlertSeverity;
  title: string;
  message: string;
  details: Record<string, any>;
  timestamp: Date;
  acknowledged: boolean;
  acknowledgedAt?: Date;
  acknowledgedBy?: string;
  resolved: boolean;
  resolvedAt?: Date;
  resolvedBy?: string;
}

export type SecurityAlertType =
  | 'new_device_login'
  | 'suspicious_login'
  | 'multiple_failed_attempts'
  | 'password_compromise'
  | 'permission_abuse'
  | 'data_access_anomaly'
  | 'device_compromise'
  | 'account_takeover'
  | 'unauthorized_access'
  | 'security_vulnerability';

// ============================================================================
// Password and Credentials
// ============================================================================

/**
 * Password strength analysis
 */
export interface PasswordStrength {
  score: number; // 0-100
  level: PasswordStrengthLevel;
  feedback: string[];
  estimatedCrackTime: string;
  recommendations: string[];
}

export type PasswordStrengthLevel = 'very_weak' | 'weak' | 'fair' | 'good' | 'strong';

/**
 * Password reset request
 */
export interface PasswordResetRequest {
  email: string;
  deviceId?: string;
  resetToken?: string;
  newPassword: string;
  confirmPassword: string;
  mfaCode?: string;
}

/**
 * Password change request
 */
export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
  mfaCode?: string;
}

// ============================================================================
// Device Security
// ============================================================================

/**
 * Device security assessment
 */
export interface DeviceSecurityAssessment {
  deviceId: string;
  deviceName: string;
  overallScore: number; // 0-100
  riskLevel: RiskLevel;
  assessments: DeviceAssessment[];
  recommendations: string[];
  lastAssessed: Date;
}

export interface DeviceAssessment {
  category: DeviceSecurityCategory;
  score: number;
  status: AssessmentStatus;
  details: Record<string, any>;
}

export type DeviceSecurityCategory =
  | 'encryption'
  | 'biometrics'
  | 'system_integrity'
  | 'network_security'
  | 'app_security'
  | 'data_protection'
  | 'physical_security';

export type AssessmentStatus = 'secure' | 'vulnerable' | 'compromised' | 'unknown';

// ============================================================================
// Encryption and Keys
// ============================================================================

/**
 * Encryption key information
 */
export interface EncryptionKey {
  keyId: string;
  algorithm: EncryptionAlgorithm;
  keySize: number;
  purpose: KeyPurpose;
  createdAt: Date;
  expiresAt?: Date;
  status: KeyStatus;
  version: number;
  parentKeyId?: string;
}

export type EncryptionAlgorithm = 'aes-256-gcm' | 'aes-128-gcm' | 'chacha20-poly1305' | 'rsa-4096';
export type KeyPurpose = 'data_encryption' | 'key_encryption' | 'signing' | 'verification';
export type KeyStatus = 'active' | 'expired' | 'revoked' | 'compromised' | 'pending';

/**
 * Encrypted data wrapper
 */
export interface EncryptedData {
  data: string; // Base64 encoded encrypted data
  keyId: string;
  algorithm: string;
  nonce: string;
  tag: string;
  metadata?: Record<string, any>;
}

// ============================================================================
// Zod Schemas
// ============================================================================

export const AuthSessionSchema = z.object({
  sessionId: z.string().uuid(),
  userId: z.string().uuid(),
  deviceId: z.string().uuid(),
  token: z.string(),
  refreshToken: z.string(),
  createdAt: z.date(),
  expiresAt: z.date(),
  lastActivity: z.date(),
  ipAddress: z.string().ip().optional(),
  userAgent: z.string().optional(),
  deviceInfo: z.any().optional(), // DeviceInfo schema
  permissions: z.array(z.string()),
  mfaVerified: z.boolean(),
});

export const AuthCredentialsSchema = z.object({
  identifier: z.string().min(1),
  password: z.string().optional(),
  mfaCode: z.string().optional(),
  biometricData: z.any().optional(), // BiometricData schema
  deviceId: z.string().uuid().optional(),
  rememberMe: z.boolean(),
});

export const BiometricDataSchema = z.object({
  type: z.enum(['fingerprint', 'face', 'voice', 'iris', 'palm']),
  data: z.string(), // Base64 encoded
  metadata: z.record(z.any()).optional(),
});

export const PermissionSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string(),
  category: z.enum(['system', 'data', 'device', 'network', 'privacy', 'security', 'billing', 'user_management']),
  scope: z.enum(['read', 'write', 'delete', 'admin', 'execute', 'moderate', 'audit', 'delegate']),
  granted: z.boolean(),
  grantedAt: z.date().optional(),
  expiresAt: z.date().optional(),
  conditions: z.array(z.any()), // PermissionCondition schema
  delegatedFrom: z.string().uuid().optional(),
  delegationExpiresAt: z.date().optional(),
});

export const SecuritySettingsSchema = z.object({
  passwordPolicy: z.any(), // PasswordPolicy schema
  sessionSettings: z.any(), // SessionSettings schema
  mfaSettings: z.any(), // MFASettings schema
  deviceSettings: z.any(), // DeviceSettings schema
  privacySettings: z.any(), // PrivacySecuritySettings schema
  auditSettings: z.any(), // AuditSettings schema
  alertsSettings: z.any(), // SecurityAlertSettings schema
});

export const PasswordPolicySchema = z.object({
  minLength: z.number().min(4).max(128),
  requireUppercase: z.boolean(),
  requireLowercase: z.boolean(),
  requireNumbers: z.boolean(),
  requireSpecialChars: z.boolean(),
  preventReuse: z.number().min(0).max(50),
  maxAge: z.number().min(0),
  lockoutThreshold: z.number().min(1).max(20),
  lockoutDuration: z.number().min(1).max(1440), // minutes
});

export const SessionSettingsSchema = z.object({
  maxConcurrentSessions: z.number().min(1).max(10),
  sessionTimeoutMinutes: z.number().min(5).max(1440),
  idleTimeoutMinutes: z.number().min(5).max(1440),
  absoluteTimeoutHours: z.number().min(1).max(168),
  requireReauthForSensitiveActions: z.boolean(),
  rememberDeviceDays: z.number().min(1).max(365),
  insecureConnections: z.enum(['blocked', 'warn', 'allowed']),
});

export const MFASettingsSchema = z.object({
  required: z.boolean(),
  gracePeriodDays: z.number().min(0).max(90),
  trustedDeviceDays: z.number().min(1).max(365),
  backupCodeCount: z.number().min(5).max(20),
  allowedMethods: z.array(z.enum(['totp', 'sms', 'email', 'biometric', 'hardware_key'])),
  sensitiveActionsOnly: z.boolean(),
});

export const SecurityAuditLogSchema = z.object({
  id: z.string().uuid(),
  userId: z.string().uuid(),
  sessionId: z.string().uuid().optional(),
  deviceId: z.string().uuid().optional(),
  event: z.enum([
    'login_attempt', 'login_success', 'login_failure', 'logout', 'password_change',
    'mfa_enabled', 'mfa_disabled', 'permission_granted', 'permission_revoked',
    'data_access', 'data_modification', 'device_trusted', 'device_untrusted',
    'security_setting_change', 'suspicious_activity', 'data_breach',
    'account_locked', 'account_unlocked'
  ]),
  timestamp: z.date(),
  ipAddress: z.string().ip().optional(),
  userAgent: z.string().optional(),
  location: z.any().optional(), // LocationInfo schema
  result: z.enum(['success', 'failure', 'error', 'blocked']),
  details: z.record(z.any()),
  risk: z.enum(['low', 'medium', 'high', 'critical']),
});

// ============================================================================
// Type Guards and Utilities
// ============================================================================

export function isValidSession(session: any): session is AuthSession {
  return session && typeof session === 'object' &&
    typeof session.sessionId === 'string' &&
    typeof session.userId === 'string' &&
    typeof session.token === 'string' &&
    session.createdAt instanceof Date &&
    session.expiresAt instanceof Date;
}

export function hasPermission(session: AuthSession, permissionId: string): boolean {
  return session.permissions.includes(permissionId);
}

export function isSessionExpired(session: AuthSession): boolean {
  return new Date() > session.expiresAt;
}

export function isSessionIdle(session: AuthSession, maxIdleMinutes: number = 30): boolean {
  const idleTime = new Date().getTime() - session.lastActivity.getTime();
  return idleTime > (maxIdleMinutes * 60 * 1000);
}