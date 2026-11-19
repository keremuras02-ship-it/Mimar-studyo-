package com.example.data.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class DesignProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val spaceType: String = SpaceType.BOTH.name,
    val style: String = "Modern Akdeniz",
    val roomCategory: String = "Dış Cephe & Salon",
    val areaSqm: Double = 160.0,
    val promptSummary: String = "",
    val renderSettings: String = "16:9 • 2K • Altın Saat",
    val architecturalModelType: String = Model3DPreset.VILLA.name,
    val thumbnailBase64: String? = null,
    val notes: String? = null,
    val estimatedTotalTry: Double = 0.0,
    val phase: String = ProjectPhase.KONSEPT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MaterialItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val name: String = "",
    val category: String = MaterialCategory.YAPI.name,
    val specification: String = "",
    val quantity: Double = 1.0,
    val unit: String = "m²",
    val unitPriceTry: Double = 0.0,
    val purchased: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val lineTotalTry: Double get() = quantity * unitPriceTry
}

@Keep
@IgnoreExtraProperties
data class ChatMessageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "USER",
    val text: String = "",
    val thinkingUsed: Boolean = false,
    val attachedPrompt: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class SpaceType(val labelTr: String, val promptHint: String) {
    INTERIOR("İç Mekan", "interior architectural space"),
    EXTERIOR("Dış Mekan", "exterior building facade and landscape"),
    BOTH("İç & Dış", "seamless indoor-outdoor residential architecture");

    companion object {
        fun fromRaw(raw: String): SpaceType =
            entries.firstOrNull { it.name == raw } ?: BOTH
    }
}

enum class ProjectPhase(val labelTr: String) {
    KONSEPT("Konsept"),
    UYGULAMA("Uygulama"),
    TESLIM("Teslim Edildi");

    companion object {
        fun fromRaw(raw: String): ProjectPhase =
            entries.firstOrNull { it.name == raw } ?: KONSEPT
    }
}

enum class MaterialCategory(val labelTr: String) {
    YAPI("Kaba Yapı"),
    ZEMIN("Zemin & Duvar"),
    CEPHE("Dış Cephe"),
    AYDINLATMA("Aydınlatma"),
    MOBILYA("Mobilya & Doğrama"),
    PEYZAJ("Peyzaj & Havuz");

    companion object {
        fun fromRaw(raw: String): MaterialCategory =
            entries.firstOrNull { it.name == raw.uppercase() } ?: YAPI
    }
}

enum class RenderAspectRatio(val label: String, val ratioValue: Float, val apiValue: String) {
    RATIO_1_1("1:1", 1f, "1:1"),
    RATIO_2_3("2:3", 2f / 3f, "2:3"),
    RATIO_3_2("3:2", 3f / 2f, "3:2"),
    RATIO_3_4("3:4", 3f / 4f, "3:4"),
    RATIO_4_3("4:3", 4f / 3f, "4:3"),
    RATIO_9_16("9:16", 9f / 16f, "9:16"),
    RATIO_16_9("16:9", 16f / 9f, "16:9"),
    RATIO_21_9("21:9", 21f / 9f, "21:9")
}

enum class RenderResolution(
    val label: String,
    val subtitleTr: String,
    val modelName: String
) {
    SIZE_1K("1K", "Hızlı • Nano Banana 2.1", "gemini-nano-banana-2.1"),
    SIZE_2K("2K", "Yüksek Detay • Pro Render", "gemini-3-pro-image"),
    SIZE_4K("4K", "Stüdyo Ultra • Pro Render", "gemini-3-pro-image")
}

enum class ArchStylePreset(val labelTr: String, val promptDesc: String) {
    MEDITERRANEAN("Modern Akdeniz", "modern Mediterranean villa architecture with honed travertine stone, cedar wood, warm minimalist elegance"),
    JAPANDI("Japandi Minimal", "serene Japandi architectural minimalism, warm oak slats, limewash plaster, organic natural textures"),
    LUXURY_CONTEMPORARY("Lüks Çağdaş", "ultra-luxury contemporary architecture, cantilevered slabs, frameless floor-to-ceiling glass, bespoke brass accents"),
    SCANDINAVIAN("İskandinav", "warm Nordic Scandinavian residential design, pale ash wood, soft natural daylight, cozy wool and stone"),
    NEOCLASSICAL("Neo-Klasik", "refined contemporary neoclassical architecture, subtle wall moldings, Calacatta marble, timeless proportions"),
    INDUSTRIAL_LOFT("Endüstriyel Loft", "architectural industrial loft, board-formed exposed concrete, blackened steel beams, warm leather and brick"),
    BIOPHILIC("Biyofilik Eko", "biophilic sustainable eco-architecture, integrated courtyard greenery, green roof, timber louvers"),
    BRUTALIST("Brütalist Beton", "sculptural architectural brutalism, monolithic warm architectural concrete, dramatic shadow geometry")
}

enum class LightingPreset(val labelTr: String, val promptDesc: String) {
    GOLDEN_HOUR("Altın Saat", "warm late afternoon golden hour sunlight casting long dramatic architectural shadows"),
    BLUE_HOUR("Mavi Saat", "atmospheric twilight blue hour sky with glowing warm 2700K architectural interior and facade lighting"),
    MORNING_LIGHT("Sabah Işığı", "crisp soft natural morning sunlight streaming through floor-to-ceiling windows"),
    OVERCAST_STUDIO("Stüdyo Gün Işığı", "balanced diffused overcast daylight for true-to-life architectural material color fidelity"),
    DRAMATIC_NIGHT("Dramatik Gece", "nighttime architectural lighting with recessed linear LED coves, wall washers, and pool reflections")
}

enum class CameraLensPreset(val labelTr: String, val promptDesc: String) {
    WIDE_24("24mm Geniş", "24mm architectural tilt-shift wide-angle lens with perfectly vertical lines"),
    ARCH_35("35mm Mimari", "35mm editorial architectural photography eye-level perspective"),
    DETAIL_50("50mm Detay", "50mm medium architectural detail lens with subtle depth of field highlighting material textures"),
    DRONE_AERIAL("Drone Kuş Bakışı", "elevated three-quarter architectural drone aerial view showing full massing and landscaping")
}

enum class MaterialPalettePreset(val labelTr: String, val promptDesc: String) {
    TRAVERTINE_WALNUT("Traverten & Ceviz", "honed Denizli travertine stone, warm smoked walnut millwork, brushed bronze hardware"),
    CONCRETE_STEEL("Beton & Çelik", "architectural exposed concrete, matte black steel framing, warm oak flooring"),
    MARBLE_BRASS("Mermer & Pirinç", "bookmatched white marble slabs, fluted oak cabinetry, satin unlacquered brass fixtures"),
    CEDAR_LIMESTONE("Sedir & Doğal Taş", "thermal cedar wood cladding, hand-cut dry-stack natural stone, honed limestone pavers")
}

enum class Model3DPreset(val labelTr: String, val subtitleTr: String) {
    VILLA("Modern Havuzlu Villa", "2 Kat • 280 m² • Konsol Çatı & Sonsuzluk Havuzu"),
    LOFT("Çift Katlı Loft", "Asma Kat • 165 m² • Çelik & Cam Galeri"),
    COURTYARD("Avlulu Taş Konak", "U-Plan • 320 m² • İç Avlu & Ahşap Pergola"),
    DUPLEX("Minimalist Teras Ev", "3 Kot • 240 m² • Panoramik Teras & Cam Cephe");

    companion object {
        fun fromRaw(raw: String): Model3DPreset =
            entries.firstOrNull { it.name == raw } ?: VILLA
    }
}

enum class VirtualTourAspectRatio(val labelTr: String, val ratioValue: Float, val apiAspect: RenderAspectRatio) {
    LANDSCAPE_16_9("16:9 Yatay", 16f / 9f, RenderAspectRatio.RATIO_16_9),
    PORTRAIT_9_16("9:16 Dikey", 9f / 16f, RenderAspectRatio.RATIO_9_16)
}

enum class VirtualTourCameraMove(val labelTr: String, val instructionEn: String) {
    CINEMATIC_PAN("Sinematik Pan", "smooth horizontal wide architectural reveal showing surrounding spaces"),
    DOLLY_IN("Yavaş Yaklaşma", "forward architectural walkthrough stepping deeper into the focal area"),
    ORBIT_360("Mimari Yörünge", "three-quarter orbiting angles revealing facade depth, side perspective, and interior glow"),
    DAY_TO_NIGHT("Gündüz → Gece", "progressive atmospheric time-lapse from bright daylight to golden sunset to glowing twilight")
}

data class ShowcaseTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val spaceType: SpaceType,
    val style: ArchStylePreset,
    val roomCategory: String,
    val model3D: Model3DPreset,
    @DrawableRes val drawableRes: Int,
    val samplePrompt: String
)

val ShowcaseTemplates = listOf(
    ShowcaseTemplate(
        id = "tpl_villa",
        title = "Ege Konsol Villa",
        subtitle = "Dış Mekan • Traverten & Sedir • Havuz",
        spaceType = SpaceType.EXTERIOR,
        style = ArchStylePreset.MEDITERRANEAN,
        roomCategory = "Dış Cephe & Havuz",
        model3D = Model3DPreset.VILLA,
        drawableRes = R.drawable.img_hero_exterior_1795089517276,
        samplePrompt = "Geniş konsol çatı saçaklı, honlu traverten taş cepheli, sedir ahşap tavan kaplamalı ve sonsuzluk havuzlu iki katlı çağdaş Akdeniz villası"
    ),
    ShowcaseTemplate(
        id = "tpl_living",
        title = "Çift Katlı Galeri Salon",
        subtitle = "İç Mekan • Monolitik Şömine • Ceviz",
        spaceType = SpaceType.INTERIOR,
        style = ArchStylePreset.LUXURY_CONTEMPORARY,
        roomCategory = "Salon & Galeri",
        model3D = Model3DPreset.LOFT,
        drawableRes = R.drawable.img_hero_interior_1795089541318,
        samplePrompt = "Çift kat yüksekliğinde tavanlı, monolitik traverten şömine duvarlı, ceviz çıtalı duvar panelli ve iç bahçeye açılan lüks minimalist salon"
    ),
    ShowcaseTemplate(
        id = "tpl_kitchen",
        title = "Fütüristik Meşe Mutfak",
        subtitle = "İç Mekan • Kuvarsit Ada • Gizli LED",
        spaceType = SpaceType.INTERIOR,
        style = ArchStylePreset.JAPANDI,
        roomCategory = "Mutfak",
        model3D = Model3DPreset.DUPLEX,
        drawableRes = R.drawable.img_showcase_kitchen_1795089516866,
        samplePrompt = "Oluklu doğal meşe dolaplı, şelale kuvarsit mermer adalı, mat bronz armatürlü ve gizli doğrusal tavan aydınlatmalı modern mimari mutfak"
    ),
    ShowcaseTemplate(
        id = "tpl_courtyard",
        title = "Pergolalı Ateş Çukuru Avlu",
        subtitle = "Dış Mekan • Ahşap Pergola • Peyzaj",
        spaceType = SpaceType.EXTERIOR,
        style = ArchStylePreset.BIOPHILIC,
        roomCategory = "Avlu & Teras",
        model3D = Model3DPreset.COURTYARD,
        drawableRes = R.drawable.img_showcase_courtyard_1795089515882,
        samplePrompt = "Doğal ahşap lamel pergolalı, gömme taş oturma alanlı ateş çukuru, zeytin ağaçları ve doğal kireçtaşı yürüyüş yollu Akdeniz avlusu"
    )
)

data class VirtualTourFrame(
    val title: String,
    val narrationTr: String,
    val bitmap: Bitmap
)

data class DraftMaterialInput(
    val name: String,
    val category: MaterialCategory,
    val specification: String,
    val quantity: Double,
    val unit: String,
    val unitPriceTry: Double
)
