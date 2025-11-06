import React, { useState } from 'react';
import { SetupData } from './SetupWizard';

interface SecurityStepProps {
  data: SetupData;
  onUpdate: (data: Partial<SetupData>) => void;
  onNext: () => void;
  onBack: () => void;
}

export const SecurityStep: React.FC<SecurityStepProps> = ({
  data,
  onUpdate,
  onNext,
  onBack,
}) => {
  const [biometricStatus, setBiometricStatus] = useState<'checking' | 'available' | 'unavailable'>('checking');

  // Check biometric availability on mount
  React.useEffect(() => {
    const checkBiometric = async () => {
      try {
        // Simulate biometric availability check
        await new Promise(resolve => setTimeout(resolve, 1000));

        // In a real app, you'd check Web Authentication API
        const isAvailable = 'credentials' in navigator;
        setBiometricStatus(isAvailable ? 'available' : 'unavailable');
      } catch (error) {
        setBiometricStatus('unavailable');
      }
    };

    checkBiometric();
  }, []);

  const handleSecurityChange = (key: keyof SetupData['security'], value: any) => {
    onUpdate({
      security: {
        ...data.security,
        [key]: value,
      },
    });
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-green-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Security & Privacy
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Configure your security preferences. MISA.AI prioritizes your privacy with local-first processing.
        </p>
      </div>

      {/* Authentication Settings */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Authentication
        </h3>

        <div className="space-y-6">
          {/* Biometric Authentication */}
          <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="flex-1">
              <h4 className="font-medium text-gray-900 flex items-center">
                Biometric Authentication
                {biometricStatus === 'checking' && (
                  <span className="ml-2 text-sm text-yellow-600">(checking...)</span>
                )}
                {biometricStatus === 'available' && (
                  <span className="ml-2 text-sm text-green-600">(available)</span>
                )}
                {biometricStatus === 'unavailable' && (
                  <span className="ml-2 text-sm text-gray-500">(not available)</span>
                )}
              </h4>
              <p className="text-sm text-gray-600 mt-1">
                Use fingerprint, face recognition, or Windows Hello for secure access
              </p>
            </div>
            <button
              onClick={() => handleSecurityChange('biometric', !data.security.biometric)}
              disabled={biometricStatus !== 'available'}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.security.biometric && biometricStatus === 'available' ? 'bg-green-600' : 'bg-gray-300'
              } ${biometricStatus !== 'available' ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.security.biometric && biometricStatus === 'available' ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {/* Two-Factor Authentication */}
          <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="flex-1">
              <h4 className="font-medium text-gray-900">Two-Factor Authentication</h4>
              <p className="text-sm text-gray-600 mt-1">
                Add an extra layer of security with 2FA (optional)
              </p>
            </div>
            <button
              onClick={() => handleSecurityChange('twoFactor', !data.security.twoFactor)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.security.twoFactor ? 'bg-green-600' : 'bg-gray-300'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.security.twoFactor ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {/* Session Timeout */}
          <div className="p-4 bg-gray-50 rounded-lg">
            <h4 className="font-medium text-gray-900 mb-3">Session Timeout</h4>
            <p className="text-sm text-gray-600 mb-4">
              Automatically lock the session after inactivity
            </p>
            <div className="grid grid-cols-3 gap-3">
              {[15, 30, 60, 120, 240, 480].map((minutes) => (
                <button
                  key={minutes}
                  onClick={() => handleSecurityChange('sessionTimeout', minutes)}
                  className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    data.security.sessionTimeout === minutes
                      ? 'bg-blue-600 text-white'
                      : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {minutes < 60 ? `${minutes} min` : `${minutes / 60} hour`}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Data Protection */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Data Protection
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-center space-x-3 p-3 bg-green-50 rounded-lg">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            <div>
              <h4 className="font-medium text-gray-900">End-to-End Encryption</h4>
              <p className="text-sm text-gray-600">Always enabled</p>
            </div>
          </div>

          <div className="flex items-center space-x-3 p-3 bg-green-50 rounded-lg">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <div>
              <h4 className="font-medium text-gray-900">Local Processing</h4>
              <p className="text-sm text-gray-600">Default setting</p>
            </div>
          </div>

          <div className="flex items-center space-x-3 p-3 bg-green-50 rounded-lg">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <div>
              <h4 className="font-medium text-gray-900">Audit Logging</h4>
              <p className="text-sm text-gray-600">Security events tracked</p>
            </div>
          </div>

          <div className="flex items-center space-x-3 p-3 bg-green-50 rounded-lg">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
            <div>
              <h4 className="font-medium text-gray-900">Plugin Sandboxing</h4>
              <p className="text-sm text-gray-600">Isolated execution</p>
            </div>
          </div>
        </div>
      </div>

      {/* Security Recommendations */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-blue-900 mb-4">
          🔒 Security Recommendations
        </h3>

        <div className="space-y-3">
          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-blue-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p className="text-sm text-blue-900">
                <strong>Enable biometric authentication</strong> for convenient and secure access
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-blue-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p className="text-sm text-blue-900">
                <strong>Keep session timeout moderate</strong> (30-60 minutes) for balance of security and convenience
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-blue-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p className="text-sm text-blue-900">
                <strong>Consider 2FA</strong> if you're handling sensitive information
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
          Continue
        </button>
      </div>
    </div>
  );
};