import React from 'react';
import { SetupData, SetupStep } from './SetupWizard';

interface WelcomeStepProps {
  data: SetupData;
  onUpdate: (data: Partial<SetupData>) => void;
  onNext: () => void;
  onSkip: () => void;
}

export const WelcomeStep: React.FC<WelcomeStepProps> = ({
  data,
  onUpdate,
  onNext,
  onSkip,
}) => {
  const handlePrivacyChange = (key: keyof SetupData['privacy'], value: boolean) => {
    onUpdate({
      privacy: {
        ...data.privacy,
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
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Welcome to MISA.AI
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Your privacy-first intelligent assistant is ready to help you with 18 integrated applications.
          Let's configure your experience in just a few steps.
        </p>
      </div>

      {/* Key Features */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="text-center p-6 bg-blue-50 rounded-lg">
          <div className="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h3 className="font-semibold text-gray-900 mb-2">Privacy-First</h3>
          <p className="text-sm text-gray-600">
            Your data stays private with local processing and end-to-end encryption
          </p>
        </div>

        <div className="text-center p-6 bg-green-50 rounded-lg">
          <div className="w-12 h-12 bg-green-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <h3 className="font-semibold text-gray-900 mb-2">18 Applications</h3>
          <p className="text-sm text-gray-600">
            Complete ecosystem with Calendar, Notes, Tasks, Focus, and more
          </p>
        </div>

        <div className="text-center p-6 bg-purple-50 rounded-lg">
          <div className="w-12 h-12 bg-purple-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h3 className="font-semibold text-gray-900 mb-2">AI-Powered</h3>
          <p className="text-sm text-gray-600">
            Local and cloud AI models with automatic switching
          </p>
        </div>
      </div>

      {/* Privacy Settings */}
      <div className="bg-gray-50 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Privacy Settings (Recommended: Keep all disabled)
        </h3>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="font-medium text-gray-900">Data Collection</h4>
              <p className="text-sm text-gray-600">Help improve MISA.AI with usage data</p>
            </div>
            <button
              onClick={() => handlePrivacyChange('dataCollection', !data.privacy.dataCollection)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.privacy.dataCollection ? 'bg-blue-600' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.privacy.dataCollection ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h4 className="font-medium text-gray-900">Crash Reports</h4>
              <p className="text-sm text-gray-600">Automatically send error reports</p>
            </div>
            <button
              onClick={() => handlePrivacyChange('crashReports', !data.privacy.crashReports)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.privacy.crashReports ? 'bg-blue-600' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.privacy.crashReports ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h4 className="font-medium text-gray-900">Usage Analytics</h4>
              <p className="text-sm text-gray-600">Share anonymous usage statistics</p>
            </div>
            <button
              onClick={() => handlePrivacyChange('analytics', !data.privacy.analytics)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                data.privacy.analytics ? 'bg-blue-600' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  data.privacy.analytics ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        </div>

        <div className="mt-4 p-3 bg-blue-50 rounded-md">
          <p className="text-sm text-blue-800">
            <strong>Privacy Note:</strong> MISA.AI is designed to work perfectly with all privacy settings disabled.
            Your data stays on your device by default.
          </p>
        </div>
      </div>

      {/* Quick Start Options */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Quick Start Options
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            onClick={onNext}
            className="flex items-center justify-center px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
            Guided Setup (Recommended)
          </button>

          <button
            onClick={onSkip}
            className="flex items-center justify-center px-6 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 9l3 3m0 0l-3 3m3-3H8m13 0a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Use Default Settings
          </button>
        </div>

        <p className="text-sm text-gray-600 mt-4">
          <strong>Guided Setup:</strong> Customize your experience with security preferences, device pairing, and app selection.<br/>
          <strong>Default Settings:</strong> Start immediately with privacy-first configuration. You can change settings later.
        </p>
      </div>

      {/* Footer */}
      <div className="text-center text-sm text-gray-500">
        <p>
          This setup will take approximately 2-3 minutes. You can always change these settings later in the application.
        </p>
      </div>
    </div>
  );
};