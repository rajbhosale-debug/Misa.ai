/**
 * Base API Client
 *
 * Provides common functionality for all MISA.AI API clients including
 * HTTP client, WebSocket management, authentication, error handling,
 * retry logic, and request/response interceptors.
 */

import { z } from 'zod';
import {
  ApiResponse,
  ErrorResponse,
  PaginatedResponse,
  PaginationOptions,
  SearchOptions
} from '../types/common';
import { AuthSession } from '../types/auth';

// ============================================================================
// Configuration and Types
// ============================================================================

export interface ApiClientConfig {
  baseURL: string;
  timeout?: number;
  retryAttempts?: number;
  retryDelay?: number;
  enableLogging?: boolean;
  headers?: Record<string, string>;
  interceptors?: Interceptors;
}

export interface Interceptors {
  request?: RequestInterceptor[];
  response?: ResponseInterceptor[];
}

export type RequestInterceptor = (
  config: RequestConfig
) => Promise<RequestConfig> | RequestConfig;

export type ResponseInterceptor = (
  response: ApiResponse<any>
) => Promise<ApiResponse<any>> | ApiResponse<any>;

export interface RequestConfig {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers?: Record<string, string>;
  body?: any;
  timeout?: number;
  retries?: number;
  signal?: AbortSignal;
}

export interface WebSocketConfig {
  url: string;
  protocols?: string[];
  reconnect?: boolean;
  reconnectDelay?: number;
  maxReconnectAttempts?: number;
  heartbeatInterval?: number;
  heartbeatTimeout?: number;
}

// ============================================================================
// HTTP Client
// ============================================================================

/**
 * Base HTTP client with authentication, retries, and error handling
 */
export class BaseHttpClient {
  private config: ApiClientConfig;
  private session?: AuthSession;
  private interceptors: Interceptors;

  constructor(config: ApiClientConfig) {
    this.config = {
      timeout: 30000,
      retryAttempts: 3,
      retryDelay: 1000,
      enableLogging: false,
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'MISA.AI-Client/1.0.0'
      },
      ...config
    };

    this.interceptors = this.config.interceptors || {};
  }

  /**
   * Set authentication session
   */
  setSession(session: AuthSession): void {
    this.session = session;
  }

  /**
   * Clear authentication session
   */
  clearSession(): void {
    this.session = undefined;
  }

  /**
   * Get current session
   */
  getSession(): AuthSession | undefined {
    return this.session;
  }

  /**
   * Make HTTP GET request
   */
  async get<T = any>(
    url: string,
    options?: Partial<RequestConfig>
  ): Promise<ApiResponse<T>> {
    return this.request<T>({ url, method: 'GET', ...options });
  }

  /**
   * Make HTTP POST request
   */
  async post<T = any>(
    url: string,
    data?: any,
    options?: Partial<RequestConfig>
  ): Promise<ApiResponse<T>> {
    return this.request<T>({
      url,
      method: 'POST',
      body: data,
      ...options
    });
  }

  /**
   * Make HTTP PUT request
   */
  async put<T = any>(
    url: string,
    data?: any,
    options?: Partial<RequestConfig>
  ): Promise<ApiResponse<T>> {
    return this.request<T>({
      url,
      method: 'PUT',
      body: data,
      ...options
    });
  }

  /**
   * Make HTTP DELETE request
   */
  async delete<T = any>(
    url: string,
    options?: Partial<RequestConfig>
  ): Promise<ApiResponse<T>> {
    return this.request<T>({ url, method: 'DELETE', ...options });
  }

  /**
   * Make HTTP PATCH request
   */
  async patch<T = any>(
    url: string,
    data?: any,
    options?: Partial<RequestConfig>
  ): Promise<ApiResponse<T>> {
    return this.request<T>({
      url,
      method: 'PATCH',
      body: data,
      ...options
    });
  }

  /**
   * Make paginated request
   */
  async getPaginated<T = any>(
    url: string,
    options?: SearchOptions
  ): Promise<PaginatedResponse<T>> {
    const response = await this.get<PaginatedResponse<T>>(url, {
      headers: this.buildPaginationHeaders(options?.pagination)
    });

    return response.data as PaginatedResponse<T>;
  }

  /**
   * Upload file
   */
  async uploadFile<T = any>(
    url: string,
    file: File | Blob,
    options?: {
      fieldName?: string;
      metadata?: Record<string, any>;
      onProgress?: (progress: number) => void;
    }
  ): Promise<ApiResponse<T>> {
    const formData = new FormData();
    const fieldName = options?.fieldName || 'file';

    formData.append(fieldName, file);

    if (options?.metadata) {
      Object.entries(options.metadata).forEach(([key, value]) => {
        formData.append(key, String(value));
      });
    }

    const config: RequestConfig = {
      url,
      method: 'POST',
      headers: {
        // Don't set Content-Type for FormData (browser sets it with boundary)
      },
      body: formData
    };

    if (options?.onProgress) {
      // For browsers that support upload progress
      return this.requestWithProgress(config, options.onProgress);
    }

    return this.request<T>(config);
  }

  /**
   * Core request method with retries and interceptors
   */
  private async request<T = any>(
    config: RequestConfig
  ): Promise<ApiResponse<T>> {
    let requestConfig = await this.prepareRequest(config);
    let lastError: Error | null = null;

    const maxAttempts = requestConfig.retries ?? this.config.retryAttempts!;

    for (let attempt = 0; attempt <= maxAttempts; attempt++) {
      try {
        const response = await this.executeRequest<T>(requestConfig);
        const processedResponse = await this.processResponse(response);
        return processedResponse;
      } catch (error) {
        lastError = error as Error;

        // Don't retry on certain error types
        if (!this.shouldRetry(error as Error, attempt, maxAttempts)) {
          break;
        }

        // Wait before retrying
        if (attempt < maxAttempts) {
          await this.delay(this.config.retryDelay! * Math.pow(2, attempt));
        }
      }
    }

    throw lastError || new Error('Request failed');
  }

  /**
   * Prepare request with authentication and interceptors
   */
  private async prepareRequest(config: RequestConfig): Promise<RequestConfig> {
    let requestConfig: RequestConfig = {
      url: this.buildUrl(config.url),
      method: config.method,
      headers: { ...this.config.headers, ...config.headers },
      body: config.body,
      timeout: config.timeout ?? this.config.timeout,
      signal: config.signal
    };

    // Add authentication header
    if (this.session) {
      requestConfig.headers = {
        ...requestConfig.headers,
        'Authorization': `Bearer ${this.session.token}`
      };
    }

    // Apply request interceptors
    for (const interceptor of this.interceptors.request || []) {
      requestConfig = await interceptor(requestConfig);
    }

    return requestConfig;
  }

  /**
   * Execute HTTP request
   */
  private async executeRequest<T = any>(
    config: RequestConfig
  ): Promise<ApiResponse<T>> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), config.timeout);

    try {
      const response = await fetch(config.url, {
        method: config.method,
        headers: config.headers,
        body: this.serializeBody(config.body, config.headers),
        signal: config.signal || controller.signal
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const errorData = await this.parseErrorResponse(response);
        throw new ApiError(
          response.status,
          errorData.message || `HTTP ${response.status}`,
          errorData
        );
      }

      const data = await this.parseResponse<T>(response);

      return {
        success: true,
        data,
        timestamp: new Date()
      };
    } catch (error) {
      clearTimeout(timeoutId);
      throw error;
    }
  }

  /**
   * Process response through interceptors
   */
  private async processResponse<T = any>(
    response: ApiResponse<T>
  ): Promise<ApiResponse<T>> {
    let processedResponse = response;

    for (const interceptor of this.interceptors.response || []) {
      processedResponse = await interceptor(processedResponse);
    }

    return processedResponse;
  }

  /**
   * Request with progress tracking (for file uploads)
   */
  private async requestWithProgress<T = any>(
    config: RequestConfig,
    onProgress: (progress: number) => void
  ): Promise<ApiResponse<T>> {
    // This is a simplified implementation
    // In a real browser environment, you would use XMLHttpRequest for progress
    return this.request<T>(config);
  }

  /**
   * Determine if request should be retried
   */
  private shouldRetry(
    error: Error,
    attempt: number,
    maxAttempts: number
  ): boolean {
    if (attempt >= maxAttempts) {
      return false;
    }

    if (error instanceof ApiError) {
      // Don't retry on client errors (4xx)
      if (error.status >= 400 && error.status < 500) {
        return false;
      }

      // Don't retry on specific server errors
      if (error.status === 401 || error.status === 403 || error.status === 404) {
        return false;
      }
    }

    // Retry on network errors and 5xx server errors
    return true;
  }

  /**
   * Parse error response from server
   */
  private async parseErrorResponse(response: Response): Promise<ErrorResponse> {
    try {
      const data = await response.json();
      return {
        code: data.code || `HTTP_${response.status}`,
        message: data.message || response.statusText,
        details: data.details,
        timestamp: new Date(data.timestamp || Date.now()),
        requestId: data.requestId
      };
    } catch {
      return {
        code: `HTTP_${response.status}`,
        message: response.statusText,
        timestamp: new Date()
      };
    }
  }

  /**
   * Parse successful response
   */
  private async parseResponse<T>(response: Response): Promise<T> {
    const contentType = response.headers.get('content-type');

    if (contentType?.includes('application/json')) {
      return response.json();
    }

    if (contentType?.includes('text/')) {
      return response.text() as unknown as T;
    }

    if (contentType?.includes('application/octet-stream') ||
        contentType?.includes('application/')) {
      return response.blob() as unknown as T;
    }

    return response.text() as unknown as T;
  }

  /**
   * Serialize request body
   */
  private serializeBody(
    body: any,
    headers?: Record<string, string>
  ): BodyInit | undefined {
    if (body === null || body === undefined) {
      return undefined;
    }

    const contentType = headers?.['content-type'];

    if (body instanceof FormData || body instanceof Blob || body instanceof File) {
      return body;
    }

    if (typeof body === 'string') {
      return body;
    }

    if (contentType?.includes('application/x-www-form-urlencoded')) {
      return new URLSearchParams(body as Record<string, string>);
    }

    // Default to JSON
    return JSON.stringify(body);
  }

  /**
   * Build full URL
   */
  private buildUrl(path: string): string {
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }

    const baseUrl = this.config.baseURL.endsWith('/')
      ? this.config.baseURL.slice(0, -1)
      : this.config.baseURL;
    const apiPath = path.startsWith('/') ? path : `/${path}`;

    return `${baseUrl}${apiPath}`;
  }

  /**
   * Build pagination headers
   */
  private buildPaginationHeaders(pagination?: PaginationOptions): Record<string, string> {
    if (!pagination) {
      return {};
    }

    const headers: Record<string, string> = {};

    if (pagination.page !== undefined) {
      headers['X-Page'] = String(pagination.page);
    }

    if (pagination.pageSize !== undefined) {
      headers['X-Page-Size'] = String(pagination.pageSize);
    }

    if (pagination.sortBy) {
      headers['X-Sort-By'] = pagination.sortBy;
    }

    if (pagination.sortOrder) {
      headers['X-Sort-Order'] = pagination.sortOrder;
    }

    return headers;
  }

  /**
   * Delay utility for retries
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// ============================================================================
// WebSocket Client
// ============================================================================

/**
 * WebSocket client with reconnection and heartbeat
 */
export class BaseWebSocketClient {
  private config: WebSocketConfig;
  private ws?: WebSocket;
  private reconnectAttempts = 0;
  private heartbeatInterval?: NodeJS.Timeout;
  private heartbeatTimeout?: NodeJS.Timeout;
  private messageQueue: any[] = [];
  private eventListeners: Map<string, Function[]> = new Map();

  constructor(config: WebSocketConfig) {
    this.config = {
      reconnect: true,
      reconnectDelay: 1000,
      maxReconnectAttempts: 5,
      heartbeatInterval: 30000,
      heartbeatTimeout: 5000,
      ...config
    };
  }

  /**
   * Connect to WebSocket
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.config.url, this.config.protocols);

        this.ws.onopen = () => {
          this.reconnectAttempts = 0;
          this.startHeartbeat();
          this.processMessageQueue();
          this.emit('open');
          resolve();
        };

        this.ws.onclose = (event) => {
          this.stopHeartbeat();
          this.emit('close', event);

          if (this.config.reconnect && this.shouldReconnect(event)) {
            this.scheduleReconnect();
          }
        };

        this.ws.onerror = (error) => {
          this.emit('error', error);
          reject(error);
        };

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data);
        };

      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    this.config.reconnect = false;
    this.stopHeartbeat();

    if (this.ws) {
      this.ws.close();
      this.ws = undefined;
    }
  }

  /**
   * Send message
   */
  send(data: any): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    } else {
      // Queue message for when connection is restored
      this.messageQueue.push(data);
    }
  }

  /**
   * Add event listener
   */
  on(event: string, callback: Function): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.push(callback);
    this.eventListeners.set(event, listeners);
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
  }

  /**
   * Emit event to listeners
   */
  private emit(event: string, data?: any): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error(`Error in WebSocket event listener for ${event}:`, error);
      }
    });
  }

  /**
   * Handle incoming message
   */
  private handleMessage(data: string): void {
    try {
      const parsed = JSON.parse(data);
      this.emit('message', parsed);

      if (parsed.type === 'pong') {
        this.resetHeartbeatTimeout();
      }
    } catch (error) {
      this.emit('error', new Error('Invalid JSON message received'));
    }
  }

  /**
   * Process queued messages
   */
  private processMessageQueue(): void {
    while (this.messageQueue.length > 0 && this.ws?.readyState === WebSocket.OPEN) {
      const message = this.messageQueue.shift();
      this.send(message);
    }
  }

  /**
   * Start heartbeat
   */
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'ping' }));
        this.startHeartbeatTimeout();
      }
    }, this.config.heartbeatInterval);
  }

  /**
   * Stop heartbeat
   */
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = undefined;
    }

    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
      this.heartbeatTimeout = undefined;
    }
  }

  /**
   * Start heartbeat timeout
   */
  private startHeartbeatTimeout(): void {
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
    }

    this.heartbeatTimeout = setTimeout(() => {
      this.emit('timeout');
      this.ws?.close();
    }, this.config.heartbeatTimeout);
  }

  /**
   * Reset heartbeat timeout
   */
  private resetHeartbeatTimeout(): void {
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
    }
    this.startHeartbeatTimeout();
  }

  /**
   * Check if should reconnect
   */
  private shouldReconnect(event: CloseEvent): boolean {
    return !event.wasClean && this.reconnectAttempts < (this.config.maxReconnectAttempts || 5);
  }

  /**
   * Schedule reconnection attempt
   */
  private scheduleReconnect(): void {
    const delay = (this.config.reconnectDelay || 1000) * Math.pow(2, this.reconnectAttempts);

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connect().catch(error => {
        console.error('WebSocket reconnection failed:', error);
      });
    }, delay);
  }
}

// ============================================================================
// Custom Error Classes
// ============================================================================

/**
 * API Error with status and details
 */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details?: ErrorResponse
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Network Error for connection issues
 */
export class NetworkError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'NetworkError';
  }
}

/**
 * Timeout Error for request timeouts
 */
export class TimeoutError extends Error {
  constructor(timeout: number) {
    super(`Request timed out after ${timeout}ms`);
    this.name = 'TimeoutError';
  }
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Create HTTP client with default configuration
 */
export function createHttpClient(config: Partial<ApiClientConfig> = {}): BaseHttpClient {
  return new BaseHttpClient({
    baseURL: 'http://localhost:8080/api/v1',
    ...config
  });
}

/**
 * Create WebSocket client with default configuration
 */
export function createWebSocketClient(config: Partial<WebSocketConfig> = {}): BaseWebSocketClient {
  return new BaseWebSocketClient({
    url: 'ws://localhost:8080/ws',
    ...config
  });
}