import React, { useState, useEffect } from 'react';
import { CheckCircle, Circle, Settings, Smartphone, Monitor, Wifi, Shield, ChevronRight } from 'lucide-react';

interface ExpressSetupProps {
  onComplete: (setupData: any) => void;
  onSkip: () => void;
  onAdvancedSetup: () => void;
}

interface SetupStep {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  status: 'pending' | 'in_progress' | 'completed' | 'error';
  optional?: boolean;
}

export const ExpressSetup: React.FC<ExpressSetupProps> = ({
  onComplete,
  onSkip,
  onAdvancedSetup
}) => {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [isSettingUp, setIsSettingUp] = useState(false);
  const [setupProgress, setSetupProgress] = useState(0);
  const [deviceName, setDeviceName] = useState('');
  const [detectedDevices, setDetectedDevices] = useState<any[]>([]);

  const [steps, setSteps] = useState<SetupStep[]>([
    {
      id: 'privacy',
      title: 'Privacy-First Setup',
      description: 'All data processing happens locally. No telemetry or tracking.',
      icon: <Shield className="w-5 h-5" />,
      status: 'pending'
    },
    {
      id: 'device',
      title: 'Device Configuration',
      description: 'Configure your device name and enable discovery.',
      icon: <Monitor className="w-5 h-5" />,
      status: 'pending'
    },
    {
      id: 'discovery',
      title: 'Multi-Device Discovery',
      description: 'Scanning for nearby devices and enabling automatic pairing.',
      icon: <Wifi className="w-5 h-5" />,
      status: 'pending',
      optional: true
    },
    {
      id: 'applications',
      title: 'Application Setup',
      description: 'Installing essential applications and AI models.',
      icon: <Smartphone className="w-5 h-5" />,
      status: 'pending'
    },
    {
      id: 'completion',
      title: 'Final Setup',
      description: 'Completing configuration and launching your AI assistant.',
      icon: <CheckCircle className="w-5 h-5" />,
      status: 'pending'
    }
  ]);

  // Auto-detect device name on mount
  useEffect(() => {
    const autoDetectDeviceName = () => {
      const userAgent = navigator.userAgent;
      const platform = navigator.platform;

      let detectedName = 'MISA Device';

      if (platform.includes('Win')) {
        detectedName = `${platform.split(' ')[0]} Desktop`;
      } else if (platform.includes('Mac')) {
        detectedName = 'Mac Desktop';
      } else if (platform.includes('Linux')) {
        detectedName = 'Linux Desktop';
      } else if (/Android/.test(userAgent)) {
        detectedName = 'Android Device';
      } else if (/iPhone|iPad/.test(userAgent)) {
        detectedName = 'iOS Device';
      }

      setDeviceName(detectedName);
    };

    autoDetectDeviceName();

    // Start device discovery
    startDeviceDiscovery();

    return () => {
      // Cleanup if component unmounts
    };
  }, []);

  const startDeviceDiscovery = async () => {
    try {
      // Simulate device discovery
      setSteps(prev => prev.map(step =>
        step.id === 'discovery'
          ? { ...step, status: 'in_progress' }
          : step
      ));

      // In real implementation, this would use WebSocket/UDP discovery
      setTimeout(() => {
        const mockDevices = [
          { id: 'phone-1', name: 'iPhone 14', type: 'mobile', status: 'online' },
          { id: 'laptop-1', name: 'Work Laptop', type: 'desktop', status: 'online' }
        ];
        setDetectedDevices(mockDevices);

        setSteps(prev => prev.map(step =>
          step.id === 'discovery'
            ? { ...step, status: 'completed' }
            : step
        ));
      }, 3000);
    } catch (error) {
      console.error('Device discovery failed:', error);
      setSteps(prev => prev.map(step =>
        step.id === 'discovery'
          ? { ...step, status: 'error' }
          : step
      ));
    }
  };

  const startExpressSetup = async () => {
    setIsSettingUp(true);
    setSetupProgress(0);

    try {
      for (let i = 0; i < steps.length; i++) {
        const step = steps[i];

        // Update step status to in_progress
        setSteps(prev => prev.map(s =>
          s.id === step.id
            ? { ...s, status: 'in_progress' }
            : s
        ));

        setCurrentStepIndex(i);
        setSetupProgress((i / steps.length) * 100);

        // Execute step logic
        await executeStep(step.id);

        // Mark step as completed
        setSteps(prev => prev.map(s =>
          s.id === step.id
            ? { ...s, status: 'completed' }
            : s
        ));

        // Small delay between steps for visual feedback
        await new Promise(resolve => setTimeout(resolve, 1000));
      }

      // Setup complete
      setSetupProgress(100);

      const setupData = {
        deviceName,
        privacy: {
          dataCollection: false,
          crashReports: false,
          analytics: false,
          localOnlyProcessing: true
        },
        device: {
          deviceName,
          enableSync: detectedDevices.length > 0,
          remoteDesktop: true,
          autoDiscovery: true
        },
        applications: {
          enabledApps: ['notes', 'tasks', 'calendar'],
          defaultModel: 'mixtral',
          voiceEnabled: true,
          autoDownloadModels: true
        },
        detectedDevices
      };

      onComplete(setupData);
    } catch (error) {
      console.error('Express setup failed:', error);
      setIsSettingUp(false);

      // Mark current step as failed
      setSteps(prev => prev.map(s =>
        s.id === steps[currentStepIndex].id
          ? { ...s, status: 'error' }
          : s
      ));
    }
  };

  const executeStep = async (stepId: string): Promise<void> => {
    switch (stepId) {
      case 'privacy':
        // Configure privacy settings (all disabled by default)
        await new Promise(resolve => setTimeout(resolve, 1500));
        break;

      case 'device':
        // Configure device settings
        if (!deviceName.trim()) {
          throw new Error('Device name is required');
        }
        await new Promise(resolve => setTimeout(resolve, 1000));
        break;

      case 'discovery':
        // Device discovery already started in useEffect
        await new Promise(resolve => setTimeout(resolve, 2000));
        break;

      case 'applications':
        // Install essential applications and download models
        await new Promise(resolve => setTimeout(resolve, 3000));
        break;

      case 'completion':
        // Final configuration
        await new Promise(resolve => setTimeout(resolve, 1000));
        break;

      default:
        throw new Error(`Unknown step: ${stepId}`);
    }
  };

  const retryStep = (stepId: string) => {
    setSteps(prev => prev.map(step =>
      step.id === stepId
        ? { ...step, status: 'pending' }
        : step
    ));
  };

  const getStepIcon = (step: SetupStep) => {
    switch (step.status) {
      case 'completed':
        return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'in_progress':
        return <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />;
      case 'error':
        return <div className="w-5 h-5 text-red-500 cursor-pointer" onClick={() => retryStep(step.id)}>
          <Circle className="w-5 h-5" />
        </div>;
      default:
        return <Circle className="w-5 h-5 text-gray-400" />;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-8 py-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold">Express Setup</h1>
              <p className="text-blue-100 mt-1">Get MISA.AI running in just a few clicks</p>
            </div>
            <button
              onClick={onAdvancedSetup}
              className="flex items-center gap-2 px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg transition-colors"
            >
              <Settings className="w-4 h-4" />
              Advanced
            </button>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="px-8 py-4 bg-gray-50">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-600">Setup Progress</span>
            <span className="text-sm font-medium text-gray-900">{Math.round(setupProgress)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-gradient-to-r from-blue-500 to-indigo-500 h-2 rounded-full transition-all duration-500"
              style={{ width: `${setupProgress}%` }}
            />
          </div>
        </div>

        {/* Content */}
        <div className="p-8">
          {/* Device Name Input */}
          <div className="mb-8">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Device Name
            </label>
            <input
              type="text"
              value={deviceName}
              onChange={(e) => setDeviceName(e.target.value)}
              placeholder="Enter a name for this device"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={isSettingUp}
            />
          </div>

          {/* Steps */}
          <div className="space-y-4 mb-8">
            {steps.map((step, index) => (
              <div
                key={step.id}
                className={`flex items-center gap-4 p-4 rounded-lg border ${
                  step.status === 'in_progress'
                    ? 'border-blue-500 bg-blue-50'
                    : step.status === 'completed'
                    ? 'border-green-500 bg-green-50'
                    : step.status === 'error'
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-200'
                }`}
              >
                <div className="flex-shrink-0">
                  {getStepIcon(step)}
                </div>
                <div className="flex-grow">
                  <div className="flex items-center gap-2">
                    <step.icon className="w-5 h-5 text-gray-600" />
                    <h3 className="font-medium text-gray-900">{step.title}</h3>
                    {step.optional && (
                      <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                        Optional
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600 mt-1">{step.description}</p>
                </div>
                {step.status === 'completed' && (
                  <CheckCircle className="w-6 h-6 text-green-500" />
                )}
              </div>
            ))}
          </div>

          {/* Detected Devices */}
          {detectedDevices.length > 0 && (
            <div className="mb-8 p-4 bg-blue-50 rounded-lg">
              <h3 className="font-medium text-gray-900 mb-2">Detected Devices</h3>
              <div className="space-y-2">
                {detectedDevices.map(device => (
                  <div key={device.id} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Wifi className="w-4 h-4 text-blue-500" />
                      <span className="text-sm text-gray-700">{device.name}</span>
                    </div>
                    <span className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                      {device.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-4">
            <button
              onClick={onSkip}
              disabled={isSettingUp}
              className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Skip Setup
            </button>
            <button
              onClick={startExpressSetup}
              disabled={isSettingUp || !deviceName.trim()}
              className="flex-1 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 transition-all transform hover:scale-[1.02] disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none flex items-center justify-center gap-2"
            >
              {isSettingUp ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Setting Up...
                </>
              ) : (
                <>
                  Start Using MISA
                  <ChevronRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>

          {/* Help Text */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              Express Setup configures privacy-friendly defaults.
              <br />
              You can customize all settings later in the application.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};