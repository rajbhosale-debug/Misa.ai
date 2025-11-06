import React, { useState, useEffect } from 'react';
import { SetupData } from './SetupWizard';

interface DeviceStepProps {
  data: SetupData;
  onUpdate: (data: Partial<SetupData>) => void;
  onNext: () => void;
  onBack: () => void;
}

export const DeviceStep: React.FC<DeviceStepProps> = ({
  data,
  onUpdate,
  onNext,
  onBack,
}) => {
  const [deviceName, setDeviceName] = useState('');
  const [isScanning, setIsScanning] = useState(false);
  const [foundDevices, setFoundDevices] = useState<Array<{id: string, name: string, type: string}>>([]);

  useEffect(() => {
    // Set default device name
    const defaultName = `${navigator.userAgent.includes('Mac') ? 'Mac' :
                        navigator.userAgent.includes('Windows') ? 'Windows' :
                        navigator.userAgent.includes('Linux') ? 'Linux' : 'Computer'}-${Date.now().toString(36).slice(-4)}`;
    setDeviceName(defaultName);
    handleDeviceChange('deviceName', defaultName);
  }, []);

  const handleDeviceChange = (key: keyof SetupData['device'], value: any) => {
    onUpdate({
      device: {
        ...data.device,
        [key]: value,
      },
    });
  };

  const scanForDevices = async () => {
    setIsScanning(true);
    try {
      // Simulate device discovery
      await new Promise(resolve => setTimeout(resolve, 3000));

      // Mock devices for demonstration
      const mockDevices = [
        { id: 'phone-123', name: 'iPhone 14 Pro', type: 'mobile' },
        { id: 'tablet-456', name: 'iPad Air', type: 'tablet' },
        { id: 'laptop-789', name: 'MacBook Pro', type: 'desktop' },
      ];

      setFoundDevices(mockDevices);
    } catch (error) {
      console.error('Device scan failed:', error);
    } finally {
      setIsScanning(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-purple-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Device Setup
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Configure your device and enable multi-device synchronization for seamless AI assistance across all your devices.
        </p>
      </div>

      {/* Device Name */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Device Information
        </h3>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Device Name
            </label>
            <input
              type="text"
              value={deviceName}
              onChange={(e) => {
                setDeviceName(e.target.value);
                handleDeviceChange('deviceName', e.target.value);
              }}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="My Computer"
            />
            <p className="text-sm text-gray-500 mt-1">
              This name will be used to identify this device in your MISA.AI network
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded-lg">
            <div>
              <p className="text-sm font-medium text-gray-700">Platform</p>
              <p className="text-sm text-gray-900">
                {navigator.userAgent.includes('Mac') ? 'macOS' :
                 navigator.userAgent.includes('Windows') ? 'Windows' :
                 navigator.userAgent.includes('Linux') ? 'Linux' : 'Unknown'}
              </p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-700">Browser</p>
              <p className="text-sm text-gray-900">
                {navigator.userAgent.includes('Chrome') ? 'Chrome' :
                 navigator.userAgent.includes('Firefox') ? 'Firefox' :
                 navigator.userAgent.includes('Safari') ? 'Safari' :
                 navigator.userAgent.includes('Edge') ? 'Edge' : 'Unknown'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Multi-Device Sync */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Multi-Device Features
        </h3>

        <div className="space-y-6">
          <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="flex-1">
              <h4 className="font-medium text-gray-900">Enable Sync</h4>
              <p className="text-sm text-gray-600 mt-1">
                Synchronize your data across all your devices
              </p>
            </div>
            <button
              onClick={() => handleDeviceChange('enableSync', !data.device.enableSync)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.device.enableSync ? 'bg-purple-600' : 'bg-gray-300'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.device.enableSync ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="flex-1">
              <h4 className="font-medium text-gray-900">Remote Desktop</h4>
              <p className="text-sm text-gray-600 mt-1">
                Control this device remotely from other paired devices
              </p>
            </div>
            <button
              onClick={() => handleDeviceChange('remoteDesktop', !data.device.remoteDesktop)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.device.remoteDesktop ? 'bg-purple-600' : 'bg-gray-300'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.device.remoteDesktop ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* Device Discovery */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">
            Discover Other Devices
          </h3>
          <button
            onClick={scanForDevices}
            disabled={isScanning}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              isScanning
                ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                : 'bg-purple-600 text-white hover:bg-purple-700'
            }`}
          >
            {isScanning ? (
              <span className="flex items-center">
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Scanning...
              </span>
            ) : (
              'Scan for Devices'
            )}
          </button>
        </div>

        {foundDevices.length > 0 && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">Found {foundDevices.length} device(s) on your network:</p>
            {foundDevices.map((device) => (
              <div key={device.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
                    {device.type === 'mobile' && (
                      <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                      </svg>
                    )}
                    {device.type === 'tablet' && (
                      <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                      </svg>
                    )}
                    {device.type === 'desktop' && (
                      <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    )}
                  </div>
                  <div>
                    <h4 className="font-medium text-gray-900">{device.name}</h4>
                    <p className="text-sm text-gray-500 capitalize">{device.type}</p>
                  </div>
                </div>
                <button className="px-3 py-1 bg-purple-600 text-white text-sm rounded-lg hover:bg-purple-700 transition-colors">
                  Pair Device
                </button>
              </div>
            ))}
          </div>
        )}

        {foundDevices.length === 0 && !isScanning && (
          <div className="text-center py-8">
            <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="mt-2 text-sm text-gray-500">
              No devices found. Make sure other devices are on the same network and have MISA.AI running.
            </p>
          </div>
        )}
      </div>

      {/* Feature Preview */}
      <div className="bg-purple-50 border border-purple-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-purple-900 mb-4">
          🌐 Multi-Device Benefits
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-purple-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
            <div>
              <p className="text-sm text-purple-900">
                <strong>Seamless handoff</strong> - Start tasks on one device, continue on another
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-purple-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            <div>
              <p className="text-sm text-purple-900">
                <strong>Secure sync</strong> - End-to-end encrypted data synchronization
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-purple-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <div>
              <p className="text-sm text-purple-900">
                <strong>Remote control</strong> - Access and control other devices securely
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-purple-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            <div>
              <p className="text-sm text-purple-900">
                <strong>Smart routing</strong> - AI tasks automatically routed to best device
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex justify-between">
        <button
          onClick={onBack}
          className="px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
        >
          Back
        </button>
        <button
          onClick={onNext}
          className="px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
        >
          Continue
        </button>
      </div>
    </div>
  );
};