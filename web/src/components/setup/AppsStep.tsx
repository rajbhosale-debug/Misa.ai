import React from 'react';
import { SetupData } from './SetupWizard';

interface AppsStepProps {
  data: SetupData;
  onUpdate: (data: Partial<SetupData>) => void;
  onNext: () => void;
  onBack: () => void;
}

const availableApps = [
  { id: 'calendar', name: 'Calendar', description: 'AI scheduling with OCR import', icon: '📅' },
  { id: 'notes', name: 'Notes', description: 'Rich text + voice + handwriting', icon: '📝' },
  { id: 'tasks', name: 'TaskFlow', description: 'AI-powered task management', icon: '✅' },
  { id: 'filehub', name: 'FileHub', description: 'Unified file manager with AI search', icon: '📁' },
  { id: 'focus', name: 'Focus', description: 'Productivity tracking and sessions', icon: '🎯' },
  { id: 'persona', name: 'Persona Studio', description: 'Avatar & voice customization', icon: '🎭' },
  { id: 'webiq', name: 'WebIQ', description: 'Browser assistant for page analysis', icon: '🌐' },
  { id: 'chatsync', name: 'ChatSync', description: 'Multi-platform message integration', icon: '💬' },
  { id: 'meet', name: 'Meet', description: 'Meeting recording & transcription', icon: '📹' },
  { id: 'home', name: 'Home', description: 'IoT device control and automation', icon: '🏠' },
  { id: 'powersense', name: 'PowerSense', description: 'System monitoring & compute routing', icon: '⚡' },
  { id: 'worksuite', name: 'WorkSuite', description: 'Professional productivity tools', icon: '💼' },
  { id: 'devhub', name: 'DevHub', description: 'IDE integrations & development tools', icon: '👨‍💻' },
  { id: 'store', name: 'Store', description: 'Plugin marketplace', icon: '🛍️' },
  { id: 'vault', name: 'Vault', description: 'Secure password management', icon: '🔐' },
  { id: 'biolink', name: 'BioLink', description: 'Wearable data integration', icon: '⌚' },
  { id: 'workflow', name: 'Workflow AI', description: 'Visual automation builder', icon: '🔄' },
  { id: 'ambient', name: 'Ambient Mode', description: 'Contextual background assistance', icon: '🌙' },
];

const availableModels = [
  { id: 'mixtral', name: 'Mixtral 8x7B', description: 'General purpose, balanced performance', size: '26GB' },
  { id: 'codellama', name: 'CodeLlama 34B', description: 'Specialized for coding tasks', size: '64GB' },
  { id: 'wizardcoder', name: 'WizardCoder 15B', description: 'Code generation and debugging', size: '30GB' },
  { id: 'dolphin-mistral', name: 'Dolphin Mistral 7B', description: 'Lightweight, fast responses', size: '14GB' },
];

export const AppsStep: React.FC<AppsStepProps> = ({
  data,
  onUpdate,
  onNext,
  onBack,
}) => {
  const handleAppsChange = (appId: string, enabled: boolean) => {
    const newApps = enabled
      ? [...data.apps.enabledApps, appId]
      : data.apps.enabledApps.filter(id => id !== appId);

    onUpdate({
      apps: {
        ...data.apps,
        enabledApps: newApps,
      },
    });
  };

  const handleModelChange = (modelId: string) => {
    onUpdate({
      apps: {
        ...data.apps,
        defaultModel: modelId,
      },
    });
  };

  const handleVoiceChange = (enabled: boolean) => {
    onUpdate({
      apps: {
        ...data.apps,
        voiceEnabled: enabled,
      },
    });
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <div className="mx-auto w-20 h-20 bg-orange-600 rounded-full flex items-center justify-center mb-6">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Applications & AI Models
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Choose which applications to enable and select your preferred AI model. You can change these settings later.
        </p>
      </div>

      {/* AI Model Selection */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Default AI Model
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {availableModels.map((model) => (
            <div
              key={model.id}
              className={`border rounded-lg p-4 cursor-pointer transition-colors ${
                data.apps.defaultModel === model.id
                  ? 'border-orange-500 bg-orange-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
              onClick={() => handleModelChange(model.id)}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h4 className="font-medium text-gray-900">{model.name}</h4>
                  <p className="text-sm text-gray-600 mt-1">{model.description}</p>
                  <p className="text-xs text-gray-500 mt-2">Size: {model.size}</p>
                </div>
                <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                  data.apps.defaultModel === model.id
                    ? 'border-orange-500 bg-orange-500'
                    : 'border-gray-300'
                }`}>
                  {data.apps.defaultModel === model.id && (
                    <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-4 p-3 bg-blue-50 rounded-md">
          <p className="text-sm text-blue-800">
            <strong>Recommendation:</strong> Start with Mixtral for balanced performance. Models will be downloaded on first use.
          </p>
        </div>
      </div>

      {/* Voice Settings */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Voice Interaction
        </h3>

        <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
          <div className="flex-1">
            <h4 className="font-medium text-gray-900">Enable Voice Assistant</h4>
            <p className="text-sm text-gray-600 mt-1">
              Use voice commands and wake word activation ("Hey Misa")
            </p>
          </div>
          <button
            onClick={() => handleVoiceChange(!data.apps.voiceEnabled)}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              data.apps.voiceEnabled ? 'bg-orange-600' : 'bg-gray-300'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                data.apps.voiceEnabled ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>
      </div>

      {/* Application Selection */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Enable Applications
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {availableApps.map((app) => (
            <div
              key={app.id}
              className={`border rounded-lg p-4 transition-colors ${
                data.apps.enabledApps.includes(app.id)
                  ? 'border-orange-500 bg-orange-50'
                  : 'border-gray-200'
              }`}
            >
              <div className="flex items-start space-x-3">
                <button
                  onClick={() => handleAppsChange(app.id, !data.apps.enabledApps.includes(app.id))}
                  className={`mt-1 w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                    data.apps.enabledApps.includes(app.id)
                      ? 'border-orange-500 bg-orange-500'
                      : 'border-gray-300 hover:border-gray-400'
                  }`}
                >
                  {data.apps.enabledApps.includes(app.id) && (
                    <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  )}
                </button>
                <div className="flex-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-2xl">{app.icon}</span>
                    <h4 className="font-medium text-gray-900">{app.name}</h4>
                  </div>
                  <p className="text-sm text-gray-600 mt-1">{app.description}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-4 flex items-center justify-between">
          <span className="text-sm text-gray-600">
            {data.apps.enabledApps.length} of {availableApps.length} applications enabled
          </span>
          <div className="space-x-2">
            <button
              onClick={() => onUpdate({ apps: { ...data.apps, enabledApps: [] } })}
              className="px-3 py-1 text-sm bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition-colors"
            >
              Deselect All
            </button>
            <button
              onClick={() => onUpdate({ apps: { ...data.apps, enabledApps: availableApps.map(a => a.id) } })}
              className="px-3 py-1 text-sm bg-orange-600 text-white rounded hover:bg-orange-700 transition-colors"
            >
              Select All
            </button>
          </div>
        </div>
      </div>

      {/* Resource Requirements */}
      <div className="bg-orange-50 border border-orange-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-orange-900 mb-4">
          📊 Resource Requirements
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h4 className="font-medium text-orange-900 mb-2">Minimum Requirements</h4>
            <ul className="text-sm text-orange-800 space-y-1">
              <li>• RAM: 8GB (16GB recommended)</li>
              <li>• Storage: 20GB free space</li>
              <li>• CPU: 4+ cores recommended</li>
              <li>• Network: Broadband connection</li>
            </ul>
          </div>
          <div>
            <h4 className="font-medium text-orange-900 mb-2">Performance Tips</h4>
            <ul className="text-sm text-orange-800 space-y-1">
              <li>• Enable fewer apps for better performance</li>
              <li>• Choose smaller models for limited RAM</li>
              <li>• SSD storage improves model loading</li>
              <li>• GPU acceleration supported (NVIDIA/Apple)</li>
            </ul>
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
          className="px-6 py-3 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition-colors"
        >
          Continue
        </button>
      </div>
    </div>
  );
};