/**
 * Memory and Context Types
 *
 * Types related to memory storage, context management, and data
 * persistence across the MISA.AI platform.
 */

import { z } from 'zod';

// ============================================================================
// Memory Types
// ============================================================================

/**
 * Memory search query
 */
export interface MemorySearchQuery {
  text?: string;
  contentTypes?: ContentType[];
  memoryTypes?: MemoryType[];
  importance?: Importance[];
  tags?: string[];
  dateRange?: {
    start: Date;
    end: Date;
  };
  limit?: number;
  offset?: number;
  sortBy?: SortField;
  sortOrder?: 'asc' | 'desc';
}

export type ContentType = 'text' | 'image' | 'audio' | 'video' | 'document' | 'code' | 'structured_data';
export type MemoryType = 'short_term' | 'medium_term' | 'long_term' | 'permanent';
export type Importance = 'low' | 'medium' | 'high' | 'critical';
export type SortField = 'created_at' | 'last_accessed' | 'access_count' | 'importance';

/**
 * Memory statistics
 */
export interface MemoryStats {
  totalMemories: number;
  shortTermCount: number;
  mediumTermCount: number;
  longTermCount: number;
  permanentCount: number;
  avgAccessCount: number;
  newestMemory?: Date;
  oldestMemory?: Date;
  storageUsed: number;
  storageLimit: number;
}

/**
 * Memory export options
 */
export interface MemoryExportOptions {
  format: 'json' | 'csv' | 'xml' | 'pdf';
  dateRange?: {
    start: Date;
    end: Date;
  };
  contentTypes?: ContentType[];
  includeEncrypted: boolean;
  compress: boolean;
}

/**
 * Memory deletion request
 */
export interface MemoryDeletionRequest {
  memoryIds?: string[];
  dateRange?: {
    start: Date;
    end: Date;
  };
  contentTypes?: ContentType[];
  memoryTypes?: MemoryType[];
  tags?: string[];
  confirm: boolean;
}

/**
 * Memory deletion result
 */
export interface MemoryDeletionResult {
  deletedCount: number;
  failedCount: number;
  errors: string[];
  categoriesDeleted: ContentType[];
  completionTime: Date;
}

// ============================================================================
// Context Types
// ============================================================================

/**
 * Context source information
 */
export interface ContextSource {
  sourceId: string;
  sourceType: ContextSourceType;
  name: string;
  enabled: boolean;
  priority: number;
  lastData?: any;
  lastUpdated: Date;
}

export type ContextSourceType =
  | 'application'
  | 'system'
  | 'sensor'
  | 'browser'
  | 'calendar'
  | 'email'
  | 'chat'
  | 'file'
  | 'location'
  | 'biometric';

/**
 * Context update request
 */
export interface ContextUpdateRequest {
  sourceId: string;
  data: any;
  priority?: number;
  metadata?: Record<string, any>;
}

/**
 * Context fusion settings
 */
export interface ContextFusionSettings {
  enabledSources: string[];
  fusionAlgorithm: FusionAlgorithm;
  relevanceThreshold: number;
  maxContextAge: number; // hours
  compressionEnabled: boolean;
  privacyLevel: PrivacyLevel;
}

export type FusionAlgorithm = 'weighted_average' | 'neural' | 'rule_based' | 'hybrid';
export type PrivacyLevel = 'public' | 'private' | 'sensitive' | 'confidential';

// ============================================================================
// Zod Schemas
// ============================================================================

export const MemorySearchQuerySchema = z.object({
  text: z.string().optional(),
  contentTypes: z.array(z.enum([
    'text', 'image', 'audio', 'video', 'document', 'code', 'structured_data'
  ])).optional(),
  memoryTypes: z.array(z.enum(['short_term', 'medium_term', 'long_term', 'permanent'])).optional(),
  importance: z.array(z.enum(['low', 'medium', 'high', 'critical'])).optional(),
  tags: z.array(z.string()).optional(),
  dateRange: z.object({
    start: z.date(),
    end: z.date()
  }).optional(),
  limit: z.number().positive().optional(),
  offset: z.number().nonnegative().optional(),
  sortBy: z.enum(['created_at', 'last_accessed', 'access_count', 'importance']).optional(),
  sortOrder: z.enum(['asc', 'desc']).optional()
});

export const MemoryStatsSchema = z.object({
  totalMemories: z.number(),
  shortTermCount: z.number(),
  mediumTermCount: z.number(),
  longTermCount: z.number(),
  permanentCount: z.number(),
  avgAccessCount: z.number(),
  newestMemory: z.date().optional(),
  oldestMemory: z.date().optional(),
  storageUsed: z.number(),
  storageLimit: z.number()
});

export const ContextSourceSchema = z.object({
  sourceId: z.string(),
  sourceType: z.enum([
    'application', 'system', 'sensor', 'browser', 'calendar',
    'email', 'chat', 'file', 'location', 'biometric'
  ]),
  name: z.string(),
  enabled: z.boolean(),
  priority: z.number(),
  lastData: z.any().optional(),
  lastUpdated: z.date()
});

export const ContextUpdateRequestSchema = z.object({
  sourceId: z.string(),
  data: z.any(),
  priority: z.number().optional(),
  metadata: z.record(z.any()).optional()
});

export const ContextFusionSettingsSchema = z.object({
  enabledSources: z.array(z.string()),
  fusionAlgorithm: z.enum(['weighted_average', 'neural', 'rule_based', 'hybrid']),
  relevanceThreshold: z.number().min(0).max(1),
  maxContextAge: z.number().positive(),
  compressionEnabled: z.boolean(),
  privacyLevel: z.enum(['public', 'private', 'sensitive', 'confidential'])
});

// ============================================================================
// Re-exports
// ============================================================================

export * from './common'; // Re-export MemoryItem, ContextState, etc.