/**
 * Common Types and Interfaces
 *
 * Shared types used across all MISA.AI platforms
 */

import { z } from 'zod';

// ============================================================================
// User and Profile Types
// ============================================================================

/**
 * User information
 */
export interface User {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatar?: string;
  profile: UserProfile;
  preferences: UserPreferences;
  createdAt: Date;
  lastActiveAt: Date;
  isActive: boolean;
  verified: boolean;
  roles: string[];
}

/**
 * User profile information
 */
export interface UserProfile {
  firstName: string;
  lastName: string;
  bio?: string;
  location?: string;
  timezone: string;
  language: string;
  dateOfBirth?: Date;
  phone?: string;
  website?: string;
  socialLinks?: Record<string, string>;
  interests: string[];
  skills: string[];
}

/**
 * User preferences and settings
 */
export interface UserPreferences {
  // Interface preferences
  theme: 'light' | 'dark' | 'auto';
  language: string;
  timezone: string;
  dateFormat: string;
  timeFormat: '12h' | '24h';

  // Notification preferences
  emailNotifications: boolean;
  pushNotifications: boolean;
  inAppNotifications: boolean;
  notificationSound: boolean;
  doNotDisturb: {
    enabled: boolean;
    startTime: string; // HH:mm
    endTime: string;   // HH:mm
    weekends: boolean;
  };

  // AI Assistant preferences
  assistantName: string;
  voiceEnabled: boolean;
  voiceGender: 'male' | 'female' | 'neutral';
  personality: 'professional' | 'friendly' | 'casual' | 'formal';
  responseStyle: 'concise' | 'balanced' | 'detailed';

  // Privacy preferences
  dataSharing: boolean;
  analytics: boolean;
  crashReporting: boolean;
  locationTracking: boolean;
  biometricAuth: boolean;

  // Work preferences
  workHours: {
    startHour: number;
    endHour: number;
    workDays: number[]; // 0-6 (Sunday-Saturday)
    breaks: Array<{
      startHour: number;
      endHour: number;
      type: string;
    }>;
  };

  focusPreferences: {
    deepWorkMinutes: number;
    breakMinutes: number;
    pomodoroEnabled: boolean;
    distractionBlocking: boolean;
  };
}

// ============================================================================
// Device and Platform Types
// ============================================================================

/**
 * Device information
 */
export interface Device {
  id: string;
  name: string;
  type: DeviceType;
  platform: string;
  osVersion: string;
  appVersion: string;
  capabilities: DeviceCapabilities;
  status: DeviceStatus;
  lastSeen: Date;
  batteryLevel?: number;
  isPrimary: boolean;
  location?: LocationInfo;
}

export type DeviceType = 'desktop' | 'laptop' | 'phone' | 'tablet' | 'server' | 'embedded';
export type DeviceStatus = 'online' | 'offline' | 'busy' | 'sleep' | 'error';

export interface DeviceCapabilities {
  supportsGPU: boolean;
  supportsVision: boolean;
  supportsAudio: boolean;
  hasCamera: boolean;
  hasMicrophone: boolean;
  maxMemoryMB: number;
  cpuCores: number;
  gpuMemoryMB?: number;
  batteryPowered: boolean;
  supportsRemoteDesktop: boolean;
}

export interface LocationInfo {
  latitude: number;
  longitude: number;
  accuracy: number;
  address?: string;
  timestamp: Date;
}

// ============================================================================
// Application Types
// ============================================================================

/**
 * MISA.AI application information
 */
export interface Application {
  id: string;
  name: string;
  displayName: string;
  description: string;
  version: string;
  category: ApplicationCategory;
  icon: string;
  status: ApplicationStatus;
  permissions: Permission[];
  settings: ApplicationSettings;
  isDefault: boolean;
  dependencies: string[];
  supportedPlatforms: DeviceType[];
}

export type ApplicationCategory =
  | 'productivity'
  | 'communication'
  | 'entertainment'
  | 'education'
  | 'health'
  | 'finance'
  | 'utilities'
  | 'development'
  | 'media'
  | 'system';

export type ApplicationStatus = 'installed' | 'active' | 'inactive' | 'updating' | 'error';

export interface ApplicationSettings {
  autoStart: boolean;
  notifications: boolean;
  dataUsage: boolean;
  backgroundSync: boolean;
  customSettings: Record<string, any>;
}

// ============================================================================
// Plugin Types
// ============================================================================

/**
 * Plugin information and manifest
 */
export interface Plugin {
  id: string;
  name: string;
  displayName: string;
  description: string;
  version: string;
  author: string;
  category: PluginCategory;
  status: PluginStatus;
  manifest: PluginManifest;
  permissions: Permission[];
  settings: PluginSettings;
  installationDate: Date;
  lastUpdated: Date;
  isSystem: boolean;
}

export type PluginCategory =
  | 'ai-model'
  | 'data-source'
  | 'integration'
  | 'utility'
  | 'theme'
  | 'voice'
  | 'automation'
  | 'security'
  | 'communication'
  | 'productivity';

export type PluginStatus = 'installed' | 'active' | 'inactive' | 'error' | 'updating';

export interface PluginManifest {
  id: string;
  name: string;
  version: string;
  description: string;
  author: string;
  homepage?: string;
  repository?: string;
  license: string;
  main: string;
  type: PluginType;
  permissions: PermissionScope[];
  capabilities: PluginCapability[];
  dependencies: PluginDependency[];
  supportedPlatforms: string[];
  minimumKernelVersion: string;
}

export type PluginType = 'typescript' | 'rust' | 'python' | 'web-component';
export type PermissionScope =
  | 'memory.read'
  | 'memory.write'
  | 'device.control'
  | 'network.access'
  | 'filesystem.read'
  | 'filesystem.write'
  | 'system.info'
  | 'camera.access'
  | 'microphone.access'
  | 'location.access';

export interface PluginCapability {
  name: string;
  description: string;
  parameters: Record<string, any>;
}

export interface PluginDependency {
  id: string;
  version: string;
  optional: boolean;
}

export interface PluginSettings {
  enabled: boolean;
  autoUpdate: boolean;
  customSettings: Record<string, any>;
}

// ============================================================================
// Task and Workflow Types
// ============================================================================

/**
 * Task information
 */
export interface Task {
  id: string;
  title: string;
  description?: string;
  type: TaskType;
  status: TaskStatus;
  priority: TaskPriority;
  assignedTo?: string; // device or user ID
  createdBy: string;
  createdAt: Date;
  updatedAt: Date;
  dueDate?: Date;
  completedAt?: Date;
  estimatedDuration?: number; // minutes
  actualDuration?: number; // minutes
  tags: string[];
  dependencies: string[]; // task IDs
  subtasks: Task[];
  attachments: TaskAttachment[];
  context: TaskContext;
  metadata: Record<string, any>;
}

export type TaskType =
  | 'general'
  | 'coding'
  | 'writing'
  | 'research'
  | 'analysis'
  | 'design'
  | 'communication'
  | 'automation'
  | 'system';

export type TaskStatus =
  | 'todo'
  | 'in_progress'
  | 'review'
  | 'testing'
  | 'completed'
  | 'cancelled'
  | 'blocked';

export type TaskPriority = 'low' | 'normal' | 'high' | 'urgent' | 'critical';

export interface TaskAttachment {
  id: string;
  name: string;
  type: string;
  size: number;
  url: string;
  uploadedAt: Date;
}

export interface TaskContext {
  projectId?: string;
  application?: string;
  device?: string;
  location?: string;
  relatedTasks: string[];
  notes: string;
}

/**
 * Workflow information
 */
export interface Workflow {
  id: string;
  name: string;
  description: string;
  version: string;
  status: WorkflowStatus;
  trigger: WorkflowTrigger;
  steps: WorkflowStep[];
  variables: WorkflowVariable[];
  settings: WorkflowSettings;
  createdBy: string;
  createdAt: Date;
  lastExecuted?: Date;
  executionCount: number;
  errorCount: number;
}

export type WorkflowStatus = 'draft' | 'active' | 'paused' | 'error' | 'archived';

export interface WorkflowTrigger {
  type: TriggerType;
  config: Record<string, any>;
}

export type TriggerType =
  | 'manual'
  | 'schedule'
  | 'event'
  | 'webhook'
  | 'device_state'
  | 'location'
  | 'time';

export interface WorkflowStep {
  id: string;
  name: string;
  type: StepType;
  config: Record<string, any>;
  position: { x: number; y: number };
  connections: StepConnection[];
  retryPolicy?: RetryPolicy;
}

export type StepType =
  | 'action'
  | 'condition'
  | 'loop'
  | 'delay'
  | 'notification'
  | 'ai_task'
  | 'device_control'
  | 'data_processing';

export interface StepConnection {
  sourceStep: string;
  targetStep: string;
  condition?: string;
}

export interface RetryPolicy {
  maxAttempts: number;
  backoffType: 'fixed' | 'exponential' | 'linear';
  delay: number;
}

export interface WorkflowVariable {
  name: string;
  type: VariableType;
  defaultValue?: any;
  required: boolean;
  description?: string;
}

export type VariableType = 'string' | 'number' | 'boolean' | 'array' | 'object' | 'date';

export interface WorkflowSettings {
  autoRetry: boolean;
  timeoutMinutes: number;
  errorNotification: boolean;
  logLevel: 'debug' | 'info' | 'warn' | 'error';
}

// ============================================================================
// Memory and Context Types
// ============================================================================

/**
 * Memory item
 */
export interface MemoryItem {
  id: string;
  content: string;
  contentType: ContentType;
  memoryType: MemoryType;
  importance: Importance;
  tags: string[];
  metadata: Record<string, any>;
  createdAt: Date;
  lastAccessed: Date;
  accessCount: number;
  encrypted: boolean;
  expiresAt?: Date;
  relatedItems: string[];
}

export type ContentType =
  | 'text'
  | 'image'
  | 'audio'
  | 'video'
  | 'document'
  | 'code'
  | 'structured_data';

export type MemoryType = 'short_term' | 'medium_term' | 'long_term' | 'permanent';
export type Importance = 'low' | 'medium' | 'high' | 'critical';

/**
 * Context state
 */
export interface ContextState {
  sessionId: string;
  userId: string;
  currentTask?: string;
  activeApplications: ApplicationInfo[];
  systemState: SystemState;
  environment: EnvironmentContext;
  userPreferences: UserPreferences;
  shortTermMemory: MemoryItem[];
  lastUpdated: Date;
}

export interface ApplicationInfo {
  appId: string;
  name: string;
  windowTitle?: string;
  processId: number;
  memoryUsageMB?: number;
  cpuUsagePercent?: number;
  focused: boolean;
  startTime: Date;
}

export interface SystemState {
  cpuUsagePercent: number;
  memoryUsageMB: number;
  diskUsageMB: number;
  batteryLevel?: number;
  powerSource: 'battery' | 'ac' | 'ups';
  networkStatus: NetworkStatus;
  activeDevices: string[];
}

export interface NetworkStatus {
  connected: boolean;
  connectionType: string;
  signalStrength?: number;
  bandwidthMbps?: number;
}

export interface EnvironmentContext {
  location?: LocationData;
  timeOfDay: TimeOfDay;
  dayOfWeek: DayOfWeek;
  ambientConditions?: AmbientConditions;
  nearbyDevices: NearbyDevice[];
}

export interface LocationData {
  latitude: number;
  longitude: number;
  accuracy: number;
  address?: string;
}

export type TimeOfDay =
  | 'early_morning'  // 5-8
  | 'morning'        // 8-12
  | 'afternoon'      // 12-17
  | 'evening'        // 17-21
  | 'night'          // 21-24
  | 'late_night';    // 0-5

export type DayOfWeek =
  | 'monday' | 'tuesday' | 'wednesday' | 'thursday' | 'friday' | 'saturday' | 'sunday';

export interface AmbientConditions {
  temperatureCelsius: number;
  humidityPercent: number;
  noiseLevelDb: number;
  lightLevelLux: number;
}

export interface NearbyDevice {
  deviceId: string;
  name: string;
  deviceType: string;
  signalStrength: number;
  lastSeen: Date;
}

// ============================================================================
// AI and Model Types
// ============================================================================

export type ModelType =
  | 'chat'
  | 'coding'
  | 'vision'
  | 'reasoning'
  | 'summarization'
  | 'speech_to_text'
  | 'text_to_speech'
  | 'embedding'
  | 'multimodal';

// ============================================================================
// Permission and Security Types
// ============================================================================

/**
 * Permission definition
 */
export interface Permission {
  id: string;
  name: string;
  description: string;
  category: PermissionCategory;
  granted: boolean;
  grantedAt?: Date;
  expiresAt?: Date;
  conditions: PermissionCondition[];
  scope: PermissionScope;
}

export type PermissionCategory =
  | 'system'
  | 'data'
  | 'device'
  | 'network'
  | 'privacy'
  | 'security';

export interface PermissionCondition {
  type: ConditionType;
  value: any;
}

export type ConditionType =
  | 'time_range'
  | 'location_based'
  | 'user_present'
  | 'device_locked'
  | 'network_secure'
  | 'explicit_confirmation';

// ============================================================================
// Privacy and Consent Types
// ============================================================================

export type ConsentType =
  | 'data_processing'
  | 'cloud_sync'
  | 'analytics'
  | 'biometric'
  | 'location'
  | 'microphone'
  | 'camera'
  | 'screen_capture'
  | 'app_usage'
  | 'contact_access'
  | 'file_access'
  | 'network_access'
  | 'third_party_sharing'
  | 'marketing'
  | 'research';

export interface ConsentRecord {
  consentId: string;
  userId: string;
  consentType: ConsentType;
  purpose: string;
  granted: boolean;
  grantedAt?: Date;
  expiresAt?: Date;
  revokedAt?: Date;
  version: string;
  metadata: Record<string, any>;
}

// ============================================================================
// Billing and Subscription Types
// ============================================================================

export type BillingTier = 'free' | 'pro' | 'enterprise';

// ============================================================================
// API Response Types
// ============================================================================

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
  timestamp: Date;
}

export interface PaginatedResponse<T = any> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

export interface ErrorResponse {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: Date;
  requestId?: string;
}

// ============================================================================
// Zod Schemas for Validation
// ============================================================================

export const UserSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  username: z.string().min(3).max(50),
  displayName: z.string().min(1).max(100),
  profile: z.any(), // UserProfile schema
  preferences: z.any(), // UserPreferences schema
  createdAt: z.date(),
  lastActiveAt: z.date(),
  isActive: z.boolean(),
  verified: z.boolean(),
  roles: z.array(z.string()),
});

export const DeviceSchema = z.object({
  id: z.string().uuid(),
  name: z.string().min(1).max(100),
  type: z.enum(['desktop', 'laptop', 'phone', 'tablet', 'server', 'embedded']),
  platform: z.string(),
  osVersion: z.string(),
  appVersion: z.string(),
  capabilities: z.any(), // DeviceCapabilities schema
  status: z.enum(['online', 'offline', 'busy', 'sleep', 'error']),
  lastSeen: z.date(),
  batteryLevel: z.number().min(0).max(100).optional(),
  isPrimary: z.boolean(),
  location: z.any().optional(), // LocationInfo schema
});

export const TaskSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1).max(200),
  description: z.string().optional(),
  type: z.enum(['general', 'coding', 'writing', 'research', 'analysis', 'design', 'communication', 'automation', 'system']),
  status: z.enum(['todo', 'in_progress', 'review', 'testing', 'completed', 'cancelled', 'blocked']),
  priority: z.enum(['low', 'normal', 'high', 'urgent', 'critical']),
  assignedTo: z.string().uuid().optional(),
  createdBy: z.string().uuid(),
  createdAt: z.date(),
  updatedAt: z.date(),
  dueDate: z.date().optional(),
  completedAt: z.date().optional(),
  estimatedDuration: z.number().positive().optional(),
  actualDuration: z.number().positive().optional(),
  tags: z.array(z.string()),
  dependencies: z.array(z.string().uuid()),
  subtasks: z.array(z.any()), // Task schema
  attachments: z.array(z.any()), // TaskAttachment schema
  context: z.any(), // TaskContext schema
  metadata: z.record(z.any()),
});

export const MemoryItemSchema = z.object({
  id: z.string().uuid(),
  content: z.string(),
  contentType: z.enum(['text', 'image', 'audio', 'video', 'document', 'code', 'structured_data']),
  memoryType: z.enum(['short_term', 'medium_term', 'long_term', 'permanent']),
  importance: z.enum(['low', 'medium', 'high', 'critical']),
  tags: z.array(z.string()),
  metadata: z.record(z.any()),
  createdAt: z.date(),
  lastAccessed: z.date(),
  accessCount: z.number().nonnegative(),
  encrypted: z.boolean(),
  expiresAt: z.date().optional(),
  relatedItems: z.array(z.string().uuid()),
});

// ============================================================================
// Utility Types
// ============================================================================

export type Optional<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;
export type RequiredBy<T, K extends keyof T> = T & Required<Pick<T, K>>;
export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

export type EventCallback<T = any> = (data: T) => void;
export type AsyncCallback<T = any> = (data: T) => Promise<void>;

export interface PaginationOptions {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface SearchOptions {
  query?: string;
  filters?: Record<string, any>;
  pagination?: PaginationOptions;
}