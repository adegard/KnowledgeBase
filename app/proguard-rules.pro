# Keep WebView/CDN scripts referenced from the preview harness
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}