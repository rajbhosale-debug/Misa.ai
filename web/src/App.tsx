import React, { Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast/toast';

import LoadingScreen from './components/ui/LoadingScreen';
import ErrorBoundary from './components/ui/ErrorBoundary';
import Layout from './components/layout/Layout';

// Lazy load routes for code splitting
const Home = React.lazy(() => import('./pages/Home'));
const Calendar = React.lazy(() => import('./pages/apps/Calendar'));
const Notes = React.lazy(() => import('./pages/apps/Notes'));
const TaskFlow = React.lazy(() => import('./pages/apps/TaskFlow'));
const FileHub = React.lazy(() => import('./pages/apps/FileHub'));
const Focus = React.lazy(() => import('./pages/apps/Focus'));
const Persona = React.lazy(() => import('./pages/apps/Persona'));
const WebIQ = React.lazy(() => import('./pages/apps/WebIQ'));
const ChatSync = React.lazy(() => import('./pages/apps/ChatSync'));
const Meet = React.lazy(() => import('./pages/apps/Meet'));
const IotHome = React.lazy(() => import('./pages/apps/IotHome'));
const PowerSense = React.lazy(() => import('./pages/apps/PowerSense'));
const WorkSuite = React.lazy(() => import('./pages/apps/WorkSuite'));
const DevHub = React.lazy(() => import('./pages/apps/DevHub'));
const Store = React.lazy(() => import('./pages/apps/Store'));
const Vault = React.lazy(() => import('./pages/apps/Vault'));
const BioLink = React.lazy(() => import('./pages/apps/BioLink'));
const Workflow = React.lazy(() => import('./pages/apps/Workflow'));
const Settings = React.lazy(() => import('./pages/Settings'));
const Devices = React.lazy(() => import('./pages/Devices'));
const RemoteDesktop = React.lazy(() => import('./pages/RemoteDesktop'));

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <Router>
          <div className="min-h-screen bg-background text-foreground">
            <Routes>
                {/* Main App with Layout */}
                <Route path="/" element={<Layout />}>
                  <Route index element={<Navigate to="/home" replace />} />

                  {/* Home Dashboard */}
                  <Route path="home" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Home />
                    </Suspense>
                  } />

                  {/* 18 Integrated Applications */}
                  <Route path="calendar" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Calendar />
                    </Suspense>
                  } />

                  <Route path="notes" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Notes />
                    </Suspense>
                  } />

                  <Route path="tasks" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <TaskFlow />
                    </Suspense>
                  } />

                  <Route path="files" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <FileHub />
                    </Suspense>
                  } />

                  <Route path="focus" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Focus />
                    </Suspense>
                  } />

                  <Route path="persona" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Persona />
                    </Suspense>
                  } />

                  <Route path="webiq" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <WebIQ />
                    </Suspense>
                  } />

                  <Route path="chatsync" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <ChatSync />
                    </Suspense>
                  } />

                  <Route path="meet" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Meet />
                    </Suspense>
                  } />

                  <Route path="home-iot" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <IotHome />
                    </Suspense>
                  } />

                  <Route path="powersense" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <PowerSense />
                    </Suspense>
                  } />

                  <Route path="worksuite" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <WorkSuite />
                    </Suspense>
                  } />

                  <Route path="devhub" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <DevHub />
                    </Suspense>
                  } />

                  <Route path="store" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Store />
                    </Suspense>
                  } />

                  <Route path="vault" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Vault />
                    </Suspense>
                  } />

                  <Route path="biolink" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <BioLink />
                    </Suspense>
                  } />

                  <Route path="workflow" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Workflow />
                    </Suspense>
                  } />

                  {/* Settings and Management */}
                  <Route path="settings" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Settings />
                    </Suspense>
                  } />

                  <Route path="devices" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <Devices />
                    </Suspense>
                  } />

                  <Route path="remote-desktop" element={
                    <Suspense fallback={<LoadingScreen />}>
                      <RemoteDesktop />
                    </Suspense>
                  } />
                </Route>

                {/* Authentication Routes */}
                <Route path="/auth" element={
                  <Suspense fallback={<LoadingScreen />}>
                    <div>Auth Page (To be implemented)</div>
                  </Suspense>
                } />

                {/* Catch-all route */}
                <Route path="*" element={<Navigate to="/home" replace />} />
              </Routes>
            </div>
          </Router>
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              style: {
                background: 'hsl(var(--background))',
                color: 'hsl(var(--foreground))',
                border: '1px solid hsl(var(--border))',
              },
            }}
          />
        </Router>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;