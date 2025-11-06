//! Vision System Module
//! Provides advanced computer vision capabilities including screen capture,
//! UI element detection, OCR, and visual intelligence

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;
use anyhow::{anyhow, Result};
use serde::{Deserialize, Serialize};
use image::{DynamicImage, ImageFormat, RgbImage};
use crate::error::MisaError;

pub mod capture;
pub mod detection;
pub mod ocr;
pub mod analysis;
pub mod ui;

pub use capture::*;
pub use detection::*;
pub use ocr::*;
pub use analysis::*;
pub use ui::*;

/// Vision System Core
/// Provides comprehensive computer vision capabilities
pub struct VisionSystem {
    /// Screen capture manager
    screen_capture: Arc<RwLock<ScreenCaptureManager>>,

    /// UI element detector
    ui_detector: Arc<RwLock<UIElementDetector>>,

    /// OCR engine
    ocr_engine: Arc<RwLock<OCREngine>>,

    /// Visual analyzer
    visual_analyzer: Arc<RwLock<VisualAnalyzer>>,

    /// Vision configuration
    config: VisionConfig,

    /// Performance metrics
    metrics: Arc<RwLock<VisionMetrics>>,
}

impl VisionSystem {
    /// Create new Vision system
    pub async fn new(config: VisionConfig) -> Result<Self> {
        let screen_capture = Arc::new(RwLock::new(
            ScreenCaptureManager::new(&config.capture)?
        ));

        let ui_detector = Arc::new(RwLock::new(
            UIElementDetector::new(&config.detection)?
        ));

        let ocr_engine = Arc::new(RwLock::new(
            OCREngine::new(&config.ocr)?
        ));

        let visual_analyzer = Arc::new(RwLock::new(
            VisualAnalyzer::new(&config.analysis)?
        ));

        let metrics = Arc::new(RwLock::new(VisionMetrics::new()));

        Ok(Self {
            screen_capture,
            ui_detector,
            ocr_engine,
            visual_analyzer,
            config,
            metrics,
        })
    }

    /// Initialize the vision system
    pub async fn initialize(&self) -> Result<()> {
        // Initialize all subsystems
        self.screen_capture.write().await.initialize().await?;
        self.ui_detector.write().await.initialize().await?;
        self.ocr_engine.write().await.initialize().await?;
        self.visual_analyzer.write().await.initialize().await?;

        Ok(())
    }

    /// Capture screen with specified parameters
    pub async fn capture_screen(&self, params: CaptureParams) -> Result<ScreenCapture> {
        let start_time = Instant::now();

        let capture = self.screen_capture
            .write()
            .await
            .capture_screen(params)
            .await?;

        // Update metrics
        let capture_time = start_time.elapsed();
        self.metrics.write().await.record_capture(capture_time);

        Ok(capture)
    }

    /// Detect UI elements in an image
    pub async fn detect_ui_elements(&self, image: &DynamicImage) -> Result<Vec<UIElement>> {
        let start_time = Instant::now();

        let elements = self.ui_detector
            .write()
            .await
            .detect_elements(image)
            .await?;

        // Update metrics
        let detection_time = start_time.elapsed();
        self.metrics.write().await.record_detection(detection_time, elements.len());

        Ok(elements)
    }

    /// Extract text from image using OCR
    pub async fn extract_text(&self, image: &DynamicImage) -> Result<Vec<TextRegion>> {
        let start_time = Instant::now();

        let text_regions = self.ocr_engine
            .write()
            .await
            .extract_text(image)
            .await?;

        // Update metrics
        let ocr_time = start_time.elapsed();
        self.metrics.write().await.record_ocr(ocr_time, text_regions.len());

        Ok(text_regions)
    }

    /// Perform comprehensive visual analysis
    pub async fn analyze_image(&self, image: &DynamicImage) -> Result<VisualAnalysis> {
        let start_time = Instant::now();

        let analysis = self.visual_analyzer
            .write()
            .await
            .analyze(image)
            .await?;

        // Update metrics
        let analysis_time = start_time.elapsed();
        self.metrics.write().await.record_analysis(analysis_time);

        Ok(analysis)
    }

    /// Get current metrics
    pub async fn get_metrics(&self) -> VisionMetrics {
        self.metrics.read().await.clone()
    }

    /// Update configuration
    pub async fn update_config(&mut self, new_config: VisionConfig) -> Result<()> {
        self.config = new_config;

        // Reinitialize subsystems with new config
        self.screen_capture.write().await.update_config(&self.config.capture)?;
        self.ui_detector.write().await.update_config(&self.config.detection)?;
        self.ocr_engine.write().await.update_config(&self.config.ocr)?;
        self.visual_analyzer.write().await.update_config(&self.config.analysis)?;

        Ok(())
    }

    /// Perform intelligent screenshot with automatic analysis
    pub async fn intelligent_screenshot(&self, params: IntelligentScreenshotParams) -> Result<IntelligentScreenshot> {
        // Capture screen
        let capture_params = CaptureParams {
            region: params.region,
            quality: params.quality,
            format: ImageFormat::PNG,
        };
        let capture = self.capture_screen(capture_params).await?;

        // Convert to DynamicImage
        let image = capture.to_dynamic_image().map_err(|e| anyhow!("Failed to convert image: {}", e))?;

        // Perform parallel analysis
        let (ui_elements, text_regions, visual_analysis) = tokio::try_join!(
            self.detect_ui_elements(&image),
            self.extract_text(&image),
            self.analyze_image(&image)
        )?;

        // Apply AI insights if requested
        let insights = if params.include_ai_insights {
            self.generate_ai_insights(&image, &ui_elements, &text_regions).await?
        } else {
            Vec::new()
        };

        Ok(IntelligentScreenshot {
            capture,
            ui_elements,
            text_regions,
            visual_analysis,
            insights,
            timestamp: chrono::Utc::now(),
        })
    }

    /// Generate AI-powered insights
    async fn generate_ai_insights(
        &self,
        image: &DynamicImage,
        ui_elements: &[UIElement],
        text_regions: &[TextRegion],
    ) -> Result<Vec<AIInsight>> {
        let mut insights = Vec::new();

        // Analyze UI patterns
        if ui_elements.len() > 5 {
            insights.push(AIInsight {
                type_: InsightType::ComplexUI,
                confidence: 0.8,
                description: "Complex UI with many interactive elements detected".to_string(),
                recommendations: vec![
                    "Consider simplifying the interface for better user experience".to_string(),
                ],
            });
        }

        // Analyze text density
        let total_text_chars: usize = text_regions.iter().map(|r| r.text.len()).sum();
        if total_text_chars > 1000 {
            insights.push(AIInsight {
                type_: InsightType::HighTextDensity,
                confidence: 0.7,
                description: "High text density detected".to_string(),
                recommendations: vec![
                    "Consider breaking down information into smaller chunks".to_string(),
                    "Use visual hierarchy to improve readability".to_string(),
                ],
            });
        }

        // Analyze color scheme
        let color_analysis = self.analyze_color_scheme(image).await?;
        if let Some(color_insight) = self.generate_color_insights(color_analysis) {
            insights.push(color_insight);
        }

        Ok(insights)
    }

    /// Analyze color scheme of image
    async fn analyze_color_scheme(&self, image: &DynamicImage) -> Result<ColorAnalysis> {
        let rgb_image = image.to_rgb8();
        let mut color_counts = HashMap::new();

        // Sample colors from the image
        for pixel in rgb_image.pixels().step_by(100) {
            let color = (pixel[0], pixel[1], pixel[2]);
            *color_counts.entry(color).or_insert(0) += 1;
        }

        // Find dominant colors
        let mut colors: Vec<_> = color_counts.into_iter().collect();
        colors.sort_by(|a, b| b.1.cmp(&a.1));

        let dominant_colors: Vec<RGBColor> = colors
            .into_iter()
            .take(5)
            .map(|(color, count)| RGBColor {
                r: color.0,
                g: color.1,
                b: color.2,
                percentage: (count as f32 / (rgb_image.width() * rgb_image.height()) as f32) * 100.0,
            })
            .collect();

        Ok(ColorAnalysis {
            dominant_colors,
            average_brightness: self.calculate_average_brightness(&rgb_image),
            contrast_ratio: self.calculate_contrast_ratio(&rgb_image),
        })
    }

    /// Generate insights based on color analysis
    fn generate_color_insights(&self, analysis: ColorAnalysis) -> Option<AIInsight> {
        if analysis.contrast_ratio < 3.0 {
            Some(AIInsight {
                type_: InsightType::LowContrast,
                confidence: 0.8,
                description: "Low contrast detected, may affect readability".to_string(),
                recommendations: vec![
                    "Increase contrast between text and background".to_string(),
                    "Use higher contrast color combinations".to_string(),
                ],
            })
        } else {
            None
        }
    }

    /// Calculate average brightness
    fn calculate_average_brightness(&self, image: &RgbImage) -> f32 {
        let mut total_brightness = 0u64;
        let pixel_count = (image.width() * image.height()) as u64;

        for pixel in image.pixels() {
            let brightness = (pixel[0] as u64 + pixel[1] as u64 + pixel[2] as u64) / 3;
            total_brightness += brightness;
        }

        (total_brightness as f32 / pixel_count as f32) / 255.0
    }

    /// Calculate contrast ratio
    fn calculate_contrast_ratio(&self, image: &RgbImage) -> f32 {
        let mut min_brightness = 255.0;
        let mut max_brightness = 0.0;

        for pixel in image.pixels().step_by(50) {
            let brightness = (pixel[0] as f32 + pixel[1] as f32 + pixel[2] as f32) / 3.0 / 255.0;
            min_brightness = min_brightness.min(brightness);
            max_brightness = max_brightness.max(brightness);
        }

        if min_brightness == 0.0 { return max_brightness; }
        max_brightness / min_brightness
    }
}

/// Vision system configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VisionConfig {
    /// Screen capture configuration
    pub capture: CaptureConfig,

    /// UI detection configuration
    pub detection: DetectionConfig,

    /// OCR configuration
    pub ocr: OCRConfig,

    /// Analysis configuration
    pub analysis: AnalysisConfig,
}

impl Default for VisionConfig {
    fn default() -> Self {
        Self {
            capture: CaptureConfig::default(),
            detection: DetectionConfig::default(),
            ocr: OCRConfig::default(),
            analysis: AnalysisConfig::default(),
        }
    }
}

/// Intelligent screenshot parameters
#[derive(Debug, Clone)]
pub struct IntelligentScreenshotParams {
    pub region: Option<ScreenRegion>,
    pub quality: CaptureQuality,
    pub include_ai_insights: bool,
    pub analysis_types: Vec<AnalysisType>,
}

/// Intelligent screenshot result
#[derive(Debug, Clone)]
pub struct IntelligentScreenshot {
    pub capture: ScreenCapture,
    pub ui_elements: Vec<UIElement>,
    pub text_regions: Vec<TextRegion>,
    pub visual_analysis: VisualAnalysis,
    pub insights: Vec<AIInsight>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

/// AI insight
#[derive(Debug, Clone)]
pub struct AIInsight {
    pub type_: InsightType,
    pub confidence: f32,
    pub description: String,
    pub recommendations: Vec<String>,
}

/// Insight types
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum InsightType {
    ComplexUI,
    HighTextDensity,
    LowContrast,
    PoorLayout,
    AccessibilityIssue,
    ColorHarmony,
    PerformanceIssue,
}

/// Color analysis result
#[derive(Debug, Clone)]
pub struct ColorAnalysis {
    pub dominant_colors: Vec<RGBColor>,
    pub average_brightness: f32,
    pub contrast_ratio: f32,
}

/// RGB color with percentage
#[derive(Debug, Clone)]
pub struct RGBColor {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub percentage: f32,
}

/// Vision performance metrics
#[derive(Debug, Clone)]
pub struct VisionMetrics {
    pub captures_performed: u64,
    pub elements_detected: u64,
    pub text_regions_found: u64,
    pub analyses_performed: u64,
    pub average_capture_time: Duration,
    pub average_detection_time: Duration,
    pub average_ocr_time: Duration,
    pub average_analysis_time: Duration,
}

impl VisionMetrics {
    pub fn new() -> Self {
        Self {
            captures_performed: 0,
            elements_detected: 0,
            text_regions_found: 0,
            analyses_performed: 0,
            average_capture_time: Duration::ZERO,
            average_detection_time: Duration::ZERO,
            average_ocr_time: Duration::ZERO,
            average_analysis_time: Duration::ZERO,
        }
    }

    pub fn record_capture(&mut self, duration: Duration) {
        self.captures_performed += 1;
        self.average_capture_time = self.update_average(
            self.average_capture_time,
            duration,
            self.captures_performed,
        );
    }

    pub fn record_detection(&mut self, duration: Duration, element_count: usize) {
        self.elements_detected += element_count as u64;
        // Update average detection time similar to record_capture
    }

    pub fn record_ocr(&mut self, duration: Duration, text_count: usize) {
        self.text_regions_found += text_count as u64;
        // Update average OCR time similar to record_capture
    }

    pub fn record_analysis(&mut self, duration: Duration) {
        self.analyses_performed += 1;
        self.average_analysis_time = self.update_average(
            self.average_analysis_time,
            duration,
            self.analyses_performed,
        );
    }

    fn update_average(&self, current: Duration, new: Duration, count: u64) -> Duration {
        if count == 1 {
            new
        } else {
            Duration::from_nanos(
                (current.as_nanos() as u64 * (count - 1) + new.as_nanos() as u64) / count
            )
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_vision_system_creation() {
        let config = VisionConfig::default();
        let vision_system = VisionSystem::new(config).await;
        assert!(vision_system.is_ok());
    }

    #[tokio::test]
    async fn test_metrics_tracking() {
        let mut metrics = VisionMetrics::new();
        metrics.record_capture(Duration::from_millis(100));
        assert_eq!(metrics.captures_performed, 1);
    }
}