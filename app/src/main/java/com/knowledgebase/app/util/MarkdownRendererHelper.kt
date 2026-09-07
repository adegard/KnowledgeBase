package com.knowledgebase.app.util

import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Renders markdown to HTML inside a WebView for the live preview pane.
 *
 * The document is loaded once with a small harness page; subsequent edits
 * are pushed through `renderMarkdown()` via JavaScript without reloading.
 */
object MarkdownRendererHelper {

    private val pendingContent = java.util.concurrent.ConcurrentHashMap<Int, String>()
    private val darkMode = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()

    @JavascriptInterface
    fun onRendered() {}

    /**
     * Loads the preview harness page. Content passed here (and any content
     * pushed before the page finishes loading) is rendered once ready.
     */
    fun loadPreviewPage(webView: WebView, initialContent: String, dark: Boolean) {
        pendingContent[webView.hashCode()] = initialContent
        darkMode[webView.hashCode()] = dark
        val html = buildPreviewPage(initialContent, dark)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (view == null) return
                pendingContent[view.hashCode()]?.let { renderInto(view, it) }
                darkMode[view.hashCode()]?.let { applyTheme(view, it) }
            }
        }
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
    }

    fun renderInto(webView: WebView, content: String) {
        pendingContent[webView.hashCode()] = content
        val escaped = jsString(content)
        webView.evaluateJavascript("renderMarkdown($escaped);", null)
    }

    fun applyTheme(webView: WebView, dark: Boolean) {
        darkMode[webView.hashCode()] = dark
        webView.evaluateJavascript("applyTheme('${if (dark) "dark" else "light"}');", null)
    }

    fun setFontSize(webView: WebView, px: Float) {
        webView.evaluateJavascript("setFontSize($px);", null)
    }

    private fun jsString(text: String): String = JSON_QUOTE(text)

    private fun JSON_QUOTE(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\u2028' -> sb.append("\\u2028")
                '\u2029' -> sb.append("\\u2029")
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private fun buildPreviewPage(content: String, dark: Boolean): String {
        val initial = jsString(content)
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
            <style>
            :root { --fg:#1c1b18; --fg2:#6b6860; --fg3:#a09e98; --bg:#ffffff; --bg-alt:#f2f1ed;
                --border:#dddbd4; --accent:#2563a8; --accent-bg:#e8f0fb; }
            body.dark { --fg:#e8e6e0; --fg2:#9a9890; --fg3:#5c5a54; --bg:#1e1e24; --bg-alt:#26262e;
                --border:#2e2e38; --accent:#5b9de0; --accent-bg:#1a2a40; }
            * { box-sizing:border-box; }
            html,body { margin:0; padding:0; background:var(--bg); color:var(--fg);
                font-family:-apple-system,'Segoe UI',Roboto,sans-serif; font-size:var(--fs,14px);
                line-height:1.7; word-wrap:break-word; }
            body { padding:12px 16px; }
            h1 { font-size:1.7em; border-bottom:2px solid var(--border); padding-bottom:.3em; margin:0 0 .6em; }
            h2 { font-size:1.35em; margin:1.4em 0 .5em; }
            h3 { font-size:1.1em; margin:1.2em 0 .4em; }
            p { margin:0 0 .85em; }
            code { font-family:monospace; background:var(--bg-alt); padding:1px 5px; border-radius:3px; font-size:.88em; }
            pre { background:var(--bg-alt); border:1px solid var(--border); border-radius:6px; padding:12px 16px; overflow-x:auto; }
            pre code { background:none; padding:0; }
            blockquote { border-left:3px solid var(--accent); margin:0 0 1em; padding:6px 14px;
                background:var(--accent-bg); border-radius:0 6px 6px 0; color:var(--fg2); }
            table { border-collapse:collapse; width:100%; margin:0 0 1em; font-size:.93em; }
            th { background:var(--bg-alt); text-align:left; }
            th,td { border:1px solid var(--border); padding:6px 10px; }
            tr:nth-child(even) td { background:var(--bg-alt); }
            a { color:var(--accent); text-decoration:none; }
            a:hover { text-decoration:underline; }
            img { max-width:100%; }
            hr { border:none; border-top:1px solid var(--border); margin:1.5em 0; }
            ul,ol { padding-left:1.5em; margin:0 0 .85em; }
            li { margin:.2em 0; }
            .katex-display { overflow-x:auto; overflow-y:hidden; margin:1em 0; padding:4px 0; }
            .mermaid { text-align:center; }
            .doc-link { display:inline-flex; align-items:center; gap:8px; padding:6px 12px;
                border:1px solid var(--border); background:var(--bg-alt); border-radius:6px;
                font-size:13px; text-decoration:none; color:var(--fg); margin:4px 0; }
            .doc-link:hover { border-color:var(--accent); background:var(--accent-bg); }
            </style>
            </head>
            <body>
            <div id="content"></div>
            <script>
            marked.setOptions({ breaks:false, gfm:true });

            // Minimal offline fallback renderer used when the CDN is unreachable.
            function escapeHtml(s) {
                return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
                    .replace(/"/g,'&quot;');
            }
            function inlineMd(s) {
                s = escapeHtml(s);
                s = s.replace(/\*\*([^*]+)\*\*/g,'<strong>$1</strong>');
                s = s.replace(/(^|[^*])\*([^*]+)\*/g,'$1<em>$2</em>');
                s = s.replace(/`([^`]+)`/g,'<code>$1</code>');
                s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g,'<a href="$2">$1</a>');
                return s;
            }
            function renderFallback(md) {
                if (!md) return '';
                var out = [], inList = null, inCode = false, codeBuf = [], para = [];
                function endPara(){ if(para.length){ out.push('<p>'+inlineMd(para.join(' '))+'</p>'); para=[]; } }
                function endList(){ if(inList){ out.push('</'+inList+'>'); inList=null; } }
                md.split('\n').forEach(function(line){
                    if (inCode) {
                        if (line.trim() === '```') { inCode=false; out.push('<pre><code>'+codeBuf.join('\n')+'</code></pre>'); codeBuf=[]; return; }
                        codeBuf.push(line.replace(/&/g,'&amp;').replace(/</g,'&lt;')); return;
                    }
                    if (line.trim().startsWith('```')) { endPara(); endList(); inCode=true; codeBuf=[]; return; }
                    var t = line.trim();
                    if (t === '') { endPara(); endList(); return; }
                    var m;
                    if ((m = t.match(/^(#{1,6})\s+(.*)$/))) { endPara(); endList(); var h = m[1].length; out.push('<h'+h+'>'+inlineMd(m[2])+'</h'+h+'>'); return; }
                    if (t.indexOf('---') === 0 && t.length >= 3) { endPara(); endList(); out.push('<hr>'); return; }
                    if (t.startsWith('> ')) { endPara(); endList(); out.push('<blockquote><p>'+inlineMd(t.slice(2))+'</p></blockquote>'); return; }
                    if (t.startsWith('- ')) { endPara(); if(inList!=='ul'){ endList(); inList='ul'; out.push('<ul>'); } out.push('<li>'+inlineMd(t.slice(2))+'</li>'); return; }
                    if (t.startsWith('* ')) { endPara(); if(inList!=='ul'){ endList(); inList='ul'; out.push('<ul>'); } out.push('<li>'+inlineMd(t.slice(2))+'</li>'); return; }
                    if ((m = t.match(/^\s*\d+\.\s+(.*)$/))) { endPara(); if(inList!=='ol'){ endList(); inList='ol'; out.push('<ol>'); } out.push('<li>'+inlineMd(m[1])+'</li>'); return; }
                    if (t.startsWith('|') && (m = t.split('|').filter(function(x){return x.trim()!==''}).length) > 1) { endPara(); endList(); return; }
                    para.push(line.trim());
                });
                endPara(); endList();
                if (inCode) out.push('<pre><code>'+codeBuf.join('\n')+'</code></pre>');
                return out.join('\n');
            }

            function renderMarkdown(md) {
                var el = document.getElementById('content');
                var html = null;
                if (typeof marked !== 'undefined') {
                    try { html = marked.parse(md || ''); } catch(e){}
                }
                if (html === null) { html = renderFallback(md || ''); }
                el.innerHTML = html;
                try {
                    renderMathInElement(el, { delimiters:[
                        {left:'\\\\[',right:'\\\\]',display:true},
                        {left:'$$',right:'$$',display:true},
                        {left:'\\\\(',right:'\\\\)',display:false}
                    ], throwOnError:false });
                } catch(e){}
                try {
                    mermaid.initialize({ startOnLoad:false, theme: 'default' });
                    mermaid.run({ nodes: document.querySelectorAll('.language-mermaid') }).catch(function(){});
                } catch(e){}
            }
            function applyTheme(t) {
                document.body.classList.toggle('dark', t === 'dark');
            }
            function setFontSize(px) {
                document.documentElement.style.setProperty('--fs', px + 'px');
            }
            renderMarkdown($initial);
            applyTheme(${if (dark) "'dark'" else "'light'"});
            </script>
            </body>
            </html>
        """.trimIndent()
    }
}