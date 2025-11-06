import { z } from 'zod';

/**
 * Billing and Subscription Types
 */

export type BillingTier = 'free' | 'pro' | 'enterprise';

export interface SubscriptionPlan {
  id: string;
  name: string;
  tier: BillingTier;
  price: number; // Monthly price in USD
  yearlyPrice?: number; // Yearly price in USD
  currency: string;
  features: SubscriptionFeature[];
  limits: SubscriptionLimits;
  trialDays?: number;
  popular?: boolean;
  description: string;
}

export interface SubscriptionFeature {
  id: string;
  name: string;
  description: string;
  included: boolean;
  icon?: string;
  category: FeatureCategory;
}

export type FeatureCategory =
  | 'ai_models'
  | 'device_management'
  | 'storage'
  | 'applications'
  | 'security'
  | 'support'
  | 'integrations'
  | 'advanced_features';

export interface SubscriptionLimits {
  devices: number;
  storageGB: number;
  apiCallsPerMonth: number;
  tasksPerMonth: number;
  notesPerMonth: number;
  filesPerMonth: number;
  meetingsPerMonth: number;
  plugins: number;
  customWorkflows: number;
  teamMembers: number;
}

export interface UserSubscription {
  id: string;
  userId: string;
  planId: string;
  status: SubscriptionStatus;
  currentPeriodStart: Date;
  currentPeriodEnd: Date;
  cancelAtPeriodEnd: boolean;
  trialEndsAt?: Date;
  renewsAt?: Date;
  createdAt: Date;
  updatedAt: Date;
  metadata: Record<string, any>;
}

export type SubscriptionStatus =
  | 'active'
  | 'trialing'
  | 'past_due'
  | 'canceled'
  | 'unpaid'
  | 'incomplete'
  | 'incomplete_expired';

export interface PaymentMethod {
  id: string;
  type: PaymentMethodType;
  brand?: string; // 'visa', 'mastercard', etc.
  last4?: string;
  expiryMonth?: number;
  expiryYear?: number;
  isDefault: boolean;
  createdAt: Date;
  metadata: Record<string, any>;
}

export type PaymentMethodType =
  | 'card'
  | 'bank_account'
  | 'paypal'
  | 'crypto'
  | 'sepa_debit';

export interface Transaction {
  id: string;
  userId: string;
  subscriptionId?: string;
  invoiceId?: string;
  type: TransactionType;
  amount: number;
  currency: string;
  status: TransactionStatus;
  description: string;
  metadata: Record<string, any>;
  createdAt: Date;
  failedAt?: Date;
  refunded: boolean;
  refundAmount?: number;
}

export type TransactionType =
  | 'subscription'
  | 'invoice'
  | 'one_time'
  | 'refund'
  | 'credit'
  | 'chargeback';

export type TransactionStatus =
  | 'succeeded'
  | 'pending'
  | 'failed'
  | 'refunded'
  | 'partially_refunded'
  | 'void'
  | 'canceled';

export interface Invoice {
  id: string;
  userId: string;
  subscriptionId?: string;
  number: string;
  status: InvoiceStatus;
  amountDue: number;
  amountPaid: number;
  currency: string;
  dueDate: Date;
  paidAt?: Date;
  createdAt: Date;
  items: InvoiceItem[];
  metadata: Record<string, any>;
  downloadUrl?: string;
}

export type InvoiceStatus =
  | 'draft'
  | 'open'
  | 'paid'
  | 'void'
  | 'uncollectible';

export interface InvoiceItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  currency: string;
  period?: {
    start: Date;
    end: Date;
  };
}

export interface UsageMetric {
  id: string;
  userId: string;
  subscriptionId: string;
  metric: UsageMetricType;
  value: number;
  unit: string;
  period: UsagePeriod;
  recordedAt: Date;
}

export type UsageMetricType =
  | 'api_calls'
  | 'tasks'
  | 'notes'
  | 'files'
  | 'meetings'
  | 'storage_gb'
  | 'devices'
  | 'team_members'
  | 'plugins'
  | 'custom_workflows';

export type UsagePeriod = 'daily' | 'weekly' | 'monthly' | 'yearly';

export interface UsageReport {
  userId: string;
  period: UsagePeriod;
  startDate: Date;
  endDate: Date;
  metrics: Record<UsageMetricType, UsageMetric[]>;
  summary: UsageSummary;
  generatedAt: Date;
}

export interface UsageSummary {
  totalCost: number;
  overageCosts: number;
  projectedMonthlyCost: number;
  warnings: UsageWarning[];
}

export interface UsageWarning {
  type: WarningType;
  metric: UsageMetricType;
  current: number;
  limit: number;
  percentageUsed: number;
  message: string;
}

export type WarningType =
  | 'approaching_limit'
  | 'limit_exceeded'
  | 'high_usage'
  | 'unusual_activity';

export interface Coupon {
  id: string;
  code: string;
  type: CouponType;
  amountOff?: number;
  percentOff?: number;
  currency?: string;
  duration?: CouponDuration;
  durationInMonths?: number;
  maxRedemptions?: number;
  redeemed: number;
  validUntil?: Date;
  metadata: Record<string, any>;
  createdAt: Date;
  isActive: boolean;
}

export type CouponType = 'amount_off' | 'percent_off';

export type CouponDuration =
  | 'once'
  | 'repeating'
  | 'forever';

export interface SubscriptionModificationRequest {
  planId: string;
  quantity?: number;
  trialFromPlan?: string;
  prorationBehavior?: ProrationBehavior;
  couponCode?: string;
  metadata?: Record<string, any>;
}

export type ProrationBehavior =
  | 'create_prorations'
  | 'none'
  | 'always_invoice';

export interface Customer {
  id: string;
  email: string;
  name?: string;
  description?: string;
  currency: string;
  balance: number;
  created: Date;
  metadata: Record<string, any>;
}

export interface BillingPortalConfig {
  returnUrl: string;
  businessProfile?: {
    businessName?: string;
    privacyPolicyUrl?: string;
    termsOfServiceUrl?: string;
  };
}

// Predefined subscription plans
export const SUBSCRIPTION_PLANS: SubscriptionPlan[] = [
  {
    id: 'free',
    name: 'Free',
    tier: 'free',
    price: 0,
    currency: 'USD',
    description: 'Perfect for personal use with basic AI assistance',
    features: [
      {
        id: 'local_ai',
        name: 'Local AI Models',
        description: 'Access to Ollama local models',
        included: true,
        category: 'ai_models',
        icon: '🤖'
      },
      {
        id: 'basic_apps',
        name: 'Core Applications',
        description: 'Calendar, Notes, and Tasks',
        included: true,
        category: 'applications',
        icon: '📱'
      },
      {
        id: 'single_device',
        name: 'Single Device',
        description: 'Use on one device at a time',
        included: true,
        category: 'device_management',
        icon: '💻'
      }
    ],
    limits: {
      devices: 1,
      storageGB: 1,
      apiCallsPerMonth: 1000,
      tasksPerMonth: 100,
      notesPerMonth: 50,
      filesPerMonth: 10,
      meetingsPerMonth: 0,
      plugins: 0,
      customWorkflows: 0,
      teamMembers: 1
    },
    trialDays: 14
  },
  {
    id: 'pro',
    name: 'Pro',
    tier: 'pro',
    price: 9.99,
    yearlyPrice: 99.99,
    currency: 'USD',
    popular: true,
    description: 'Advanced AI with unlimited device sync and premium features',
    features: [
      {
        id: 'cloud_ai',
        name: 'Cloud AI Models',
        description: 'GPT-4, Claude, and other premium models',
        included: true,
        category: 'ai_models',
        icon: '☁️'
      },
      {
        id: 'all_apps',
        name: 'All 18 Applications',
        description: 'Complete productivity suite',
        included: true,
        category: 'applications',
        icon: '🎯'
      },
      {
        id: 'multi_device',
        name: 'Multi-Device Sync',
        description: 'Sync across up to 5 devices',
        included: true,
        category: 'device_management',
        icon: '🔄'
      },
      {
        id: 'cloud_sync',
        name: 'Cloud Sync',
        description: 'Encrypted cloud storage and sync',
        included: true,
        category: 'storage',
        icon: '☁️'
      },
      {
        id: 'remote_desktop',
        name: 'Remote Desktop',
        description: 'Control devices remotely',
        included: true,
        category: 'advanced_features',
        icon: '🖥️'
      },
      {
        id: 'priority_support',
        name: 'Priority Support',
        description: '24/7 email support',
        included: true,
        category: 'support',
        icon: '💬'
      }
    ],
    limits: {
      devices: 5,
      storageGB: 100,
      apiCallsPerMonth: 50000,
      tasksPerMonth: 5000,
      notesPerMonth: 10000,
      filesPerMonth: 1000,
      meetingsPerMonth: 100,
      plugins: 25,
      customWorkflows: 10,
      teamMembers: 1
    },
    trialDays: 14
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    tier: 'enterprise',
    price: 49,
    yearlyPrice: 490,
    currency: 'USD',
    description: 'Complete solution with custom integrations and enterprise support',
    features: [
      {
        id: 'unlimited_everything',
        name: 'Unlimited Access',
        description: 'No limits on any features',
        included: true,
        category: 'ai_models',
        icon: '♾️'
      },
      {
        id: 'custom_models',
        name: 'Custom AI Models',
        description: 'Train and deploy custom models',
        included: true,
        category: 'ai_models',
        icon: '🎨'
      },
      {
        id: 'unlimited_devices',
        name: 'Unlimited Devices',
        description: 'Sync across unlimited devices',
        included: true,
        category: 'device_management',
        icon: '♾️'
      },
      {
        id: 'enterprise_integrations',
        name: 'Enterprise Integrations',
        description: 'Slack, Notion, Salesforce, and more',
        included: true,
        category: 'integrations',
        icon: '🔗'
      },
      {
        id: 'advanced_security',
        name: 'Advanced Security',
        description: 'SSO, audit logs, compliance tools',
        included: true,
        category: 'security',
        icon: '🔒'
      },
      {
        id: 'custom_workflows',
        name: 'Custom Workflows',
        description: 'Unlimited custom workflows',
        included: true,
        category: 'advanced_features',
        icon: '⚙️'
      },
      {
        id: 'white_glove',
        name: 'White-Glove Support',
        description: 'Dedicated account manager and 24/7 support',
        included: true,
        category: 'support',
        icon: '👔'
      },
      {
        id: 'onprem_deployment',
        name: 'On-Premises Option',
        description: 'Deploy in your own infrastructure',
        included: true,
        category: 'advanced_features',
        icon: '🏢'
      }
    ],
    limits: {
      devices: -1, // Unlimited
      storageGB: -1, // Unlimited
      apiCallsPerMonth: -1,
      tasksPerMonth: -1,
      notesPerMonth: -1,
      filesPerMonth: -1,
      meetingsPerMonth: -1,
      plugins: -1,
      customWorkflows: -1,
      teamMembers: 100
    }
  }
];

// Zod schemas
export const SubscriptionPlanSchema = z.object({
  id: z.string(),
  name: z.string(),
  tier: z.enum(['free', 'pro', 'enterprise']),
  price: z.number().min(0),
  yearlyPrice: z.number().min(0).optional(),
  currency: z.string().length(3),
  features: z.array(z.object({
    id: z.string(),
    name: z.string(),
    description: z.string(),
    included: z.boolean(),
    icon: z.string().optional(),
    category: z.enum([
      'ai_models', 'device_management', 'storage', 'applications',
      'security', 'support', 'integrations', 'advanced_features'
    ])
  })),
  limits: z.object({
    devices: z.number().min(-1),
    storageGB: z.number().min(-1),
    apiCallsPerMonth: z.number().min(-1),
    tasksPerMonth: z.number().min(-1),
    notesPerMonth: z.number().min(-1),
    filesPerMonth: z.number().min(-1),
    meetingsPerMonth: z.number().min(-1),
    plugins: z.number().min(-1),
    customWorkflows: z.number().min(-1),
    teamMembers: z.number().min(1)
  }),
  trialDays: z.number().positive().optional(),
  popular: z.boolean().optional(),
  description: z.string()
});

export const UserSubscriptionSchema = z.object({
  id: z.string(),
  userId: z.string(),
  planId: z.string(),
  status: z.enum(['active', 'trialing', 'past_due', 'canceled', 'unpaid', 'incomplete', 'incomplete_expired']),
  currentPeriodStart: z.date(),
  currentPeriodEnd: z.date(),
  cancelAtPeriodEnd: z.boolean(),
  trialEndsAt: z.date().optional(),
  renewsAt: z.date().optional(),
  createdAt: z.date(),
  updatedAt: z.date(),
  metadata: z.record(z.any())
});

export const PaymentMethodSchema = z.object({
  id: z.string(),
  type: z.enum(['card', 'bank_account', 'paypal', 'crypto', 'sepa_debit']),
  brand: z.string().optional(),
  last4: z.string().optional(),
  expiryMonth: z.number().min(1).max(12).optional(),
  expiryYear: z.number().min(new Date().getFullYear()).optional(),
  isDefault: z.boolean(),
  createdAt: z.date(),
  metadata: z.record(z.any())
});

export const TransactionSchema = z.object({
  id: z.string(),
  userId: z.string(),
  subscriptionId: z.string().optional(),
  invoiceId: z.string().optional(),
  type: z.enum(['subscription', 'invoice', 'one_time', 'refund', 'credit', 'chargeback']),
  amount: z.number(),
  currency: z.string().length(3),
  status: z.enum(['succeeded', 'pending', 'failed', 'refunded', 'partially_refunded', 'void', 'canceled']),
  description: z.string(),
  metadata: z.record(z.any()),
  createdAt: z.date(),
  failedAt: z.date().optional(),
  refunded: z.boolean(),
  refundAmount: z.number().optional()
});

export const InvoiceSchema = z.object({
  id: z.string(),
  userId: z.string(),
  subscriptionId: z.string().optional(),
  number: z.string(),
  status: z.enum(['draft', 'open', 'paid', 'void', 'uncollectible']),
  amountDue: z.number(),
  amountPaid: z.number(),
  currency: z.string().length(3),
  dueDate: z.date(),
  paidAt: z.date().optional(),
  createdAt: z.date(),
  items: z.array(z.object({
    id: z.string(),
    description: z.string(),
    quantity: z.number().positive(),
    unitPrice: z.number(),
    amount: z.number(),
    currency: z.string().length(3),
    period: z.object({
      start: z.date(),
      end: z.date()
    }).optional()
  })),
  metadata: z.record(z.any()),
  downloadUrl: z.string().url().optional()
});