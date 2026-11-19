package com.davidlg.iptvplayer.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Shaka Player real (el mismo motor y la misma configuración que la app de LG) dentro de un WebView.
 * Se usa como respaldo cuando ExoPlayer no puede abrir un canal (p. ej. cifrado CENC/CBCS con ClearKey).
 * Un solo WebView se reutiliza para todos los canales, así el cambio de canal sigue siendo rápido.
 */
@SuppressLint("SetJavaScriptEnabled")
class ShakaWebView(ctx: Context, private val onEvent: (String, String) -> Unit) : WebView(ctx) {

    init {
        setBackgroundColor(Color.BLACK)
        isFocusable = false
        isFocusableInTouchMode = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.userAgentString = settings.userAgentString + " DavidLG/Android"
        addJavascriptInterface(Bridge(), "Android")
        webViewClient = WebViewClient()
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onPermissionRequest(request: android.webkit.PermissionRequest) { request.grant(request.resources) }
        }
        loadDataWithBaseURL("https://davidlg.local/", HTML, "text/html", "utf-8", null)
    }

    fun playJson(json: String) { evaluateJavascript("window.dlPlay($json)", null) }
    fun stopPlayback() { evaluateJavascript("window.dlStop()", null) }

    private inner class Bridge {
        @JavascriptInterface fun onState(state: String, msg: String) { post { onEvent(state, msg) } }
    }

    companion object {
        private const val HTML = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}video{width:100%;height:100%;object-fit:contain;background:#000}</style></head>
<body><video id="v" autoplay playsinline></video>
<script>
var Q=null,P=null,SEQ=0,V=document.getElementById('v');
window.dlPlay=function(c){Q=c;if(P)run(c)};
window.dlStop=function(){SEQ++;Q=null;try{V.pause()}catch(e){};if(P){P.unload().catch(function(){})}};
V.addEventListener('playing',function(){Android.onState('playing','')});
V.addEventListener('waiting',function(){Android.onState('buffering','')});
function boot(){
  shaka.polyfill.installAll();
  if(!shaka.Player.isBrowserSupported()){Android.onState('error','Shaka no es compatible con este WebView');return}
  P=new shaka.Player(V);
  P.addEventListener('error',function(e){var d=e.detail||{};Android.onState('error','Shaka '+(d.code||''))});
  P.getNetworkingEngine().registerRequestFilter(function(type,req){
    if(type===shaka.net.NetworkingEngine.RequestType.LICENSE&&window._hdr){for(var k in window._hdr){req.headers[k]=window._hdr[k]}}
  });
  if(Q)run(Q)
}
async function run(c){
  var my=++SEQ;
  try{
    var drm={};
    if(c.clearKeys)drm.clearKeys=c.clearKeys;
    var sv={};if(c.server)sv['com.widevine.alpha']=c.server;if(c.clearServer)sv['org.w3.clearkey']=c.clearServer;
    if(c.server||c.clearServer)drm.servers=sv;
    window._hdr=c.headers||null;
    P.configure({drm:drm,abr:{enabled:true,defaultBandwidthEstimate:5000000},streaming:{bufferingGoal:c.vod?20:12,rebufferingGoal:c.vod?5:1,bufferBehind:c.vod?30:20,lowLatencyMode:false,segmentPrefetchLimit:2,allowMediaSourceRecoveries:true,retryParameters:{maxAttempts:4,baseDelay:500,backoffFactor:1.8,fuzzFactor:0.5}}});
    Android.onState('buffering','');
    await P.load(c.url);
    if(my!==SEQ)return;
    V.play().catch(function(){});
  }catch(e){ if(my===SEQ)Android.onState('error','Shaka '+(e&&e.code||e&&e.message||'')) }
}
var s=document.createElement('script');
s.src='https://cdn.jsdelivr.net/npm/shaka-player@4.16.51/dist/shaka-player.compiled.js';
s.onload=boot;s.onerror=function(){Android.onState('error','No se pudo cargar Shaka (sin internet)')};
document.head.appendChild(s);
</script></body></html>"""
    }
}
