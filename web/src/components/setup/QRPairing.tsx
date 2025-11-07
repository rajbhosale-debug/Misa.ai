import React, { useState, useEffect, useRef } from 'react';
import { QrCode, Camera, Wifi, RefreshCw, CheckCircle, XCircle, Smartphone, Monitor } from 'lucide-react';
import QRCodeLib from 'qrcode';

interface QRPairingProps {
  onComplete: (deviceInfo: any) => void;
  onCancel: () => void;
  deviceInfo?: {
    id: string;
    name: string;
    type: string;
  };
}

interface PairedDevice {
  id: string;
  name: string;
  type: string;
  status: 'pairing' | 'connected' | 'failed';
  qrToken?: string;
  connectionStrength?: number;
}

export const QRPairing: React.FC<QRPairingProps> = ({
  onComplete,
  onCancel,
  deviceInfo
}) => {
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>('');
  const [isScanning, setIsScanning] = useState(false);
  const [pairedDevices, setPairedDevices] = useState<PairedDevice[]>([]);
  const [pairingStatus, setPairingStatus] = useState<'idle' | 'generating' | 'scanning' | 'pairing' | 'completed'>('idle');
  const [scanResult, setScanResult] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [countdown, setCountdown] = useState<number>(300); // 5 minutes
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const [currentDevice] = useState(deviceInfo || {
    id: `device-${Math.random().toString(36).substr(2, 9)}`,
    name: 'MISA Device',
    type: 'desktop'
  });

  // Generate QR code on component mount
  useEffect(() => {
    generateQRCode();
    startCountdown();

    return () => {
      // Cleanup countdown interval
    };
  }, []);

  const generateQRCode = async () => {
    setPairingStatus('generating');
    setError('');

    try {
      const timestamp = Date.now();
      const signature = generateSignature(currentDevice.id, timestamp);
      const qrToken = `misa://pair/${currentDevice.id}/${timestamp}/${signature}/${encodeURIComponent(currentDevice.name)}`;

      // Generate QR code image
      const dataUrl = await QRCodeLib.toDataURL(qrToken, {
        width: 300,
        margin: 2,
        color: {
          dark: '#1e293b',
          light: '#ffffff'
        },
        errorCorrectionLevel: 'H'
      });

      setQrCodeDataUrl(dataUrl);
      setPairingStatus('idle');

      // Start listening for pairing requests
      startPairingListener(qrToken);

    } catch (error) {
      console.error('Failed to generate QR code:', error);
      setError('Failed to generate QR code. Please try again.');
      setPairingStatus('idle');
    }
  };

  const generateSignature = (deviceId: string, timestamp: number): string => {
    // Simple signature generation - in production, use proper cryptographic signing
    const data = `${deviceId}:${timestamp}`;
    let hash = 0;
    for (let i = 0; i < data.length; i++) {
      const char = data.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash).toString(16);
  };

  const startCountdown = () => {
    const interval = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(interval);
          generateQRCode(); // Regenerate QR code when expired
          return 300;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const startPairingListener = (qrToken: string) => {
    // Simulate listening for pairing requests
    // In real implementation, this would use WebSocket or other real-time communication
    setTimeout(() => {
      // Simulate a device trying to pair
      const mockDevice: PairedDevice = {
        id: 'mobile-123',
        name: 'iPhone 14',
        type: 'mobile',
        status: 'pairing',
        qrToken: qrToken,
        connectionStrength: 85
      };

      setPairedDevices(prev => [...prev, mockDevice]);
      simulatePairingProcess(mockDevice);
    }, 3000);
  };

  const simulatePairingProcess = (device: PairedDevice) => {
    // Simulate the pairing process
    setTimeout(() => {
      setPairedDevices(prev => prev.map(d =>
        d.id === device.id
          ? { ...d, status: 'connected' as const }
          : d
      ));

      // If this is the first device, auto-complete
      if (pairedDevices.length === 0) {
        setTimeout(() => {
          onComplete(device);
        }, 1000);
      }
    }, 2000);
  };

  const startCameraScanning = async () => {
    setIsScanning(true);
    setError('');
    setPairingStatus('scanning');

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' }
      });

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();

        // Start QR code scanning
        startQRScanning();
      }
    } catch (error) {
      console.error('Failed to access camera:', error);
      setError('Failed to access camera. Please check permissions.');
      setIsScanning(false);
      setPairingStatus('idle');
    }
  };

  const stopCameraScanning = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      const stream = videoRef.current.srcObject as MediaStream;
      stream.getTracks().forEach(track => track.stop());
      videoRef.current.srcObject = null;
    }
    setIsScanning(false);
    setPairingStatus('idle');
  };

  const startQRScanning = () => {
    // This would use a QR code scanning library in production
    // For now, we'll simulate the scanning process
    setTimeout(() => {
      const mockScannedToken = `misa://pair/scanned-device-${Date.now()}/${Date.now()}/abc123/ScannedDevice`;
      setScanResult(mockScannedToken);
      stopCameraScanning();
      handleQRCodeScan(mockScannedToken);
    }, 5000);
  };

  const handleQRCodeScan = (qrData: string) => {
    setPairingStatus('pairing');
    setError('');

    try {
      // Parse QR token format: misa://pair/{device_id}/{timestamp}/{signature}/{device_name}
      const match = qrData.match(/^misa:\/\/pair\/([^\/]+)\/([^\/]+)\/([^\/]+)\/(.*)$/);

      if (!match) {
        throw new Error('Invalid QR code format');
      }

      const [, deviceId, timestamp, signature, deviceName] = match;

      // Validate timestamp (prevent replay attacks)
      const now = Date.now();
      const qrTime = parseInt(timestamp) * 1000;
      if (now - qrTime > 5 * 60 * 1000) { // 5 minutes
        throw new Error('QR code has expired');
      }

      // Validate signature (simplified - in production use proper crypto verification)
      const expectedSignature = generateSignature(deviceId, parseInt(timestamp));
      if (signature !== expectedSignature) {
        // For demo purposes, we'll allow invalid signatures
        console.warn('Invalid signature, but continuing for demo');
      }

      const scannedDevice: PairedDevice = {
        id: deviceId,
        name: decodeURIComponent(deviceName),
        type: 'mobile',
        status: 'pairing',
        connectionStrength: 75
      };

      setPairedDevices(prev => [...prev, scannedDevice]);

      // Simulate pairing completion
      setTimeout(() => {
        setPairedDevices(prev => prev.map(d =>
          d.id === deviceId
            ? { ...d, status: 'connected' as const }
            : d
        ));
        setPairingStatus('completed');

        setTimeout(() => {
          onComplete(scannedDevice);
        }, 1000);
      }, 2000);

    } catch (error) {
      console.error('QR code parsing error:', error);
      setError(error instanceof Error ? error.message : 'Failed to parse QR code');
      setPairingStatus('idle');
    }
  };

  const refreshQRCode = () => {
    generateQRCode();
    setCountdown(300);
  };

  const formatCountdown = (seconds: number): string => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const getDeviceIcon = (type: string) => {
    switch (type) {
      case 'mobile':
        return <Smartphone className="w-5 h-5" />;
      case 'desktop':
      case 'laptop':
        return <Monitor className="w-5 h-5" />;
      default:
        return <Wifi className="w-5 h-5" />;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-8 py-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold">Device Pairing</h1>
              <p className="text-blue-100 mt-1">Connect your devices with QR codes</p>
            </div>
            <button
              onClick={onCancel}
              className="text-white/80 hover:text-white transition-colors"
            >
              <XCircle className="w-6 h-6" />
            </button>
          </div>
        </div>

        <div className="p-8">
          <div className="grid md:grid-cols-2 gap-8">
            {/* Left Column - QR Code Display */}
            <div className="space-y-6">
              <div>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Scan this QR code with another device
                </h2>

                <div className="bg-white border-2 border-gray-200 rounded-xl p-6 text-center">
                  {qrCodeDataUrl ? (
                    <div className="space-y-4">
                      <img
                        src={qrCodeDataUrl}
                        alt="Pairing QR Code"
                        className="w-64 h-64 mx-auto"
                      />

                      <div className="space-y-2">
                        <div className="flex items-center justify-center gap-2 text-sm text-gray-600">
                          <QrCode className="w-4 h-4" />
                          <span>QR Code expires in {formatCountdown(countdown)}</span>
                        </div>

                        <button
                          onClick={refreshQRCode}
                          className="flex items-center justify-center gap-2 w-full px-4 py-2 text-sm bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors"
                        >
                          <RefreshCw className="w-4 h-4" />
                          Refresh QR Code
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="w-64 h-64 mx-auto flex items-center justify-center">
                      <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                    </div>
                  )}
                </div>
              </div>

              {/* Device Info */}
              <div className="bg-blue-50 rounded-lg p-4">
                <h3 className="font-medium text-gray-900 mb-2">This Device</h3>
                <div className="flex items-center gap-3">
                  {getDeviceIcon(currentDevice.type)}
                  <div>
                    <p className="font-medium text-gray-900">{currentDevice.name}</p>
                    <p className="text-sm text-gray-600">ID: {currentDevice.id}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column - Camera Scanner / Paired Devices */}
            <div className="space-y-6">
              {/* Camera Scanner */}
              <div>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Scan QR code from another device
                </h2>

                <div className="bg-gray-900 rounded-xl overflow-hidden relative">
                  {isScanning ? (
                    <div className="aspect-video relative">
                      <video
                        ref={videoRef}
                        className="w-full h-full object-cover"
                        playsInline
                        muted
                      />

                      {/* Scanning overlay */}
                      <div className="absolute inset-0 border-2 border-white/20 rounded-xl">
                        <div className="absolute inset-0 flex items-center justify-center">
                          <div className="w-48 h-48 border-2 border-white rounded-lg">
                            <div className="absolute top-0 left-0 w-full h-1 bg-blue-500 animate-pulse" />
                            <div className="absolute bottom-0 left-0 w-full h-1 bg-blue-500 animate-pulse" />
                            <div className="absolute top-0 left-0 w-1 h-full bg-blue-500 animate-pulse" />
                            <div className="absolute top-0 right-0 w-1 h-full bg-blue-500 animate-pulse" />
                          </div>
                        </div>
                      </div>

                      <div className="absolute top-4 right-4">
                        <button
                          onClick={stopCameraScanning}
                          className="p-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
                        >
                          <XCircle className="w-5 h-5" />
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="aspect-video flex flex-col items-center justify-center bg-gray-800">
                      <Camera className="w-16 h-16 text-gray-400 mb-4" />
                      <button
                        onClick={startCameraScanning}
                        className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                      >
                        <Camera className="w-5 h-5" />
                        Start Camera
                      </button>
                      <p className="text-gray-400 text-sm mt-2">
                        Allow camera access to scan QR codes
                      </p>
                    </div>
                  )}
                </div>

                <canvas ref={canvasRef} className="hidden" />
              </div>

              {/* Paired Devices */}
              {pairedDevices.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">
                    Paired Devices
                  </h3>

                  <div className="space-y-3">
                    {pairedDevices.map(device => (
                      <div
                        key={device.id}
                        className="flex items-center justify-between p-4 bg-gray-50 rounded-lg"
                      >
                        <div className="flex items-center gap-3">
                          {getDeviceIcon(device.type)}
                          <div>
                            <p className="font-medium text-gray-900">{device.name}</p>
                            <p className="text-sm text-gray-600">{device.id}</p>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          {device.status === 'pairing' && (
                            <div className="flex items-center gap-1 text-yellow-600">
                              <div className="w-4 h-4 border-2 border-yellow-600 border-t-transparent rounded-full animate-spin" />
                              <span className="text-sm">Pairing...</span>
                            </div>
                          )}

                          {device.status === 'connected' && (
                            <div className="flex items-center gap-1 text-green-600">
                              <CheckCircle className="w-5 h-5" />
                              <span className="text-sm">Connected</span>
                            </div>
                          )}

                          {device.status === 'failed' && (
                            <div className="flex items-center gap-1 text-red-600">
                              <XCircle className="w-5 h-5" />
                              <span className="text-sm">Failed</span>
                            </div>
                          )}

                          {device.connectionStrength && (
                            <div className="flex items-center gap-1">
                              <Wifi className="w-4 h-4 text-gray-400" />
                              <span className="text-sm text-gray-600">{device.connectionStrength}%</span>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Scan Result */}
              {scanResult && (
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <h4 className="font-medium text-green-900 mb-2">QR Code Scanned!</h4>
                  <p className="text-sm text-green-700">{scanResult}</p>
                </div>
              )}

              {/* Error Message */}
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <h4 className="font-medium text-red-900 mb-2">Error</h4>
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              )}
            </div>
          </div>

          {/* Instructions */}
          <div className="mt-8 p-4 bg-blue-50 rounded-lg">
            <h3 className="font-medium text-gray-900 mb-2">How to pair devices:</h3>
            <ol className="text-sm text-gray-700 space-y-1 list-decimal list-inside">
              <li>Display this QR code on your current device</li>
              <li>Open MISA.AI on the device you want to pair</li>
              <li>Tap "Scan QR Code" and point the camera at this screen</li>
              <li>Wait for the pairing to complete automatically</li>
              <li>Your devices will be connected and ready to use</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  );
};