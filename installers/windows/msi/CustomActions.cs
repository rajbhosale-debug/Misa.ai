using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Configuration.Install;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Management;
using System.Net;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.ServiceProcess;
using System.Text;
using Microsoft.Deployment.WindowsInstaller;
using Microsoft.Win32;

namespace MisaCustomActions
{
    [RunInstaller(true)]
    public class CustomActions : Installer
    {
        [CustomAction]
        public static ActionResult ValidatePrerequisites(Session session)
        {
            try
            {
                session.Log("Validating MISA.AI prerequisites...");

                var issues = new List<string>();

                // Check Windows version
                var osVersion = Environment.OSVersion;
                session.Log($"Operating System: {osVersion}");

                if (osVersion.Version.Major < 10)
                {
                    issues.Add("MISA.AI requires Windows 10 or later");
                    session["MISA_OSStatus"] = "❌ Windows 10+ required";
                }
                else
                {
                    session["MISA_OSStatus"] = "✅ Windows 10+ detected";
                }

                // Check architecture
                if (!Environment.Is64BitOperatingSystem)
                {
                    issues.Add("MISA.AI requires 64-bit Windows");
                    session["MISA_ArchStatus"] = "❌ 64-bit required";
                }
                else
                {
                    session["MISA_ArchStatus"] = "✅ 64-bit detected";
                }

                // Check memory
                var memoryStatus = CheckMemory();
                session["MISA_RAMStatus"] = memoryStatus.Item1;
                if (memoryStatus.Item2 < 4)
                {
                    issues.Add("MISA.AI requires at least 4GB RAM");
                }

                // Check disk space
                var diskStatus = CheckDiskSpace();
                session["MISA_DiskStatus"] = diskStatus.Item1;
                if (diskStatus.Item2 < 10)
                {
                    issues.Add("MISA.AI requires at least 10GB free disk space");
                }

                // Check network connectivity
                var netStatus = CheckNetwork();
                session["MISA_NetStatus"] = netStatus.Item1;

                // Set overall status
                if (issues.Count > 0)
                {
                    session["MISA_CheckMessage"] = "❌ Prerequisites not met";
                    session["MISA_WarningMessage"] = string.Join("\n", issues);
                    session.Log($"Prerequisites validation failed: {string.Join(", ", issues)}");
                    return ActionResult.Failure;
                }
                else
                {
                    session["MISA_CheckMessage"] = "✅ All prerequisites met";
                    session["MISA_WarningMessage"] = "";
                    session.Log("Prerequisites validation passed");
                    return ActionResult.Success;
                }
            }
            catch (Exception ex)
            {
                session.Log($"Prerequisites validation error: {ex.Message}");
                session["MISA_CheckMessage"] = "❌ Error checking prerequisites";
                session["MISA_WarningMessage"] = ex.Message;
                return ActionResult.Failure;
            }
        }

        [CustomAction]
        public static ActionResult ConfigureFirewall(Session session)
        {
            try
            {
                session.Log("Configuring Windows Firewall for MISA.AI...");

                var installPath = session["INSTALLFOLDER"];
                var rules = new[]
                {
                    ("MISA.AI - Main Application", $"{installPath}\\MisaAI.exe", "", ""),
                    ("MISA.AI - Kernel Service", $"{installPath}\\MisaKernel.exe", "", "8080,8443"),
                    ("MISA.AI - Web Interface", "", "", "3000")
                };

                foreach (var (name, program, description, ports) in rules)
                {
                    CreateFirewallRule(name, program, description, ports);
                }

                session.Log("Windows Firewall configuration completed");
                return ActionResult.Success;
            }
            catch (Exception ex)
            {
                session.Log($"Firewall configuration failed: {ex.Message}");
                return ActionResult.Failure;
            }
        }

        [CustomAction]
        public static ActionResult RegisterService(Session session)
        {
            try
            {
                session.Log("Registering MISA Kernel Windows service...");

                var installPath = session["INSTALLFOLDER"];
                var serviceName = "MisaKernel";
                var executablePath = Path.Combine(installPath, "MisaKernel.exe");

                if (!File.Exists(executablePath))
                {
                    session.Log($"Service executable not found: {executablePath}");
                    return ActionResult.Failure;
                }

                // Remove existing service
                RemoveService(serviceName);

                // Create new service
                var process = new Process
                {
                    StartInfo = new ProcessStartInfo
                    {
                        FileName = "sc.exe",
                        Arguments = $"create \"{serviceName}\" binPath= \"{executablePath}\" start= auto DisplayName= \"MISA.AI Kernel Service\"",
                        UseShellExecute = false,
                        CreateNoWindow = true,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true
                    }
                };

                process.Start();
                process.WaitForExit();

                if (process.ExitCode != 0)
                {
                    var error = process.StandardError.ReadToEnd();
                    session.Log($"Service creation failed: {error}");
                    return ActionResult.Failure;
                }

                // Set service description
                process.StartInfo.Arguments = $"description \"{serviceName}\" \"MISA.AI Kernel - Background AI Assistant Service\"";
                process.Start();
                process.WaitForExit();

                session.Log("Windows service registration completed");
                return ActionResult.Success;
            }
            catch (Exception ex)
            {
                session.Log($"Service registration failed: {ex.Message}");
                return ActionResult.Failure;
            }
        }

        [CustomAction]
        public static ActionResult SetInstallProgress(Session session)
        {
            try
            {
                var progress = session["MISA_InstallProgress"];
                var status = session["MISA_InstallStatus"];
                var action = session["MISA_CurrentAction"];
                var step = session["MISA_CurrentStep"];

                session.Log($"Progress update: {progress}% - {status} - {action}");
                return ActionResult.Success;
            }
            catch (Exception ex)
            {
                session.Log($"Progress update failed: {ex.Message}");
                return ActionResult.Failure;
            }
        }

        [CustomAction]
        public static ActionResult UpdateProgressSteps(Session session)
        {
            try
            {
                var steps = new[]
                {
                    "Copying application files",
                    "Installing dependencies",
                    "Configuring Windows service",
                    "Setting up firewall rules",
                    "Downloading AI models",
                    "Creating desktop shortcuts",
                    "Finalizing configuration"
                };

                var currentStep = int.Parse(session["MISA_CurrentStepIndex"] ?? "0");
                if (currentStep < steps.Length)
                {
                    session["MISA_CurrentStep"] = steps[currentStep];
                    session.Log($"Progress step {currentStep + 1}: {steps[currentStep]}");
                }

                return ActionResult.Success;
            }
            catch (Exception ex)
            {
                session.Log($"Progress step update failed: {ex.Message}");
                return ActionResult.Failure;
            }
        }

        private static Tuple<string, int> CheckMemory()
        {
            try
            {
                var searcher = new ManagementObjectSearcher("SELECT TotalVisibleMemorySize FROM Win32_ComputerSystem");
                foreach (ManagementObject obj in searcher.Get())
                {
                    var totalKB = Convert.ToInt64(obj["TotalVisibleMemorySize"]);
                    var totalGB = totalKB / 1024.0 / 1024.0;
                    var status = totalGB >= 8 ? $"✅ {totalGB:F1}GB (Recommended)" : $"⚠️ {totalGB:F1}GB (Minimum)";
                    return Tuple.Create(status, (int)totalGB);
                }
            }
            catch
            {
                // Fallback to GC method
                var gcMemory = GC.GetTotalMemory(false) / 1024 / 1024 / 1024;
                return Tuple.Create($"⚠️ ~{gcMemory}GB (Estimated)", gcMemory);
            }

            return Tuple.Create("❌ Unable to determine", 0);
        }

        private static Tuple<string, int> CheckDiskSpace()
        {
            try
            {
                var drive = new DriveInfo(Path.GetPathRoot(Environment.GetFolderPath(Environment.SpecialFolder.System)));
                var freeGB = drive.AvailableFreeSpace / 1024.0 / 1024.0 / 1024.0;
                var status = freeGB >= 20 ? $"✅ {freeGB:F1}GB (Plenty)" :
                             freeGB >= 10 ? $"✅ {freeGB:F1}GB (Sufficient)" :
                             $"❌ {freeGB:F1}GB (Insufficient)";
                return Tuple.Create(status, (int)freeGB);
            }
            catch
            {
                return Tuple.Create("❌ Unable to determine", 0);
            }
        }

        private static Tuple<string, bool> CheckNetwork()
        {
            try
            {
                using (var client = new WebClient())
                {
                    client.DownloadString("http://www.google.com");
                    return Tuple.Create("✅ Connected", true);
                }
            }
            catch
            {
                return Tuple.Create("❌ No internet connection", false);
            }
        }

        private static void CreateFirewallRule(string name, string program, string description, string ports)
        {
            try
            {
                var process = new Process();

                if (!string.IsNullOrEmpty(program))
                {
                    process.StartInfo = new ProcessStartInfo
                    {
                        FileName = "netsh",
                        Arguments = $"advfirewall firewall add rule name=\"{name}\" dir=in action=allow program=\"{program}\" enable=yes",
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };
                }
                else if (!string.IsNullOrEmpty(ports))
                {
                    process.StartInfo = new ProcessStartInfo
                    {
                        FileName = "netsh",
                        Arguments = $"advfirewall firewall add rule name=\"{name}\" dir=in action=allow protocol=TCP localport={ports}",
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };
                }

                if (process.StartInfo != null)
                {
                    process.Start();
                    process.WaitForExit();
                }
            }
            catch
            {
                // Ignore firewall rule creation errors
            }
        }

        private static void RemoveService(string serviceName)
        {
            try
            {
                // Stop service if running
                var process = new Process
                {
                    StartInfo = new ProcessStartInfo
                    {
                        FileName = "sc.exe",
                        Arguments = $"stop \"{serviceName}\"",
                        UseShellExecute = false,
                        CreateNoWindow = true
                    }
                };
                process.Start();
                process.WaitForExit();

                // Delete service
                process.StartInfo.Arguments = $"delete \"{serviceName}\"";
                process.Start();
                process.WaitForExit();
            }
            catch
            {
                // Service may not exist, ignore errors
            }
        }
    }
}