import React, { useState } from 'react';
import { SetupData } from './SetupWizard';

interface CloudStepProps {
  data: SetupData;
  onUpdate: (data: Partial<SetupData>) => void;
  onNext: () => void;
  onBack: () => void;
}

const cloudProviders = [
  { id: 'misa-cloud', name: 'MISA Cloud Vault', description: 'Official encrypted cloud storage', recommended: true },
  { id: 'google-drive', name: 'Google Drive', description: 'Use your existing Google Drive' },
  { id: 'onedrive', name: 'OneDrive', description: 'Use your existing OneDrive' },
  { id: 'dropbox', name: 'Dropbox', description: 'Use your existing Dropbox' },
];

export const CloudStep: React.FC<CloudStepProps> = ({
  data,
  onUpdate,
  onNext,
  onBack,
}) => {
  const [showAdvanced, setShowAdvanced] = useState(false);

  const handleCloudChange = (key: keyof SetupData['cloud'], value: any) => {
    onUpdate({
      cloud: {
        ...data.cloud,
        [key]: value,
      },
    });
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-blue-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Cloud Services (Optional)
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Enable cloud synchronization for access to your data across devices. All cloud features are optional - MISA.AI works perfectly offline.
        </p>
      </div>

      {/* Cloud Toggle */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Cloud Synchronization
        </h3>

        <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
          <div className="flex-1">
            <h4 className="font-medium text-gray-900">Enable Cloud Sync</h4>
            <p className="text-sm text-gray-600 mt-1">
              Synchronize your data securely across all your devices
            </p>
          </div>
          <button
            onClick={() => handleCloudChange('enableCloud', !data.cloud.enableCloud)}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              data.cloud.enableCloud ? 'bg-blue-600' : 'bg-gray-300'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                data.cloud.enableCloud ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>

        {data.cloud.enableCloud && (
          <div className="mt-4 p-4 bg-blue-50 rounded-md">
            <p className="text-sm text-blue-800">
              <strong>Privacy First:</strong> All data is encrypted on your device before being uploaded to the cloud.
              Even we cannot access your unencrypted data.
            </p>
          </div>
        )}
      </div>

      {/* Cloud Provider Selection */}
      {data.cloud.enableCloud && (
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Choose Cloud Provider
          </h3>

          <div className="space-y-3">
            {cloudProviders.map((provider) => (
              <div
                key={provider.id}
                className={`border rounded-lg p-4 cursor-pointer transition-colors ${
                  data.cloud.provider === provider.id
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
                onClick={() => handleCloudChange('provider', provider.id)}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-2">
                      <h4 className="font-medium text-gray-900">{provider.name}</h4>
                      {provider.recommended && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                          Recommended
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-600 mt-1">{provider.description}</p>
                  </div>
                  <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                    data.cloud.provider === provider.id
                      ? 'border-blue-500 bg-blue-500'
                      : 'border-gray-300'
                  }`}>
                    {data.cloud.provider === provider.id && (
                      <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                      </svg>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-4 p-3 bg-yellow-50 rounded-md">
            <p className="text-sm text-yellow-800">
              <strong>Note:</strong> You'll need to sign in with your chosen cloud provider account during setup.
            </p>
          </div>
        </div>
      )}

      {/* Auto Sync Settings */}
      {data.cloud.enableCloud && (
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Synchronization Settings
          </h3>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
              <div className="flex-1">
                <h4 className="font-medium text-gray-900">Automatic Sync</h4>
                <p className="text-sm text-gray-600 mt-1">
                  Automatically sync changes across all devices
                </p>
              </div>
              <button
                onClick={() => handleCloudChange('autoSync', !data.cloud.autoSync)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  data.cloud.autoSync ? 'bg-blue-600' : 'bg-gray-300'
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    data.cloud.autoSync ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Advanced Settings */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">
            Advanced Settings
          </h3>
          <button
            onClick={() => setShowAdvanced(!showAdvanced)}
            className="text-blue-600 hover:text-blue-700 text-sm"
          >
            {showAdvanced ? 'Hide' : 'Show'}
          </button>
        </div>

        {showAdvanced && (
          <div className="space-y-4 text-sm text-gray-600">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <h4 className="font-medium text-gray-900 mb-2">Encryption</h4>
                <ul className="space-y-1">
                  <li>• End-to-end encryption (AES-256)</li>
                  <li>• Zero-knowledge architecture</li>
                  <li>• Client-side encryption keys</li>
                </ul>
              </div>
              <div>
                <h4 className="font-medium text-gray-900 mb-2">Data Types</h4>
                <ul className="space-y-1">
                  <li>• Notes and documents</li>
                  <li>• Calendar events</li>
                  <li>• Task lists and projects</li>
                  <li>• Application settings</li>
                </ul>
              </div>
            </div>
            <div className="p-3 bg-gray-50 rounded-md">
              <p className="text-gray-700">
                <strong>Security:</strong> Your encryption keys never leave your device.
                Even if cloud servers are compromised, your data remains secure.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Privacy Notice */}
      <div className="bg-green-50 border border-green-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-green-900 mb-4">
          🔒 Your Privacy is Protected
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-green-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <div>
              <p className="text-sm text-green-900">
                <strong>Local-first:</strong> MISA.AI works perfectly without cloud sync
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-green-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <div>
              <p className="text-sm text-green-900">
                <strong>Encrypted:</strong> Your data is encrypted before leaving your device
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-green-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <div>
              <p className="text-sm text-green-900">
                <strong>Optional:</strong> Cloud sync is completely optional
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-green-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <div>
              <p className="text-sm text-green-900">
                <strong>Control:</strong> You can disable cloud sync at any time
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
          className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          Complete Setup
        </button>
      </div>
    </div>
  );
};