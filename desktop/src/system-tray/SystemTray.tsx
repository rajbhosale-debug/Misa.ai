import React, { useState, useEffect, useRef } from 'react';
import {
  Home,
  Settings,
  X,
  Mic,
  MicOff,
  Volume2,
  VolumeX,
  Wifi,
  WifiOff,
  Smartphone,
  Monitor,
  Bell,
  BellOff,
  RefreshCw,
  Power,
  MessageSquare,
  Camera,
  CameraOff
} from 'lucide-react';

interface SystemTrayProps {
  onShowMain: () => void;
  onExit: () => void;
  onQuickAction: (action: string) => void;
}

interface Device {
  id: string;
  name: string;
  type: 'desktop' | 'laptop' | 'phone' | 'tablet';
  status: 'online' | 'offline' | 'busy';
  batteryLevel?: number;
  lastSeen: Date;
}

interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  timestamp: Date;
  read: boolean;
}

export const SystemTray: React.FC<SystemTrayProps> = ({
  onShowMain,
  onExit,
  onQuickAction
}) => {
  const [isVisible, setIsVisible] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [isMicOn, setIsMicOn] = useState(false);
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [connectedDevices, setConnectedDevices] = useState<Device[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [status, setStatus] = useState<'online' | 'offline' | 'busy'>('online');
  const trayRef = useRef<HTMLDivElement>(null);

  // Initialize system tray on mount
  useEffect(() => {
    initializeSystemTray();
    startBackgroundMonitoring();

    return () => {
      cleanup();
    };
  }, []);

  const initializeSystemTray = () => {
    // In a real Electron/Tauri application, this would:
    // - Create the actual system tray icon
    // - Set up tray menu
    // - Configure auto-start
    // - Initialize system tray notifications

    console.log('Initializing system tray...');

    // Simulate device discovery
    setTimeout(() => {
      const mockDevices: Device[] = [
        {
          id: 'phone-1',
          name: 'iPhone 14',
          type: 'phone',
          status: 'online',
          batteryLevel: 85,
          lastSeen: new Date()
        },
        {
          id: 'laptop-1',
          name: 'Work Laptop',
          type: 'laptop',
          status: 'online',
          batteryLevel: 92,
          lastSeen: new Date()
        }
      ];
      setConnectedDevices(mockDevices);
    }, 2000);
  };

  const startBackgroundMonitoring = () => {
    // Monitor device connections, notifications, etc.
    const interval = setInterval(() => {
      updateDeviceStatus();
      checkForNotifications();
    }, 5000); // Every 5 seconds

    return () => clearInterval(interval);
  };

  const updateDeviceStatus = () => {
    setConnectedDevices(prev => prev.map(device => ({
      ...device,
      batteryLevel: device.batteryLevel ? Math.max(0, device.batteryLevel - 1) : undefined,
      lastSeen: new Date()
    })));
  };

  const checkForNotifications = () => {
    // Simulate receiving notifications
    if (Math.random() > 0.8 && notificationsEnabled) {
      const newNotification: Notification = {
        id: `notif-${Date.now()}`,
        title: 'New Device Connected',
        message: `${connectedDevices[0]?.name || 'Unknown device'} is now online`,
        type: 'success',
        timestamp: new Date(),
        read: false
      };

      setNotifications(prev => [newNotification, ...prev].slice(0, 10));
      showTrayNotification(newNotification);
    }
  };

  const showTrayNotification = (notification: Notification) => {
    // In a real implementation, this would show a system notification
    console.log('System tray notification:', notification);

    // Show browser notification as fallback
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/favicon.ico'
      });
    }
  };

  const cleanup = () => {
    // Cleanup system tray resources
    console.log('Cleaning up system tray...');
  };

  const handleQuickAction = (action: string) => {
    onQuickAction(action);

    switch (action) {
      case 'toggle-mic':
        setIsMicOn(!isMuted);
        break;
      case 'toggle-mute':
        setIsMuted(!isMuted);
        break;
      case 'toggle-notifications':
        setNotificationsEnabled(!notificationsEnabled);
        break;
      case 'toggle-minimize':
        setIsMinimized(!isMinimized);
        break;
      case 'set-status':
        setStatus(prev => {
          const statuses: Array<'online' | 'offline' | 'busy'> = ['online', 'offline', 'busy'];
          const currentIndex = statuses.indexOf(prev);
          return statuses[(currentIndex + 1) % statuses.length];
        });
        break;
    }
  };

  const getDeviceIcon = (type: string) => {
    switch (type) {
      case 'phone':
        return <Smartphone className="w-4 h-4" />;
      case 'tablet':
        return <Monitor className="w-4 h-4" />;
      default:
        return <Monitor className="w-4 h-4" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'online':
        return 'text-green-500';
      case 'offline':
        return 'text-gray-400';
      case 'busy':
        return 'text-yellow-500';
      default:
        return 'text-gray-400';
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'success':
        return '🟢';
      case 'warning':
        return '🟡';
      case 'error':
        return '🔴';
      default:
        return '🔵';
    }
  };

  const unreadCount = notifications.filter(n => !n.read).length;

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (trayRef.current && !trayRef.current.contains(event.target as Node)) {
        setIsVisible(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <>
      {/* System Tray Icon (Floating for demo) */}
      <div className="fixed bottom-4 right-4 z-50">
        <button
          onClick={() => setIsVisible(!isVisible)}
          className="bg-blue-600 text-white p-3 rounded-full shadow-lg hover:bg-blue-700 transition-colors relative"
        >
          <div className="w-6 h-6 flex items-center justify-center">
            {/* MISA Logo Placeholder */}
            <span className="text-sm font-bold">M</span>
          </div>

          {/* Status Indicator */}
          <div className={`absolute top-0 right-0 w-3 h-3 rounded-full ${getStatusColor(status)} border-2 border-white`} />

          {/* Unread Notifications Badge */}
          {unreadCount > 0 && (
            <div className="absolute -top-1 -right-1 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center">
              {unreadCount > 9 ? '9+' : unreadCount}
            </div>
          )}
        </button>
      </div>

      {/* System Tray Menu */}
      {isVisible && (
        <div
          ref={trayRef}
          className="fixed bottom-20 right-4 w-80 bg-white rounded-lg shadow-2xl border border-gray-200 overflow-hidden"
          style={{ maxHeight: '70vh', overflowY: 'auto' }}
        >
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-4 text-white">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-semibold">MISA.AI</h2>
                <p className="text-sm text-blue-100 capitalize">{status}</p>
              </div>
              <button
                onClick={() => setIsVisible(false)}
                className="text-white/80 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="p-4 border-b border-gray-200">
            <div className="grid grid-cols-3 gap-2">
              <button
                onClick={() => onShowMain()}
                className="flex flex-col items-center gap-1 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <Home className="w-5 h-5 text-gray-700" />
                <span className="text-xs text-gray-700">Open</span>
              </button>

              <button
                onClick={() => handleQuickAction('toggle-mic')}
                className="flex flex-col items-center gap-1 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                {isMicOn ? (
                  <Mic className="w-5 h-5 text-blue-600" />
                ) : (
                  <MicOff className="w-5 h-5 text-gray-500" />
                )}
                <span className="text-xs text-gray-700">Voice</span>
              </button>

              <button
                onClick={() => handleQuickAction('toggle-mute')}
                className="flex flex-col items-center gap-1 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                {isMuted ? (
                  <VolumeX className="w-5 h-5 text-gray-500" />
                ) : (
                  <Volume2 className="w-5 h-5 text-blue-600" />
                )}
                <span className="text-xs text-gray-700">Sound</span>
              </button>

              <button
                onClick={() => handleQuickAction('toggle-notifications')}
                className="flex flex-col items-center gap-1 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                {notificationsEnabled ? (
                  <Bell className="w-5 h-5 text-blue-600" />
                ) : (
                  <BellOff className="w-5 h-5 text-gray-500" />
                )}
                <span className="text-xs text-gray-700">Alerts</span>
              </button>

              <button
                onClick={() => handleQuickAction('set-status')}
                className="flex flex-col items-center gap-1 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <RefreshCw className="w-5 h-5 text-gray-700" />
                <span className="text-xs text-gray-700">Status</span>
              </button>

              <button
                onClick={() => handleQuickAction('toggle-minimize')}
                className="flex flex-col items-center gap-1 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                {isMinimized ? (
                  <Monitor className="w-5 h-5 text-gray-700" />
                ) : (
                  <X className="w-5 h-5 text-gray-700" />
                )}
                <span className="text-xs text-gray-700">{isMinimized ? 'Show' : 'Hide'}</span>
              </button>
            </div>
          </div>

          {/* Connected Devices */}
          {connectedDevices.length > 0 && (
            <div className="p-4 border-b border-gray-200">
              <h3 className="font-medium text-gray-900 mb-3 flex items-center gap-2">
                <Wifi className="w-4 h-4" />
                Connected Devices ({connectedDevices.length})
              </h3>

              <div className="space-y-2">
                {connectedDevices.map(device => (
                  <div key={device.id} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                    <div className="flex items-center gap-2">
                      {getDeviceIcon(device.type)}
                      <div>
                        <p className="text-sm font-medium text-gray-900">{device.name}</p>
                        <p className="text-xs text-gray-600">{device.type}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <div className={`w-2 h-2 rounded-full ${getStatusColor(device.status)}`} />
                      {device.batteryLevel && (
                        <span className="text-xs text-gray-600">{device.batteryLevel}%</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recent Notifications */}
          {notifications.length > 0 && (
            <div className="p-4 border-b border-gray-200">
              <h3 className="font-medium text-gray-900 mb-3 flex items-center gap-2">
                <Bell className="w-4 h-4" />
                Notifications
                {unreadCount > 0 && (
                  <span className="bg-red-500 text-white text-xs px-2 py-1 rounded">
                    {unreadCount}
                  </span>
                )}
              </h3>

              <div className="space-y-2 max-h-32 overflow-y-auto">
                {notifications.slice(0, 5).map(notification => (
                  <div
                    key={notification.id}
                    className={`p-2 rounded text-sm ${
                      !notification.read ? 'bg-blue-50 border border-blue-200' : 'bg-gray-50'
                    }`}
                  >
                    <div className="flex items-start gap-2">
                      <span>{getNotificationIcon(notification.type)}</span>
                      <div className="flex-1">
                        <p className="font-medium text-gray-900">{notification.title}</p>
                        <p className="text-gray-600">{notification.message}</p>
                        <p className="text-xs text-gray-500">
                          {notification.timestamp.toLocaleTimeString()}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Footer Actions */}
          <div className="p-4 bg-gray-50">
            <div className="space-y-2">
              <button
                onClick={() => {
                  // Open settings
                  console.log('Opening settings...');
                }}
                className="w-full flex items-center gap-2 p-2 text-left hover:bg-gray-100 rounded transition-colors"
              >
                <Settings className="w-4 h-4 text-gray-600" />
                <span className="text-sm text-gray-700">Settings</span>
              </button>

              <button
                onClick={onExit}
                className="w-full flex items-center gap-2 p-2 text-left hover:bg-red-50 rounded transition-colors"
              >
                <Power className="w-4 h-4 text-red-600" />
                <span className="text-sm text-red-600">Exit MISA.AI</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};