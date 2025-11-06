import React, { useState, useEffect, useCallback } from 'react';
import {
  UpgradeSyncRequest,
  DataSyncStatus,
  PlatformMigration,
  SyncStatus,
  SyncPhase,
  Platform,
  createDefaultSyncSettings,
  createDefaultDataTransferOptions,
  calculateSyncProgress
} from '../../../shared/src/types/upgrade';

interface UpgradeCoordinatorProps {
  deviceId: string;
  availableDevices: Array<{ id: string; name: string; platform: Platform }>;
  onUpgradeComplete?: (result: any) => void;
  onError?: (error: Error) => void;
}

/**
 * Web-based upgrade coordination interface
 * Provides UI for platform detection, upgrade path recommendations,
 * data sync visualization, and cross-platform pairing setup
 */
export const UpgradeCoordinator: React.FC<UpgradeCoordinatorProps> = ({
  deviceId,
  availableDevices,
  onUpgradeComplete,
  onError
}) => {
  // State management
  const [currentStep, setCurrentStep] = useState<UpgradeStep>('detection');
  const [upgradeRequest, setUpgradeRequest] = useState<UpgradeSyncRequest | null>(null);
  const [syncStatus, setSyncStatus] = useState<DataSyncStatus | null>(null);
  const [selectedTargetDevice, setSelectedTargetDevice] = useState<string>('');
  const [platformAnalysis, setPlatformAnalysis] = useState<PlatformAnalysis | null>(null);
  const [upgradePath, setUpgradePath] = useState<UpgradePath | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false);

  // Step enumeration
  enum UpgradeStep {
    DETECTION = 'detection',
    PATH_SELECTION = 'path_selection',
    CONFIGURATION = 'configuration',
    PREPARATION = 'preparation',
    TRANSFER = 'transfer',
    VERIFICATION = 'verification',
    COMPLETION = 'completion'
  }

  // Platform analysis interface
  interface PlatformAnalysis {
    currentPlatform: Platform;
    availableUpgrades: Array<{
      version: string;
      type: string;
      description: string;
      compatibility: 'full' | 'partial' | 'incompatible';
    }>;
    migrationOptions: Array<{
      targetPlatform: Platform;
      feasibility: 'recommended' | 'possible' | 'not_recommended';
      dataTransferEfficiency: number;
      estimatedTime: number;
    }>;
    systemCompatibility: {
      meetsRequirements: boolean;
      issues: string[];
      recommendations: string[];
    };
  }

  // Upgrade path interface
  interface UpgradePath {
    type: 'upgrade' | 'migration' | 'sync_only';
    sourcePlatform: Platform;
    targetPlatform?: Platform;
    steps: Array<{
      name: string;
      description: string;
      estimatedDuration: number;
      requirements: string[];
    }>;
    totalEstimatedTime: number;
    riskLevel: 'low' | 'medium' | 'high';
  }

  // Initialize component
  useEffect(() => {
    performPlatformDetection();
  }, []);

  // Perform platform detection and analysis
  const performPlatformDetection = async () => {
    try {
      setIsScanning(true);

      // Analyze current platform
      const currentPlatform = detectCurrentPlatform();
      const analysis = await analyzePlatformCapabilities(currentPlatform);

      setPlatformAnalysis(analysis);

      // Recommend upgrade path
      const recommendedPath = await recommendUpgradePath(analysis);
      setUpgradePath(recommendedPath);

      setCurrentStep(UpgradeStep.PATH_SELECTION);
    } catch (error) {
      console.error('Platform detection failed:', error);
      onError?.(error as Error);
    } finally {
      setIsScanning(false);
    }
  };

  // Detect current platform
  const detectCurrentPlatform = (): Platform => {
    const userAgent = navigator.userAgent;

    if (/Android/i.test(userAgent)) return 'android';
    if (/iPhone|iPad|iPod/i.test(userAgent)) return 'ios';
    if (/Win/i.test(userAgent)) return 'windows';
    if (/Mac/i.test(userAgent)) return 'macos';
    if (/Linux/i.test(userAgent)) return 'linux';

    return 'web';
  };

  // Analyze platform capabilities
  const analyzePlatformCapabilities = async (platform: Platform): Promise<PlatformAnalysis> => {
    // This would make API calls to analyze the platform
    // For now, returning mock data

    const systemInfo = await getSystemInfo();

    return {
      currentPlatform: platform,
      availableUpgrades: [
        {
          version: '2.1.0',
          type: 'minor',
          description: 'Performance improvements and bug fixes',
          compatibility: 'full'
        }
      ],
      migrationOptions: availableDevices
        .filter(device => device.platform !== platform)
        .map(device => ({
          targetPlatform: device.platform,
          feasibility: platform === 'web' ? 'not_recommended' : 'possible',
          dataTransferEfficiency: calculateTransferEfficiency(platform, device.platform),
          estimatedTime: estimateMigrationTime(platform, device.platform)
        })),
      systemCompatibility: {
        meetsRequirements: systemInfo.meetsRequirements,
        issues: systemInfo.issues,
        recommendations: systemInfo.recommendations
      }
    };
  };

  // Get system information
  const getSystemInfo = async () => {
    // Mock system info - in real implementation, this would query the system
    return {
      meetsRequirements: true,
      issues: [],
      recommendations: []
    };
  };

  // Calculate transfer efficiency between platforms
  const calculateTransferEfficiency = (source: Platform, target: Platform): number => {
    // Efficiency matrix (0-100)
    const efficiencyMatrix: Record<Platform, Record<Platform, number>> = {
      windows: { windows: 100, macos: 85, linux: 90, android: 60, ios: 55, web: 40 },
      macos: { windows: 85, macos: 100, linux: 90, android: 65, ios: 95, web: 45 },
      linux: { windows: 90, macos: 90, linux: 100, android: 70, ios: 60, web: 50 },
      android: { windows: 60, macos: 65, linux: 70, android: 100, ios: 80, web: 30 },
      ios: { windows: 55, macos: 95, linux: 60, android: 80, ios: 100, web: 35 },
      web: { windows: 40, macos: 45, linux: 50, android: 30, ios: 35, web: 100 }
    };

    return efficiencyMatrix[source]?.[target] || 0;
  };

  // Estimate migration time
  const estimateMigrationTime = (source: Platform, target: Platform): number => {
    // Base time in minutes, modified by efficiency
    const baseTime = 15; // minutes
    const efficiency = calculateTransferEfficiency(source, target);
    return Math.ceil(baseTime * (100 / Math.max(efficiency, 10)));
  };

  // Recommend upgrade path
  const recommendUpgradePath = async (analysis: PlatformAnalysis): Promise<UpgradePath | null> => {
    if (analysis.availableUpgrades.length > 0) {
      return {
        type: 'upgrade',
        sourcePlatform: analysis.currentPlatform,
        steps: [
          {
            name: 'Download Update',
            description: 'Download latest version',
            estimatedDuration: 300,
            requirements: ['Internet connection', 'Storage space']
          },
          {
            name: 'Backup Data',
            description: 'Create backup of current data',
            estimatedDuration: 180,
            requirements: ['Storage space for backup']
          },
          {
            name: 'Install Update',
            description: 'Install the new version',
            estimatedDuration: 240,
            requirements: ['Administrator permissions']
          },
          {
            name: 'Verify Installation',
            description: 'Verify that installation completed successfully',
            estimatedDuration: 60,
            requirements: []
          }
        ],
        totalEstimatedTime: 780, // 13 minutes
        riskLevel: 'low'
      };
    }

    // Check for migration options
    if (analysis.migrationOptions.length > 0) {
      const bestOption = analysis.migrationOptions.reduce((best, current) =>
        current.feasibility === 'recommended' || current.dataTransferEfficiency > best.dataTransferEfficiency
          ? current
          : best
      );

      return {
        type: 'migration',
        sourcePlatform: analysis.currentPlatform,
        targetPlatform: bestOption.targetPlatform,
        steps: [
          {
            name: 'Prepare Target Platform',
            description: 'Ensure target platform is ready',
            estimatedDuration: 300,
            requirements: ['Target device available']
          },
          {
            name: 'Export Data',
            description: 'Export data from current platform',
            estimatedDuration: bestOption.estimatedTime * 60,
            requirements: ['Network connection between devices']
          },
          {
            name: 'Import Data',
            description: 'Import data to target platform',
            estimatedDuration: bestOption.estimatedTime * 60,
            requirements: ['Target platform installed']
          },
          {
            name: 'Verify Migration',
            description: 'Verify data integrity after migration',
            estimatedDuration: 300,
            requirements: []
          }
        ],
        totalEstimatedTime: bestOption.estimatedTime * 120 + 600,
        riskLevel: 'medium'
      };
    }

    return null;
  };

  // Initialize upgrade request
  const initializeUpgradeRequest = useCallback(() => {
    if (!selectedTargetDevice || !upgradePath) return;

    const request: UpgradeSyncRequest = {
      sourceDeviceId: deviceId,
      targetDeviceId: selectedTargetDevice,
      upgradeType: upgradePath.type === 'migration' ? 'platform_migration' : 'major',
      dataTransferOptions: createDefaultDataTransferOptions(),
      syncSettings: createDefaultSyncSettings(),
      priority: 'normal',
      metadata: {
        version: '2.1.0',
        buildNumber: '20241106.1',
        releaseDate: new Date(),
        changelog: 'Enhanced installation coordination and upgrade pairing',
        requirements: {
          minimumRAM: 4,
          recommendedRAM: 8,
          minimumStorage: 10,
          recommendedStorage: 20,
          requiredOSVersion: '10.0',
          supportedArchitectures: ['x64', 'arm64'],
          optionalDependencies: []
        },
        knownIssues: [],
        rollbackAvailable: true,
        estimatedDowntime: 300
      }
    };

    setUpgradeRequest(request);
    setCurrentStep(UpgradeStep.CONFIGURATION);
  }, [deviceId, selectedTargetDevice, upgradePath]);

  // Start upgrade process
  const startUpgradeProcess = async () => {
    if (!upgradeRequest) return;

    try {
      setCurrentStep(UpgradeStep.PREPARATION);

      // Start the upgrade synchronization
      const syncId = await initiateUpgradeSync(upgradeRequest);

      // Monitor the sync progress
      monitorSyncProgress(syncId);

    } catch (error) {
      console.error('Failed to start upgrade:', error);
      onError?.(error as Error);
    }
  };

  // Initiate upgrade sync
  const initiateUpgradeSync = async (request: UpgradeSyncRequest): Promise<string> => {
    // This would make an API call to start the sync
    const response = await fetch('/api/upgrade/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      throw new Error('Failed to initiate upgrade sync');
    }

    const result = await response.json();
    return result.syncId;
  };

  // Monitor sync progress
  const monitorSyncProgress = (syncId: string) => {
    const interval = setInterval(async () => {
      try {
        const response = await fetch(`/api/upgrade/status/${syncId}`);
        const status: DataSyncStatus = await response.json();

        setSyncStatus(status);

        // Update current step based on sync phase
        switch (status.progress.currentPhase) {
          case 'preparation':
            setCurrentStep(UpgradeStep.PREPARATION);
            break;
          case 'transfer':
            setCurrentStep(UpgradeStep.TRANSFER);
            break;
          case 'verification':
            setCurrentStep(UpgradeStep.VERIFICATION);
            break;
          case 'completion':
            setCurrentStep(UpgradeStep.COMPLETION);
            clearInterval(interval);
            onUpgradeComplete?.(status);
            break;
        }

        // Handle errors
        if (status.status === 'failed') {
          clearInterval(interval);
          onError?.(new Error(status.errors[0]?.message || 'Upgrade failed'));
        }

      } catch (error) {
        console.error('Failed to monitor sync progress:', error);
        clearInterval(interval);
      }
    }, 2000);
  };

  // Render platform detection step
  const renderPlatformDetection = () => (
    <div className="space-y-6">
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-blue-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Platform Analysis
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Analyzing your current platform and available upgrade options...
        </p>
      </div>

      {isScanning && (
        <div className="flex flex-col items-center space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="text-gray-600">Scanning system capabilities...</p>
        </div>
      )}
    </div>
  );

  // Render path selection step
  const renderPathSelection = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Choose Upgrade Path
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Select how you want to upgrade your MISA.AI installation
        </p>
      </div>

      {platformAnalysis && upgradePath && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Current Platform Info */}
          <div className="bg-white border border-gray-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Current Platform
            </h3>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Platform:</span>
                <span className="font-medium capitalize">{platformAnalysis.currentPlatform}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Compatible:</span>
                <span className={`font-medium ${platformAnalysis.systemCompatibility.meetsRequirements ? 'text-green-600' : 'text-red-600'}`}>
                  {platformAnalysis.systemCompatibility.meetsRequirements ? 'Yes' : 'No'}
                </span>
              </div>
            </div>

            {platformAnalysis.systemCompatibility.issues.length > 0 && (
              <div className="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                <p className="text-sm text-yellow-800 font-medium">Issues:</p>
                <ul className="text-sm text-yellow-700 list-disc list-inside mt-1">
                  {platformAnalysis.systemCompatibility.issues.map((issue, index) => (
                    <li key={index}>{issue}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          {/* Upgrade Options */}
          <div className="bg-white border border-gray-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Recommended Upgrade
            </h3>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Type:</span>
                <span className="font-medium capitalize">{upgradePath.type}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Risk Level:</span>
                <span className={`font-medium capitalize ${
                  upgradePath.riskLevel === 'low' ? 'text-green-600' :
                  upgradePath.riskLevel === 'medium' ? 'text-yellow-600' : 'text-red-600'
                }`}>
                  {upgradePath.riskLevel}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Est. Time:</span>
                <span className="font-medium">
                  {Math.ceil(upgradePath.totalEstimatedTime / 60)} minutes
                </span>
              </div>
            </div>

            {upgradePath.targetPlatform && (
              <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <p className="text-sm text-blue-800 font-medium">
                  Migration to: {upgradePath.targetPlatform}
                </p>
              </div>
            )}
          </div>

          {/* Migration Options */}
          {upgradePath.type === 'migration' && platformAnalysis.migrationOptions.length > 0 && (
            <div className="md:col-span-2 bg-white border border-gray-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">
                Select Target Device
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {platformAnalysis.migrationOptions.map((option) => {
                  const device = availableDevices.find(d => d.platform === option.targetPlatform);
                  if (!device) return null;

                  return (
                    <div
                      key={option.targetPlatform}
                      className={`border rounded-lg p-4 cursor-pointer transition-colors ${
                        selectedTargetDevice === device.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                      onClick={() => setSelectedTargetDevice(device.id)}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <h4 className="font-medium capitalize">{device.name}</h4>
                        <span className={`text-xs px-2 py-1 rounded-full ${
                          option.feasibility === 'recommended' ? 'bg-green-100 text-green-800' :
                          option.feasibility === 'possible' ? 'bg-yellow-100 text-yellow-800' :
                          'bg-red-100 text-red-800'
                        }`}>
                          {option.feasibility}
                        </span>
                      </div>
                      <div className="text-sm text-gray-600 space-y-1">
                        <p>Efficiency: {option.dataTransferEfficiency}%</p>
                        <p>Est. Time: {option.estimatedTime} min</p>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex justify-between">
        <button
          onClick={() => setCurrentStep(UpgradeStep.DETECTION)}
          className="px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
        >
          Back
        </button>
        <button
          onClick={initializeUpgradeRequest}
          disabled={upgradePath?.type === 'migration' && !selectedTargetDevice}
          className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          Continue
        </button>
      </div>
    </div>
  );

  // Render configuration step
  const renderConfiguration = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Configure Upgrade
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Customize your upgrade settings and data transfer options
        </p>
      </div>

      {upgradeRequest && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Data Transfer Options */}
          <div className="bg-white border border-gray-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Data Transfer Options
            </h3>
            <div className="space-y-4">
              {Object.entries(upgradeRequest.dataTransferOptions).map(([key, value]) => (
                typeof value === 'boolean' && (
                  <label key={key} className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      checked={value}
                      onChange={(e) => {
                        const updatedOptions = {
                          ...upgradeRequest.dataTransferOptions,
                          [key]: e.target.checked
                        };
                        setUpgradeRequest({
                          ...upgradeRequest,
                          dataTransferOptions: updatedOptions
                        });
                      }}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="text-gray-700 capitalize">
                      {key.replace(/([A-Z])/g, ' $1').trim()}
                    </span>
                  </label>
                )
              ))}
            </div>
          </div>

          {/* Sync Settings */}
          <div className="bg-white border border-gray-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Sync Settings
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Retry Attempts
                </label>
                <input
                  type="number"
                  value={upgradeRequest.syncSettings.retryAttempts}
                  onChange={(e) => {
                    setUpgradeRequest({
                      ...upgradeRequest,
                      syncSettings: {
                        ...upgradeRequest.syncSettings,
                        retryAttempts: parseInt(e.target.value) || 0
                      }
                    });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  min="0"
                  max="10"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Conflict Resolution
                </label>
                <select
                  value={upgradeRequest.syncSettings.conflictResolution}
                  onChange={(e) => {
                    setUpgradeRequest({
                      ...upgradeRequest,
                      syncSettings: {
                        ...upgradeRequest.syncSettings,
                        conflictResolution: e.target.value as any
                      }
                    });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="source_wins">Source Wins</option>
                  <option value="target_wins">Target Wins</option>
                  <option value="merge">Merge</option>
                  <option value="timestamp_wins">Most Recent Wins</option>
                  <option value="manual">Manual Review</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Priority
                </label>
                <select
                  value={upgradeRequest.priority}
                  onChange={(e) => {
                    setUpgradeRequest({
                      ...upgradeRequest,
                      priority: e.target.value as any
                    });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="low">Low</option>
                  <option value="normal">Normal</option>
                  <option value="high">High</option>
                  <option value="critical">Critical</option>
                </select>
              </div>
            </div>
          </div>

          {/* Advanced Options Toggle */}
          <div className="lg:col-span-2">
            <button
              onClick={() => setShowAdvancedOptions(!showAdvancedOptions)}
              className="flex items-center space-x-2 text-blue-600 hover:text-blue-700"
            >
              <svg className={`w-4 h-4 transform transition-transform ${showAdvancedOptions ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
              <span>Advanced Options</span>
            </button>

            {showAdvancedOptions && (
              <div className="mt-4 p-4 bg-gray-50 border border-gray-200 rounded-lg">
                <h4 className="font-medium text-gray-900 mb-3">Advanced Configuration</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Bandwidth Limit (MB/s)
                    </label>
                    <input
                      type="number"
                      value={upgradeRequest.syncSettings.bandwidthLimit || ''}
                      onChange={(e) => {
                        setUpgradeRequest({
                          ...upgradeRequest,
                          syncSettings: {
                            ...upgradeRequest.syncSettings,
                            bandwidthLimit: parseInt(e.target.value) || undefined
                          }
                        });
                      }}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      placeholder="No limit"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Compression Level (0-9)
                    </label>
                    <input
                      type="number"
                      value={upgradeRequest.syncSettings.compressionLevel}
                      onChange={(e) => {
                        setUpgradeRequest({
                          ...upgradeRequest,
                          syncSettings: {
                            ...upgradeRequest.syncSettings,
                            compressionLevel: parseInt(e.target.value) || 0
                          }
                        });
                      }}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      min="0"
                      max="9"
                    />
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex justify-between">
        <button
          onClick={() => setCurrentStep(UpgradeStep.PATH_SELECTION)}
          className="px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
        >
          Back
        </button>
        <button
          onClick={startUpgradeProcess}
          className="px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
        >
          Start Upgrade
        </button>
      </div>
    </div>
  );

  // Render transfer progress step
  const renderTransferProgress = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Upgrade in Progress
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Your upgrade is being processed. Please wait...
        </p>
      </div>

      {syncStatus && (
        <div className="max-w-2xl mx-auto space-y-6">
          {/* Overall Progress */}
          <div className="bg-white border border-gray-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Overall Progress
            </h3>

            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Status:</span>
                <span className="font-medium capitalize">{syncStatus.status}</span>
              </div>

              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Current Phase:</span>
                <span className="font-medium capitalize">{syncStatus.progress.currentPhase.replace('_', ' ')}</span>
              </div>

              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Progress:</span>
                <span className="font-medium">{calculateSyncProgress(syncStatus)}%</span>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-gray-200 rounded-full h-3">
                <div
                  className="bg-blue-600 h-3 rounded-full transition-all duration-300"
                  style={{ width: `${calculateSyncProgress(syncStatus)}%` }}
                />
              </div>

              {/* Transfer Statistics */}
              <div className="grid grid-cols-2 gap-4 pt-3">
                <div className="text-center">
                  <p className="text-2xl font-bold text-blue-600">
                    {Math.round(syncStatus.progress.bytesTransferred / (1024 * 1024))}MB
                  </p>
                  <p className="text-sm text-gray-600">Transferred</p>
                </div>
                <div className="text-center">
                  <p className="text-2xl font-bold text-green-600">
                    {Math.round(syncStatus.progress.transferRate / (1024 * 1024))}MB/s
                  </p>
                  <p className="text-sm text-gray-600">Speed</p>
                </div>
              </div>
            </div>
          </div>

          {/* Data Categories */}
          {syncStatus.transferredData.categories.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">
                Data Categories
              </h3>
              <div className="space-y-3">
                {syncStatus.transferredData.categories.map((category, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className={`w-3 h-3 rounded-full ${
                        category.success ? 'bg-green-500' : 'bg-red-500'
                      }`} />
                      <div>
                        <p className="font-medium capitalize">{category.category.replace('_', ' ')}</p>
                        <p className="text-sm text-gray-600">
                          {category.files} files • {Math.round(category.size / (1024 * 1024))}MB
                        </p>
                      </div>
                    </div>
                    {category.error && (
                      <p className="text-sm text-red-600">{category.error}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Errors */}
          {syncStatus.errors.length > 0 && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-red-900 mb-4">
                Errors Encountered
              </h3>
              <div className="space-y-3">
                {syncStatus.errors.slice(0, 3).map((error, index) => (
                  <div key={index} className="text-sm">
                    <p className="font-medium text-red-800">{error.message}</p>
                    {error.suggestedAction && (
                      <p className="text-red-600 mt-1">Suggestion: {error.suggestedAction}</p>
                    )}
                  </div>
                ))}
                {syncStatus.errors.length > 3 && (
                  <p className="text-sm text-red-600">
                    ...and {syncStatus.errors.length - 3} more errors
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );

  // Render completion step
  const renderCompletion = () => (
    <div className="space-y-6">
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-green-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Upgrade Complete!
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Your MISA.AI has been successfully upgraded and is ready to use.
        </p>
      </div>

      {syncStatus && (
        <div className="max-w-2xl mx-auto bg-white border border-gray-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Upgrade Summary
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Total Data Transferred:</span>
              <span className="font-medium">
                {Math.round(syncStatus.transferredData.totalSize / (1024 * 1024))}MB
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Files Transferred:</span>
              <span className="font-medium">{syncStatus.transferredData.totalFiles}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Duration:</span>
              <span className="font-medium">
                {Math.round((new Date().getTime() - syncStatus.startTime.getTime()) / (1000 * 60))} minutes
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Data Integrity:</span>
              <span className={`font-medium ${
                syncStatus.transferredData.checksums.verified ? 'text-green-600' : 'text-red-600'
              }`}>
                {syncStatus.transferredData.checksums.verified ? 'Verified' : 'Failed'}
              </span>
            </div>
          </div>

          <div className="mt-6 pt-6 border-t border-gray-200">
            <h4 className="font-medium text-gray-900 mb-3">Next Steps</h4>
            <ul className="space-y-2 text-sm text-gray-600">
              <li>• Restart MISA.AI to apply the upgrade</li>
              <li>• Verify all your data has been transferred correctly</li>
              <li>• Check for any configuration updates needed</li>
              <li>• Test your workflows to ensure everything works as expected</li>
            </ul>
          </div>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex justify-center">
        <button
          onClick={() => onUpgradeComplete?.(syncStatus)}
          className="px-8 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          Launch MISA.AI
        </button>
      </div>
    </div>
  );

  // Main render logic
  return (
    <div className="max-w-6xl mx-auto p-6">
      {currentStep === UpgradeStep.DETECTION && renderPlatformDetection()}
      {currentStep === UpgradeStep.PATH_SELECTION && renderPathSelection()}
      {currentStep === UpgradeStep.CONFIGURATION && renderConfiguration()}
      {(currentStep === UpgradeStep.PREPARATION || currentStep === UpgradeStep.TRANSFER || currentStep === UpgradeStep.VERIFICATION) && renderTransferProgress()}
      {currentStep === UpgradeStep.COMPLETION && renderCompletion()}
    </div>
  );
};