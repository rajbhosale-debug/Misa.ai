import React from 'react';
import { SetupData } from './SetupWizard';

interface CompleteStepProps {
  data: SetupData;
  onComplete: () => void;
  onBack: () => void;
}

export const CompleteStep: React.FC<CompleteStepProps> = ({
  data,
  onComplete,
  onBack,
}) => {
  const enabledAppsCount = data.apps.enabledApps.length;
  const securityEnabled = data.security.biometric || data.security.twoFactor;
  const cloudEnabled = data.cloud.enableCloud;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-green-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Setup Complete!
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Your MISA.AI intelligent assistant is ready to help you with all your tasks. Here's a summary of your configuration:
        </p>
      </div>

      {/* Configuration Summary */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Configuration Summary
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Privacy Settings */}
          <div className="space-y-3">
            <h4 className="font-medium text-gray-900 flex items-center">
              <svg className="w-5 h-5 text-green-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              Privacy Settings
            </h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Data Collection:</span>
                <span className={data.privacy.dataCollection ? 'text-red-600' : 'text-green-600'}>
                  {data.privacy.dataCollection ? 'Enabled' : 'Disabled'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Crash Reports:</span>
                <span className={data.privacy.crashReports ? 'text-orange-600' : 'text-green-600'}>
                  {data.privacy.crashReports ? 'Enabled' : 'Disabled'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Usage Analytics:</span>
                <span className={data.privacy.analytics ? 'text-orange-600' : 'text-green-600'}>
                  {data.privacy.analytics ? 'Enabled' : 'Disabled'}
                </span>
              </div>
            </div>
          </div>

          {/* Security Settings */}
          <div className="space-y-3">
            <h4 className="font-medium text-gray-900 flex items-center">
              <svg className="w-5 h-5 text-green-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              Security
            </h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Biometric Auth:</span>
                <span className={data.security.biometric ? 'text-green-600' : 'text-gray-600'}>
                  {data.security.biometric ? 'Enabled' : 'Disabled'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Two-Factor Auth:</span>
                <span className={data.security.twoFactor ? 'text-green-600' : 'text-gray-600'}>
                  {data.security.twoFactor ? 'Enabled' : 'Disabled'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Session Timeout:</span>
                <span className="text-gray-900">
                  {data.security.sessionTimeout < 60 ? `${data.security.sessionTimeout} min` : `${data.security.sessionTimeout / 60} hour`}
                </span>
              </div>
            </div>
          </div>

          {/* Applications */}
          <div className="space-y-3">
            <h4 className="font-medium text-gray-900 flex items-center">
              <svg className="w-5 h-5 text-green-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              Applications
            </h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Enabled Apps:</span>
                <span className="text-gray-900">{enabledAppsCount} of 18</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Default Model:</span>
                <span className="text-gray-900 capitalize">{data.apps.defaultModel}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Voice Assistant:</span>
                <span className={data.apps.voiceEnabled ? 'text-green-600' : 'text-gray-600'}>
                  {data.apps.voiceEnabled ? 'Enabled' : 'Disabled'}
                </span>
              </div>
            </div>
          </div>

          {/* Cloud & Sync */}
          <div className="space-y-3">
            <h4 className="font-medium text-gray-900 flex items-center">
              <svg className="w-5 h-5 text-green-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
              </svg>
              Cloud & Sync
            </h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Cloud Sync:</span>
                <span className={cloudEnabled ? 'text-green-600' : 'text-gray-600'}>
                  {cloudEnabled ? 'Enabled' : 'Disabled'}
                </span>
              </div>
              {cloudEnabled && (
                <>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Provider:</span>
                    <span className="text-gray-900">
                      {data.cloud.provider === 'misa-cloud' ? 'MISA Cloud Vault' :
                       data.cloud.provider === 'google-drive' ? 'Google Drive' :
                       data.cloud.provider === 'onedrive' ? 'OneDrive' :
                       data.cloud.provider === 'dropbox' ? 'Dropbox' : 'None'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Auto Sync:</span>
                    <span className={data.cloud.autoSync ? 'text-green-600' : 'text-gray-600'}>
                      {data.cloud.autoSync ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* What's Next */}
      <div className="bg-green-50 border border-green-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-green-900 mb-4">
          🚀 What's Next?
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0 w-8 h-8 bg-green-600 rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-bold">1</span>
            </div>
            <div>
              <h4 className="font-medium text-green-900">Explore Applications</h4>
              <p className="text-sm text-green-800">
                Discover all 18 applications and their AI-powered features
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0 w-8 h-8 bg-green-600 rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-bold">2</span>
            </div>
            <div>
              <h4 className="font-medium text-green-900">Try Voice Commands</h4>
              <p className="text-sm text-green-800">
                Say "Hey Misa" to wake up your AI assistant
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0 w-8 h-8 bg-green-600 rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-bold">3</span>
            </div>
            <div>
              <h4 className="font-medium text-green-900">Customize Your Experience</h4>
              <p className="text-sm text-green-800">
                Adjust settings, themes, and AI model preferences
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0 w-8 h-8 bg-green-600 rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-bold">4</span>
            </div>
            <div>
              <h4 className="font-medium text-green-900">Install Mobile Apps</h4>
              <p className="text-sm text-green-800">
                Get MISA.AI on your phone for on-the-go assistance
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Getting Started Tips */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-blue-900 mb-4">
          💡 Quick Tips
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div className="space-y-2">
            <h4 className="font-medium text-blue-900">AI Commands</h4>
            <ul className="text-blue-800 space-y-1">
              <li>• "Schedule a meeting for tomorrow"</li>
              <li>• "Summarize my recent notes"</li>
              <li>• "Create a task list for the project"</li>
            </ul>
          </div>
          <div className="space-y-2">
            <h4 className="font-medium text-blue-900">Privacy</h4>
            <ul className="text-blue-800 space-y-1">
              <li>• All data stays private by default</li>
              <li>• Change privacy settings anytime</li>
              <li>• Export or delete your data anytime</li>
            </ul>
          </div>
          <div className="space-y-2">
            <h4 className="font-medium text-blue-900">Support</h4>
            <ul className="text-blue-800 space-y-1">
              <li>• Built-in help and tutorials</li>
              <li>• Community Discord server</li>
              <li>• Email support@misa.ai</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Success Message */}
      <div className="text-center p-6 bg-gradient-to-r from-green-50 to-blue-50 rounded-lg border border-green-200">
        <h3 className="text-xl font-bold text-gray-900 mb-2">
          Welcome to MISA.AI! 🎉
        </h3>
        <p className="text-gray-600 mb-4">
          Your privacy-first intelligent assistant is ready to help you achieve more, while keeping your data secure.
        </p>
        <p className="text-sm text-gray-500">
          Setup completed in approximately 2-3 minutes with privacy-first configuration.
        </p>
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
          onClick={onComplete}
          className="px-8 py-3 bg-gradient-to-r from-green-600 to-blue-600 text-white rounded-lg hover:from-green-700 hover:to-blue-700 transition-all transform hover:scale-105 font-semibold"
        >
          Start Using MISA.AI
        </button>
      </div>
    </div>
  );
};