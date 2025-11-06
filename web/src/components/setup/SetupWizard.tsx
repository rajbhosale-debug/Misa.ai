import React, { useState, useEffect } from 'react';
import { WelcomeStep } from './WelcomeStep';
import { SecurityStep } from './SecurityStep';
import { DeviceStep } from './DeviceStep';
import { AppsStep } from './AppsStep';
import { CloudStep } from './CloudStep';
import { CompleteStep } from './CompleteStep';

interface SetupWizardProps {
  onComplete: () => void;
  onSkip: () => void;
}

export type SetupStep = 'welcome' | 'security' | 'device' | 'apps' | 'cloud' | 'complete';

export interface SetupData {
  privacy: {
    dataCollection: boolean;
    crashReports: boolean;
    analytics: boolean;
  };
  security: {
    biometric: boolean;
    twoFactor: boolean;
    sessionTimeout: number;
  };
  device: {
    deviceName: string;
    enableSync: boolean;
    remoteDesktop: boolean;
  };
  apps: {
    enabledApps: string[];
    defaultModel: string;
    voiceEnabled: boolean;
  };
  cloud: {
    enableCloud: boolean;
    autoSync: boolean;
    provider: string;
  };
}

export const SetupWizard: React.FC<SetupWizardProps> = ({ onComplete, onSkip }) => {
  const [currentStep, setCurrentStep] = useState<SetupStep>('welcome');
  const [setupData, setSetupData] = useState<SetupData>({
    privacy: {
      dataCollection: false,
      crashReports: false,
      analytics: false,
    },
    security: {
      biometric: true,
      twoFactor: false,
      sessionTimeout: 30,
    },
    device: {
      deviceName: '',
      enableSync: false,
      remoteDesktop: false,
    },
    apps: {
      enabledApps: ['calendar', 'notes', 'tasks'],
      defaultModel: 'mixtral',
      voiceEnabled: true,
    },
    cloud: {
      enableCloud: false,
      autoSync: false,
      provider: '',
    },
  });

  const steps: SetupStep[] = ['welcome', 'security', 'device', 'apps', 'cloud', 'complete'];
  const currentStepIndex = steps.indexOf(currentStep);

  const updateSetupData = (step: SetupStep, data: Partial<SetupData>) => {
    setSetupData(prev => ({ ...prev, ...data }));
  };

  const nextStep = () => {
    if (currentStepIndex < steps.length - 1) {
      setCurrentStep(steps[currentStepIndex + 1]);
    }
  };

  const prevStep = () => {
    if (currentStepIndex > 0) {
      setCurrentStep(steps[currentStepIndex - 1]);
    }
  };

  const goToStep = (step: SetupStep) => {
    setCurrentStep(step);
  };

  const renderStep = () => {
    switch (currentStep) {
      case 'welcome':
        return (
          <WelcomeStep
            data={setupData}
            onUpdate={(data) => updateSetupData('welcome', data)}
            onNext={nextStep}
            onSkip={onSkip}
          />
        );
      case 'security':
        return (
          <SecurityStep
            data={setupData}
            onUpdate={(data) => updateSetupData('security', data)}
            onNext={nextStep}
            onBack={prevStep}
          />
        );
      case 'device':
        return (
          <DeviceStep
            data={setupData}
            onUpdate={(data) => updateSetupData('device', data)}
            onNext={nextStep}
            onBack={prevStep}
          />
        );
      case 'apps':
        return (
          <AppsStep
            data={setupData}
            onUpdate={(data) => updateSetupData('apps', data)}
            onNext={nextStep}
            onBack={prevStep}
          />
        );
      case 'cloud':
        return (
          <CloudStep
            data={setupData}
            onUpdate={(data) => updateSetupData('cloud', data)}
            onNext={nextStep}
            onBack={prevStep}
          />
        );
      case 'complete':
        return (
          <CompleteStep
            data={setupData}
            onComplete={onComplete}
            onBack={prevStep}
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden">
        {/* Progress Bar */}
        <div className="bg-gray-50 px-8 py-6 border-b border-gray-200">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-2xl font-bold text-gray-900">MISA.AI Setup</h1>
            <button
              onClick={onSkip}
              className="text-gray-500 hover:text-gray-700 text-sm underline"
            >
              Skip setup
            </button>
          </div>
          <div className="flex items-center space-x-2">
            {steps.map((step, index) => (
              <React.Fragment key={step}>
                <div
                  className={`flex-1 h-2 rounded-full transition-colors ${
                    index <= currentStepIndex
                      ? 'bg-blue-600'
                      : 'bg-gray-300'
                  }`}
                />
                {index < steps.length - 1 && (
                  <div className="w-2 h-2 rounded-full bg-gray-300" />
                )}
              </React.Fragment>
            ))}
          </div>
          <div className="flex justify-between mt-2 text-xs text-gray-600">
            <span>Step {currentStepIndex + 1} of {steps.length}</span>
            <span className="capitalize">{currentStep}</span>
          </div>
        </div>

        {/* Step Navigation */}
        <div className="flex bg-gray-50 border-b border-gray-200">
          {steps.map((step, index) => (
            <button
              key={step}
              onClick={() => goToStep(step)}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                currentStep === step
                  ? 'text-blue-600 border-b-2 border-blue-600 bg-white'
                  : index < currentStepIndex
                  ? 'text-green-600 hover:text-green-700'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              <span className="capitalize">{step}</span>
              {index < currentStepIndex && (
                <span className="ml-1">✓</span>
              )}
            </button>
          ))}
        </div>

        {/* Step Content */}
        <div className="p-8 overflow-y-auto max-h-[60vh]">
          {renderStep()}
        </div>
      </div>
    </div>
  );
};