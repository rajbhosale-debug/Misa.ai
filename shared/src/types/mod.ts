/**
 * MISA.AI Core Types and Interfaces
 *
 * This file contains the core TypeScript types and interfaces used across
 * all MISA.AI platforms (Android, Desktop, Web). These define the data structures
 * for user profiles, applications, plugins, memory, tasks, and workflows.
 */

export * from './auth';
export * from './memory';
export * from './apps';
export * from './plugins';
export * from './devices';
export * from './ai';
export * from './tasks';
export * from './workflows';
export * from './billing';
export * from './privacy';
export * from './common';

// Re-export commonly used types
export type {
  User,
  UserProfile,
  UserPreferences,
  Device,
  Application,
  Plugin,
  Task,
  Workflow,
  MemoryItem,
  ContextState,
  ModelType,
  Permission,
  ConsentRecord,
  BillingTier
} from './common';

// Re-export enums
export type {
  DeviceType,
  ApplicationStatus,
  PluginStatus,
  TaskStatus,
  WorkflowStatus,
  MemoryType,
  Importance,
  ConsentType,
  BillingTier as BillingTierEnum
} from './common';