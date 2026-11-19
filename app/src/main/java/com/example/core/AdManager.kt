package com.example.core

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val AD_TAG = "MimarStudyoAds"

/**
 * Official Google Mobile Ads (AdMob) test unit IDs.
 * Replace with your production AdMob unit IDs before Google Play release.
 */
object AdUnitIds {
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
}

/**
 * Initializes Google Mobile Ads SDK once on a background thread, strictly when needed
 * by Normal (Free) mode. Never runs on the main thread or for Pro users.
 */
object AdSdkInitializer {
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val readyDeferred = CompletableDeferred<Boolean>()

    suspend fun ensureInitialized(context: Context): Boolean {
        if (started.compareAndSet(false, true)) {
            val appContext = context.applicationContext
            initScope.launch {
                try {
                    MobileAds.initialize(appContext) {
                        readyDeferred.complete(true)
                    }
                } catch (t: Throwable) {
                    Log.w(AD_TAG, "MobileAds initialization failed: ${t.message}")
                    readyDeferred.complete(false)
                }
            }
        }
        return readyDeferred.await()
    }
}

/**
 * Manages preloading and non-intrusive display of Google AdMob Interstitial ads
 * strictly for Normal (Free) mode users. Never loads or shows ads for Pro users.
 */
class InterstitialAdController(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var interstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false
    private var lastShownElapsedMs: Long = 0L

    // Minimum 75 seconds between interstitial ads so the free UX stays respectful & Play Policy compliant
    private val cooldownMs: Long = 75_000L

    fun preloadIfFree(isPro: Boolean) {
        if (isPro) {
            clear()
            return
        }
        if (interstitialAd != null || isLoading) return

        isLoading = true
        scope.launch {
            val initialized = AdSdkInitializer.ensureInitialized(appContext)
            if (!initialized || !isLoading) {
                isLoading = false
                return@launch
            }
            try {
                val request = AdRequest.Builder().build()
                InterstitialAd.load(
                    appContext,
                    AdUnitIds.INTERSTITIAL_UNIT_ID,
                    request,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isLoading = false
                            Log.d(AD_TAG, "Interstitial ad loaded")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitialAd = null
                            isLoading = false
                            Log.w(AD_TAG, "Interstitial failed to load: ${error.message}")
                        }
                    }
                )
            } catch (t: Throwable) {
                interstitialAd = null
                isLoading = false
                Log.w(AD_TAG, "Interstitial load error: ${t.message}")
            }
        }
    }

    fun showIfEligible(activity: Activity?, isPro: Boolean, onDone: () -> Unit = {}) {
        if (isPro || activity == null) {
            onDone()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val ad = interstitialAd
        if (ad == null || (lastShownElapsedMs != 0L && now - lastShownElapsedMs < cooldownMs)) {
            if (ad == null) preloadIfFree(isPro = false)
            onDone()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownElapsedMs = SystemClock.elapsedRealtime()
                preloadIfFree(isPro = false)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                Log.w(AD_TAG, "Interstitial failed to show: ${adError.message}")
                preloadIfFree(isPro = false)
                onDone()
            }
        }
        ad.show(activity)
    }

    fun clear() {
        interstitialAd = null
        isLoading = false
    }
}

/**
 * Compact Google Mobile Ads Banner bar shown exclusively in Normal (Free) mode.
 * Includes a clean "REKLAM" disclosure label and a one-tap Pro upgrade shortcut.
 */
@Composable
fun AdMobBannerBar(
    priceLabel: String,
    onUpgradeToProClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember(context) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdUnitIds.BANNER_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoaded = false
                    Log.w(AD_TAG, "Banner failed to load: ${error.message}")
                }
            }
        }
    }

    LaunchedEffect(adView) {
        val initialized = AdSdkInitializer.ensureInitialized(context)
        if (initialized) {
            try {
                adView.loadAd(AdRequest.Builder().build())
            } catch (t: Throwable) {
                Log.w(AD_TAG, "Banner loadAd error: ${t.message}")
            }
        }
    }

    DisposableEffect(adView) {
        onDispose {
            try {
                adView.destroy()
            } catch (_: Exception) {
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("free_mode_ad_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "REKLAM • NORMAL MOD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onUpgradeToProClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("banner_remove_ads_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reklamları Kaldır • $priceLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!adLoaded) {
                    Text(
                        text = "Sponsorlu reklam alanı yükleniyor...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
