/**
 * MISA.AI Kernel Client
 *
 * TypeScript client for communicating with the MISA.AI kernel.
 * Provides methods for model management, task routing, device communication,
 * and system control.
 */

import { z } from 'zod';
import { BaseHttpClient, BaseWebSocketClient, createWebSocketClient } from './base-client';
import { ApiClientConfig, WebSocketConfig } from './base-client';
import {
  ApiResponse,
  Task,
  TaskResponse,
  ModelType,
  TaskPriority,
  Device,
  ContextState
} from '../types/common';

// ============================================================================
// Kernel API Types
// ============================================================================

/**
 * Model information
 */
export interface ModelInfo {
  id: string;
  name: string;
  type: ModelType;
  provider: string;
  capabilities: ModelCapabilities;
  status: ModelStatus;
  loaded: boolean;
  size?: number;
  description?: string;
}

export interface ModelCapabilities {
  supportsFunctions: boolean;
  supportsVision: boolean;
  supportsStreaming: boolean;
  maxContextLength: number;
  supportsSystemPrompts: boolean;
  supportsJsonMode: boolean;
  languages: string[];
  specialties: string[];
}

export type ModelStatus = 'available' | 'loading' | 'error' | 'unavailable';

/**
 * Task execution request
 */
export interface TaskRequest {
  task: string;
  taskType: string;
  context?: Record<string, any>;
  devicePreferences?: string[];
  priority?: TaskPriority;
  modelId?: string;
  stream?: boolean;
  maxTokens?: number;
  temperature?: number;
  tools?: ToolCall[];
}

/**
 * Tool call for function calling
 */
export interface ToolCall {
  id: string;
  name: string;
  description: string;
  parameters: Record<string, any>;
  required?: string[];
}

/**
 * Model switching preferences
 */
export interface ModelSwitchingPreferences {
  preferLocal: boolean;
  gpuThreshold: number;
  costOptimization: number;
  qualityOptimization: number;
}

/**
 * System status information
 */
export interface SystemStatus {
  status: 'healthy' | 'degraded' | 'error';
  version: string;
  uptime: number;
  loadedModels: string[];
  activeConnections: number;
  memoryUsage: MemoryUsage;
  cpuUsage: number;
  timestamp: Date;
}

export interface MemoryUsage {
  total: number;
  used: number;
  available: number;
  percentage: number;
}

/**
 * Kernel statistics
 */
export interface KernelStats {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  averageResponseTime: number;
  modelUsageStats: Record<string, ModelUsageStats>;
  deviceStats: Record<string, DeviceUsageStats>;
  uptime: number;
  timestamp: Date;
}

export interface ModelUsageStats {
  requestCount: number;
  successRate: number;
  averageResponseTime: number;
  tokensUsed: number;
  lastUsed: Date;
}

export interface DeviceUsageStats {
  tasksAssigned: number;
  tasksCompleted: number;
  averageCompletionTime: number;
  lastActivity: Date;
}

// ============================================================================
// Kernel Client
// ============================================================================

/**
 * Client for communicating with the MISA.AI kernel
 */
export class MisaKernelClient {
  private httpClient: BaseHttpClient;
  private wsClient: BaseWebSocketClient;
  private eventListeners: Map<string, Function[]> = new Map();

  constructor(
    httpConfig: ApiClientConfig,
    wsConfig?: WebSocketConfig
  ) {
    this.httpClient = new BaseHttpClient(httpConfig);

    const defaultWsConfig: WebSocketConfig = {
      url: httpConfig.baseURL.replace('http', 'ws') + '/ws',
      reconnect: true,
      heartbeatInterval: 30000
    };

    this.wsClient = createWebSocketClient({ ...defaultWsConfig, ...wsConfig });

    this.setupWebSocketHandlers();
  }

  // ============================================================================
  // Model Management
  // ============================================================================

  /**
   * Get list of available models
   */
  async getModels(): Promise<ApiResponse<ModelInfo[]>> {
    return this.httpClient.get<ModelInfo[]>('/models');
  }

  /**
   * Get model information
   */
  async getModel(modelId: string): Promise<ApiResponse<ModelInfo>> {
    return this.httpClient.get<ModelInfo>(`/models/${modelId}`);
  }

  /**
   * Switch to a different model
   */
  async switchModel(
    modelId: string,
    taskType?: string,
    preferences?: ModelSwitchingPreferences
  ): Promise<ApiResponse<string>> {
    return this.httpClient.post<string>('/kernel/switch_model', {
      modelId,
      taskType,
      preferences
    });
  }

  /**
   * Load a model
   */
  async loadModel(modelId: string): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>(`/models/${modelId}/load`);
  }

  /**
   * Unload a model
   */
  async unloadModel(modelId: string): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>(`/models/${modelId}/unload`);
  }

  /**
   * Get model performance statistics
   */
  async getModelStats(modelId: string): Promise<ApiResponse<ModelUsageStats>> {
    return this.httpClient.get<ModelUsageStats>(`/models/${modelId}/stats`);
  }

  // ============================================================================
  // Task Management
  // ============================================================================

  /**
   * Execute a task
   */
  async executeTask(request: TaskRequest): Promise<ApiResponse<TaskResponse>> {
    return this.httpClient.post<TaskResponse>('/kernel/route_task', request);
  }

  /**
   * Execute task with streaming response
   */
  async executeTaskStream(
    request: TaskRequest,
    onChunk: (chunk: string) => void
  ): Promise<void> {
    const streamRequest = { ...request, stream: true };

    const response = await fetch(
      `${this.httpClient['config'].baseURL}/kernel/route_task_stream`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.httpClient.getSession()?.token}`
        },
        body: JSON.stringify(streamRequest)
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('Response body is not readable');
    }

    const decoder = new TextDecoder();

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data === '[DONE]') {
              return;
            }
            onChunk(data);
          }
        }
      }
    } finally {
      reader.releaseLock();
    }
  }

  /**
   * Get active tasks
   */
  async getActiveTasks(): Promise<ApiResponse<Task[]>> {
    return this.httpClient.get<Task[]>('/tasks/active');
  }

  /**
   * Get task history
   */
  async getTaskHistory(options?: {
    limit?: number;
    offset?: number;
    status?: string;
  }): Promise<ApiResponse<Task[]>> {
    return this.httpClient.get<Task[]>('/tasks/history', {
      headers: options ? { 'X-Options': JSON.stringify(options) } : undefined
    });
  }

  /**
   * Cancel a task
   */
  async cancelTask(taskId: string): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>(`/tasks/${taskId}/cancel`);
  }

  // ============================================================================
  // Device Management
  // ============================================================================

  /**
   * Get list of connected devices
   */
  async getDevices(): Promise<ApiResponse<Device[]>> {
    return this.httpClient.get<Device[]>('/devices');
  }

  /**
   * Get device information
   */
  async getDevice(deviceId: string): Promise<ApiResponse<Device>> {
    return this.httpClient.get<Device>(`/devices/${deviceId}`);
  }

  /**
   * Pair with a new device
   */
  async pairDevice(qrToken: string): Promise<ApiResponse<{ deviceId: string }>> {
    return this.httpClient.post<{ deviceId: string }>('/devices/pair', {
      qrToken
    });
  }

  /**
   * Generate pairing QR code
   */
  async generatePairingCode(): Promise<ApiResponse<{ qrToken: string; qrCode: string }>> {
    return this.httpClient.post<{ qrToken: string; qrCode: string }>('/devices/generate_pairing');
  }

  /**
   * Remove device pairing
   */
  async unpairDevice(deviceId: string): Promise<ApiResponse<void>> {
    return this.httpClient.delete<void>(`/devices/${deviceId}`);
  }

  /**
   * Start remote desktop session
   */
  async startRemoteDesktop(
    deviceId: string,
    options?: {
      resolution?: { width: number; height: number };
      quality?: 'low' | 'medium' | 'high' | 'ultra';
    }
  ): Promise<ApiResponse<{ sessionId: string }>> {
    return this.httpClient.post<{ sessionId: string }>(`/devices/${deviceId}/remote_desktop`, options);
  }

  /**
   * Stop remote desktop session
   */
  async stopRemoteDesktop(sessionId: string): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>(`/remote_desktop/${sessionId}/stop`);
  }

  // ============================================================================
  // Context and Memory
  // ============================================================================

  /**
   * Get current context
   */
  async getCurrentContext(): Promise<ApiResponse<ContextState>> {
    return this.httpClient.get<ContextState>('/context/current');
  }

  /**
   * Update context
   */
  async updateContext(
    sourceId: string,
    data: Record<string, any>
  ): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>(`/context/sources/${sourceId}`, { data });
  }

  /**
   * Get context history
   */
  async getContextHistory(options?: {
    startTime?: Date;
    endTime?: Date;
    limit?: number;
  }): Promise<ApiResponse<ContextState[]>> {
    return this.httpClient.get<ContextState[]>('/context/history', {
      headers: options ? { 'X-Options': JSON.stringify(options) } : undefined
    });
  }

  // ============================================================================
  // System Management
  // ============================================================================

  /**
   * Get system status
   */
  async getSystemStatus(): Promise<ApiResponse<SystemStatus>> {
    return this.httpClient.get<SystemStatus>('/system/status');
  }

  /**
   * Get kernel statistics
   */
  async getKernelStats(options?: {
    startTime?: Date;
    endTime?: Date;
  }): Promise<ApiResponse<KernelStats>> {
    return this.httpClient.get<KernelStats>('/system/stats', {
      headers: options ? { 'X-Options': JSON.stringify(options) } : undefined
    });
  }

  /**
   * Health check
   */
  async healthCheck(): Promise<ApiResponse<{ status: string; version: string }>> {
    return this.httpClient.get<{ status: string; version: string }>('/health');
  }

  /**
   * Restart kernel
   */
  async restartKernel(): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>('/system/restart');
  }

  /**
   * Shutdown kernel
   */
  async shutdownKernel(): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>('/system/shutdown');
  }

  // ============================================================================
  // Configuration
  // ============================================================================

  /**
   * Get kernel configuration
   */
  async getConfig(): Promise<ApiResponse<Record<string, any>>> {
    return this.httpClient.get<Record<string, any>>('/config');
  }

  /**
   * Update kernel configuration
   */
  async updateConfig(config: Record<string, any>): Promise<ApiResponse<void>> {
    return this.httpClient.put<void>('/config', config);
  }

  /**
   * Reset configuration to defaults
   */
  async resetConfig(): Promise<ApiResponse<void>> {
    return this.httpClient.post<void>('/config/reset');
  }

  // ============================================================================
  // WebSocket Events
  // ============================================================================

  /**
   * Connect to WebSocket for real-time updates
   */
  async connectWebSocket(): Promise<void> {
    await this.wsClient.connect();
  }

  /**
   * Disconnect from WebSocket
   */
  disconnectWebSocket(): void {
    this.wsClient.disconnect();
  }

  /**
   * Send message via WebSocket
   */
  sendWebSocketMessage(message: any): void {
    this.wsClient.send(message);
  }

  /**
   * Add event listener for WebSocket events
   */
  on(event: string, callback: Function): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.push(callback);
    this.eventListeners.set(event, listeners);

    this.wsClient.on(event, callback);
  }

  /**
   * Remove event listener
   */
  off(event: string, callback: Function): void {
    const listeners = this.eventListeners.get(event) || [];
    const index = listeners.indexOf(callback);
    if (index > -1) {
      listeners.splice(index, 1);
      this.eventListeners.set(event, listeners);
    }

    this.wsClient.off(event, callback);
  }

  // ============================================================================
  // Private Methods
  // ============================================================================

  /**
   * Setup WebSocket event handlers
   */
  private setupWebSocketHandlers(): void {
    this.wsClient.on('open', () => {
      this.emit('websocket:connected');
    });

    this.wsClient.on('close', () => {
      this.emit('websocket:disconnected');
    });

    this.wsClient.on('message', (data: any) => {
      this.handleWebSocketMessage(data);
    });

    this.wsClient.on('error', (error: any) => {
      this.emit('websocket:error', error);
    });

    this.wsClient.on('timeout', () => {
      this.emit('websocket:timeout');
    });
  }

  /**
   * Handle incoming WebSocket messages
   */
  private handleWebSocketMessage(data: any): void {
    if (data.type) {
      this.emit(`websocket:${data.type}`, data);
    }

    // Handle specific message types
    switch (data.type) {
      case 'task_update':
        this.emit('task:update', data);
        break;
      case 'model_loaded':
        this.emit('model:loaded', data);
        break;
      case 'device_connected':
        this.emit('device:connected', data);
        break;
      case 'device_disconnected':
        this.emit('device:disconnected', data);
        break;
      case 'system_status_update':
        this.emit('system:status', data);
        break;
      case 'context_update':
        this.emit('context:update', data);
        break;
      default:
        this.emit('websocket:message', data);
    }
  }

  /**
   * Emit event to registered listeners
   */
  private emit(event: string, data?: any): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error(`Error in kernel client event listener for ${event}:`, error);
      }
    });
  }
}

// ============================================================================
// Zod Schemas
// ============================================================================

export const ModelInfoSchema = z.object({
  id: z.string(),
  name: z.string(),
  type: z.enum([
    'chat', 'coding', 'vision', 'reasoning', 'summarization',
    'speech_to_text', 'text_to_speech', 'embedding', 'multimodal'
  ]),
  provider: z.string(),
  capabilities: z.object({
    supportsFunctions: z.boolean(),
    supportsVision: z.boolean(),
    supportsStreaming: z.boolean(),
    maxContextLength: z.number(),
    supportsSystemPrompts: z.boolean(),
    supportsJsonMode: z.boolean(),
    languages: z.array(z.string()),
    specialties: z.array(z.string())
  }),
  status: z.enum(['available', 'loading', 'error', 'unavailable']),
  loaded: z.boolean(),
  size: z.number().optional(),
  description: z.string().optional()
});

export const TaskRequestSchema = z.object({
  task: z.string(),
  taskType: z.string(),
  context: z.record(z.any()).optional(),
  devicePreferences: z.array(z.string()).optional(),
  priority: z.enum(['low', 'normal', 'high', 'urgent', 'critical']).optional(),
  modelId: z.string().optional(),
  stream: z.boolean().optional(),
  maxTokens: z.number().positive().optional(),
  temperature: z.number().min(0).max(2).optional(),
  tools: z.array(z.object({
    id: z.string(),
    name: z.string(),
    description: z.string(),
    parameters: z.record(z.any()),
    required: z.array(z.string()).optional()
  })).optional()
});

export const ModelSwitchingPreferencesSchema = z.object({
  preferLocal: z.boolean(),
  gpuThreshold: z.number().min(0).max(1),
  costOptimization: z.number().min(0).max(1),
  qualityOptimization: z.number().min(0).max(1)
});

export const SystemStatusSchema = z.object({
  status: z.enum(['healthy', 'degraded', 'error']),
  version: z.string(),
  uptime: z.number(),
  loadedModels: z.array(z.string()),
  activeConnections: z.number(),
  memoryUsage: z.object({
    total: z.number(),
    used: z.number(),
    available: z.number(),
    percentage: z.number()
  }),
  cpuUsage: z.number(),
  timestamp: z.date()
});

// ============================================================================
// Factory Functions
// ============================================================================

/**
 * Create kernel client with default configuration
 */
export function createKernelClient(
  config?: Partial<ApiClientConfig>,
  wsConfig?: Partial<WebSocketConfig>
): MisaKernelClient {
  const defaultHttpConfig: ApiClientConfig = {
    baseURL: 'http://localhost:8080/api/v1',
    timeout: 30000,
    retryAttempts: 3
  };

  const defaultWsConfig: WebSocketConfig = {
    url: 'ws://localhost:8080/ws',
    reconnect: true,
    heartbeatInterval: 30000
  };

  return new MisaKernelClient(
    { ...defaultHttpConfig, ...config },
    { ...defaultWsConfig, ...wsConfig }
  );
}