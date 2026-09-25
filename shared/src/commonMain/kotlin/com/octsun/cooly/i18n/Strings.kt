package com.octsun.cooly.i18n

import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.RiskLevel
import com.octsun.cooly.domain.model.SpotLayer
import com.octsun.cooly.domain.model.SpotType

/**
 * All user-facing strings. Never hardcode text in UI.
 * English is the default; Korean is provided. Extend by adding another [Strings] impl.
 */
interface Strings {
    val appName: String
    val sloganLine: String

    // Status bar
    val feelsLike: String
    val airQuality: String
    val aqi: String
    fun aqiLevel(level: AqiLevel): String
    fun riskLevel(level: RiskLevel): String

    // Advisory banner
    fun heatAdvisory(feelsLike: String): String
    fun dangerHeatAdvisory(feelsLike: String): String
    val airAdvisory: String
    val airCautionAdvisory: String
    val safeAdvisory: String
    fun nearestRefuge(name: String, minutes: Int): String
    val nearestLabel: String

    // Rewarded-ad feedback
    val adUnavailable: String
    val adLoading: String

    // Data sources / privacy (attribution — OSM requires it)
    val dataSources: String
    val dataSourceMap: String
    val dataSourceWeather: String
    val privacyAssurance: String

    // Permission recovery
    val openSettings: String
    val permissionDeniedHelp: String
    val valueProp1: String
    val valueProp2: String
    val valueProp3: String

    // Layers / filters
    fun layer(layer: SpotLayer): String
    fun spotType(type: SpotType): String

    // Lists / details
    val nearbyCoolSpots: String
    val listView: String
    val mapView: String
    val directions: String
    val openingHours: String
    fun distanceAwayLabel(distance: String): String
    fun walkMinutes(min: Int): String
    fun meters(m: Int): String
    fun kilometers(km: Double): String

    // Rewarded unlock
    val unlockAllSpots: String
    val unlockAllSpotsDesc: String
    val watchAdToUnlock: String
    val unlocked: String
    fun showingNearest(n: Int): String
    fun showingCount(shown: Int, total: Int): String

    // Permissions / states
    val locationPermissionTitle: String
    val locationPermissionRationale: String
    val grantPermission: String
    val loading: String
    val retry: String
    val noSpotsFound: String
    val spotsLoadFailed: String
    val locationUnavailable: String
    val networkError: String

    // Settings
    val settings: String
    val temperatureUnit: String
    val celsius: String
    val fahrenheit: String
    val language: String
    val privacyPolicy: String
    val close: String
    val refresh: String

    // Favorites
    val favorites: String
    val favoritesOnly: String
    val addToFavorites: String
    val removeFromFavorites: String
    val noFavorites: String

    // Toilet rewarded trigger
    val findToilets: String
    val findToiletsDesc: String

    // Saved places (Cooly Plus feature 1)
    val savedPlaces: String
    val savedPlacesDesc: String
    val searchPlaceHint: String
    val addPlace: String
    val removePlace: String
    val noSavedPlaces: String
    val searchNoResults: String

    // Cooly Plus
    val coolyPlus: String
    val plusActive: String
    val plusTagline: String
    val plusFeatureUnlimitedPlaces: String
    val plusFeatureSupport: String
    fun placeLimitReached(limit: Int): String
    val upgrade: String
    val notNow: String
    val purchasesComingSoon: String
    val purchasesUnavailable: String
    val restorePurchases: String
    val purchaseSuccess: String
    val purchaseFailed: String
    val nothingToRestore: String
    fun priceLine(price: String): String
    val onboardingPlacesHint: String

    // Trust & data quality
    val partialUpdateFailed: String

    // Background danger alerts
    val dangerAlerts: String
    val dangerAlertsDesc: String
    val alertHeatTitle: String
    fun alertHeatBody(temp: String): String
    val alertAirTitle: String
    fun alertAirBody(aqi: Int, level: String): String
    val notificationsOffHint: String
    val openingHoursDisclaimer: String
    fun localizeOpeningHours(raw: String): String
    fun weeklyMaxLine(temp: String): String
    val languageFollowsSystem: String
    fun versionLine(version: String): String
}

object EnStrings : Strings {
    override val appName = "Cooly"
    override val sloganLine = "Find cool, stay safe."

    override val feelsLike = "Feels like"
    override val airQuality = "Air quality"
    override val aqi = "AQI"
    override fun aqiLevel(level: AqiLevel) = when (level) {
        AqiLevel.GOOD -> "Good"
        AqiLevel.MODERATE -> "Moderate"
        // Short labels so the status-bar metric never ellipsizes.
        AqiLevel.UNHEALTHY_SENSITIVE -> "Sensitive"
        AqiLevel.UNHEALTHY -> "Unhealthy"
        AqiLevel.VERY_UNHEALTHY -> "Very poor"
        AqiLevel.HAZARDOUS -> "Hazardous"
    }
    override fun riskLevel(level: RiskLevel) = when (level) {
        RiskLevel.SAFE -> "Safe"
        RiskLevel.CAUTION -> "Caution"
        RiskLevel.DANGER -> "Danger"
    }

    override fun heatAdvisory(feelsLike: String) =
        "Feels like $feelsLike — outdoor activity over 30 min is risky. Head to a cool indoor spot."
    override fun dangerHeatAdvisory(feelsLike: String) =
        "Dangerous heat — feels like $feelsLike. Get indoors now; the nearest cool place is below."
    override val airAdvisory = "Air quality is very unhealthy — find an indoor space."
    override val airCautionAdvisory = "Air quality is unhealthy — consider moving indoors."
    override val safeAdvisory = "Conditions are comfortable right now."
    override fun nearestRefuge(name: String, minutes: Int) =
        if (minutes <= 0) "Nearest cool spot: $name (right nearby)"
        else "Nearest cool spot: $name ($minutes min walk)"
    override val nearestLabel = "Nearest cool spot"
    override val adUnavailable = "Couldn't load the ad right now — check your connection and try again later."
    override val adLoading = "Loading ad…"

    override val dataSources = "Data sources"
    override val dataSourceMap = "Places: © OpenStreetMap contributors (community data, may be incomplete)"
    override val dataSourceWeather = "Weather & air quality: Open-Meteo"
    override val privacyAssurance = "Your location is used only to search nearby — never stored by us or shared for advertising."

    override val openSettings = "Open settings"
    override val permissionDeniedHelp =
        "Location is off. Enable it in Settings to find cool places near you."
    override val valueProp1 = "See local heat & air-quality risk"
    override val valueProp2 = "Find the nearest cool, safe places"
    override val valueProp3 = "Walking directions in one tap"

    override fun layer(layer: SpotLayer) = when (layer) {
        SpotLayer.COOL_INDOOR -> "Cool indoors"
        SpotLayer.WATER -> "Water"
        SpotLayer.SHADE -> "Shade & parks"
        SpotLayer.TOILET -> "Toilets"
    }
    override fun spotType(type: SpotType) = when (type) {
        SpotType.LIBRARY -> "Library"
        SpotType.MALL -> "Mall"
        SpotType.SUPERMARKET -> "Supermarket"
        SpotType.SUBWAY -> "Subway station"
        SpotType.CAFE -> "Cafe"
        SpotType.COOLING_CENTER -> "Cooling center"
        SpotType.PARK -> "Park"
        SpotType.WATER_FOUNTAIN -> "Drinking water"
        SpotType.TOILET -> "Public toilet"
        SpotType.OTHER -> "Place"
    }

    override val nearbyCoolSpots = "Nearby cool spots"
    override val listView = "List"
    override val mapView = "Map"
    override val directions = "Directions"
    override val openingHours = "Hours"
    override fun distanceAwayLabel(distance: String) = "$distance away"
    override fun walkMinutes(min: Int) = "$min min walk"
    override fun meters(m: Int) = "$m m"
    override fun kilometers(km: Double) = "${formatOneDecimal(km)} km"

    override val unlockAllSpots = "See all cool spots nearby"
    override val unlockAllSpotsDesc = "Watch a short ad to see every cool spot nearby."
    override val watchAdToUnlock = "See all spots (short ad)"
    override val unlocked = "All spots unlocked"
    override fun showingNearest(n: Int) = "Showing the $n nearest — watch an ad to see all"
    override fun showingCount(shown: Int, total: Int) =
        "Showing $shown of $total nearby — watch a short ad to see all"

    override val locationPermissionTitle = "Location needed"
    override val locationPermissionRationale =
        "Cooly uses your location to find the nearest cool, safe places around you."
    // Apple 5.1.1(iv): the pre-permission button must not direct users to allow —
    // use a neutral "Continue"; the OS dialog is where the actual choice happens.
    override val grantPermission = "Continue"
    override val loading = "Loading…"
    override val retry = "Retry"
    override val noSpotsFound = "No mapped places nearby yet — try refreshing in a different area."
    override val spotsLoadFailed = "Couldn't load nearby places. Check your connection and try again."
    override val locationUnavailable = "Couldn't get your location."
    override val networkError = "Couldn't load data. Check your connection."

    override val settings = "Settings"
    override val temperatureUnit = "Temperature unit"
    override val celsius = "Celsius (°C)"
    override val fahrenheit = "Fahrenheit (°F)"
    override val language = "Language"
    override val privacyPolicy = "Privacy policy"
    override val close = "Close"
    override val refresh = "Refresh"

    override val favorites = "Favorites"
    override val favoritesOnly = "Favorites"
    override val addToFavorites = "Save to favorites"
    override val removeFromFavorites = "Remove from favorites"
    override val noFavorites = "No favorites yet. Tap ☆ on a place to save it."

    override val findToilets = "Find nearby toilets"
    override val findToiletsDesc = "Watch a short ad to show public toilets nearby."

    override val savedPlaces = "Watched places"
    override val savedPlacesDesc = "Watch the heat & air of cities you care about — home, family, your next trip."
    override val searchPlaceHint = "Search a city or neighborhood"
    override val addPlace = "Add"
    override val removePlace = "Remove"
    override val noSavedPlaces = "No watched places yet. Search a city above to start."
    override val searchNoResults = "No places found for that search."

    override val coolyPlus = "Cooly Plus"
    override val plusActive = "Cooly Plus is active — thank you for supporting the map!"
    override val plusTagline = "Every safety feature stays free. Plus is for watching more of the world."
    override val plusFeatureUnlimitedPlaces = "Track unlimited places — the free plan includes 1"
    override val plusFeatureSupport = "Support a global heat-safety map"
    override fun placeLimitReached(limit: Int) =
        "Free plan watches $limit place. Get Cooly Plus for unlimited places."
    override val upgrade = "Get Cooly Plus"
    override val notNow = "Not now"
    override val purchasesComingSoon = "Purchases are being prepared, coming very soon."
    override val purchasesUnavailable = "Purchases aren't available right now. Please try again later."
    override val restorePurchases = "Restore purchases"
    override val purchaseSuccess = "Welcome to Cooly Plus! ❄️"
    override val purchaseFailed = "The purchase couldn't be completed. Please try again."
    override val nothingToRestore = "No previous purchases found for this account."
    override fun priceLine(price: String) = "Price: $price"
    override val onboardingPlacesHint = "Tip: tap 🌍 up top to keep an eye on the heat & air of other cities you care about."

    override val partialUpdateFailed = "Some data couldn't be refreshed — showing the latest available."

    override val dangerAlerts = "Danger alerts"
    override val dangerAlertsDesc =
        "Warns you when heat or air quality turns dangerous around your last location — even when the app is closed."
    override val alertHeatTitle = "Extreme heat warning"
    override fun alertHeatBody(temp: String) =
        "Feels like $temp near you. Open Cooly to find the nearest cool place."
    override val alertAirTitle = "Air quality warning"
    override fun alertAirBody(aqi: Int, level: String) =
        "AQI $aqi ($level) near you. Limit time outside and find clean indoor air."
    override val notificationsOffHint =
        "Notifications are off — allow them in Settings to receive danger alerts."
    override val openingHoursDisclaimer = "Hours come from community data and may be out of date."
    override fun localizeOpeningHours(raw: String) = raw
        .replace("PH", "holidays").replace("24/7", "24 hours").replace("off", "closed")
    override fun weeklyMaxLine(temp: String) = "Next 7 days: up to $temp"
    override val languageFollowsSystem = "Follows your system language"
    override fun versionLine(version: String) = "Cooly $version"
}

object KoStrings : Strings {
    override val appName = "Cooly"
    override val sloganLine = "가장 가까운 시원하고 안전한 곳."

    override val feelsLike = "체감온도"
    override val airQuality = "대기질"
    override val aqi = "AQI"
    override fun aqiLevel(level: AqiLevel) = when (level) {
        AqiLevel.GOOD -> "좋음"
        AqiLevel.MODERATE -> "보통"
        AqiLevel.UNHEALTHY_SENSITIVE -> "민감군 주의"
        AqiLevel.UNHEALTHY -> "나쁨"
        AqiLevel.VERY_UNHEALTHY -> "매우 나쁨"
        AqiLevel.HAZARDOUS -> "위험"
    }
    override fun riskLevel(level: RiskLevel) = when (level) {
        RiskLevel.SAFE -> "안전"
        RiskLevel.CAUTION -> "주의"
        RiskLevel.DANGER -> "위험"
    }

    override fun heatAdvisory(feelsLike: String) =
        "지금 체감 $feelsLike — 30분 이상 야외 활동 위험. 가까운 실내로 이동하세요."
    override fun dangerHeatAdvisory(feelsLike: String) =
        "위험한 폭염 — 체감 $feelsLike. 지금 실내로 이동하세요. 가장 가까운 시원한 곳은 아래에 있어요."
    override val airAdvisory = "미세먼지 매우 나쁨 — 실내 공간을 찾으세요."
    override val airCautionAdvisory = "대기질 나쁨 — 실내로 이동을 고려하세요."
    override val safeAdvisory = "현재는 쾌적한 상태예요."
    override fun nearestRefuge(name: String, minutes: Int) =
        if (minutes <= 0) "가장 가까운 시원한 곳: $name (바로 근처)"
        else "가장 가까운 시원한 곳: $name (도보 ${minutes}분)"
    override val nearestLabel = "가장 가까운 시원한 곳"
    override val adUnavailable = "지금은 광고를 불러올 수 없어요 — 연결을 확인하고 잠시 후 다시 시도하세요."
    override val adLoading = "광고 불러오는 중…"

    override val dataSources = "데이터 출처"
    override val dataSourceMap = "장소: © OpenStreetMap 기여자 (커뮤니티 데이터, 일부 누락 가능)"
    override val dataSourceWeather = "날씨·대기질: Open-Meteo"
    override val privacyAssurance = "위치 정보는 주변 검색에만 사용되며, 자체 저장하거나 광고 목적으로 공유하지 않습니다."

    override val openSettings = "설정 열기"
    override val permissionDeniedHelp =
        "위치 권한이 꺼져 있어요. 설정에서 켜면 주변 시원한 곳을 찾을 수 있어요."
    override val valueProp1 = "우리 동네 폭염·대기질 위험 확인"
    override val valueProp2 = "가장 가까운 시원하고 안전한 곳 찾기"
    override val valueProp3 = "한 번 탭으로 도보 길안내"

    override fun layer(layer: SpotLayer) = when (layer) {
        SpotLayer.COOL_INDOOR -> "시원한 실내"
        SpotLayer.WATER -> "식수"
        SpotLayer.SHADE -> "그늘·공원"
        SpotLayer.TOILET -> "화장실"
    }
    override fun spotType(type: SpotType) = when (type) {
        SpotType.LIBRARY -> "도서관"
        SpotType.MALL -> "쇼핑몰"
        SpotType.SUPERMARKET -> "대형마트"
        SpotType.SUBWAY -> "지하철역"
        SpotType.CAFE -> "카페"
        SpotType.COOLING_CENTER -> "쿨링센터"
        SpotType.PARK -> "공원"
        SpotType.WATER_FOUNTAIN -> "급수대"
        SpotType.TOILET -> "공중화장실"
        SpotType.OTHER -> "장소"
    }

    override val nearbyCoolSpots = "가까운 시원한 곳"
    override val listView = "목록"
    override val mapView = "지도"
    override val directions = "길안내"
    override val openingHours = "영업시간"
    override fun distanceAwayLabel(distance: String) = "$distance 거리"
    override fun walkMinutes(min: Int) = "도보 ${min}분"
    override fun meters(m: Int) = "${m}m"
    override fun kilometers(km: Double) = "${formatOneDecimal(km)}km"

    override val unlockAllSpots = "주변 시원한 곳 전체 보기"
    override val unlockAllSpotsDesc = "짧은 광고를 보고 주변 모든 시원한 곳을 확인하세요."
    override val watchAdToUnlock = "전체 보기 (짧은 광고)"
    override val unlocked = "주변 모든 시원한 곳이 열렸어요"
    override fun showingNearest(n: Int) = "가까운 ${n}개만 표시 중 — 짧은 광고로 전체 보기"
    override fun showingCount(shown: Int, total: Int) =
        "주변 ${total}곳 중 ${shown}곳 표시 중 — 짧은 광고로 전체 보기"

    override val locationPermissionTitle = "위치 권한 필요"
    override val locationPermissionRationale =
        "Cooly는 위치 정보를 사용해 가장 가까운 시원하고 안전한 곳을 찾아줍니다."
    override val grantPermission = "계속"
    override val loading = "불러오는 중…"
    override val retry = "다시 시도"
    override val noSpotsFound = "주변에 등록된 장소가 아직 없어요 — 다른 지역에서 새로고침해 보세요."
    override val spotsLoadFailed = "주변 장소를 불러오지 못했어요. 연결을 확인하고 다시 시도하세요."
    override val locationUnavailable = "위치를 가져오지 못했어요."
    override val networkError = "데이터를 불러오지 못했어요. 연결을 확인하세요."

    override val settings = "설정"
    override val temperatureUnit = "온도 단위"
    override val celsius = "섭씨 (°C)"
    override val fahrenheit = "화씨 (°F)"
    override val language = "언어"
    override val privacyPolicy = "개인정보 처리방침"
    override val close = "닫기"
    override val refresh = "새로고침"

    override val favorites = "즐겨찾기"
    override val favoritesOnly = "즐겨찾기"
    override val addToFavorites = "즐겨찾기에 저장"
    override val removeFromFavorites = "즐겨찾기에서 제거"
    override val noFavorites = "저장된 즐겨찾기가 없어요. 장소에서 ☆를 눌러 저장하세요."

    override val findToilets = "주변 화장실 찾기"
    override val findToiletsDesc = "짧은 광고를 보고 주변 공중화장실을 표시하세요."

    override val savedPlaces = "관심 지역"
    override val savedPlacesDesc = "우리 집, 가족, 다음 여행지 — 신경 쓰이는 도시의 더위와 공기를 지켜보세요."
    override val searchPlaceHint = "도시나 동네 검색"
    override val addPlace = "추가"
    override val removePlace = "삭제"
    override val noSavedPlaces = "아직 관심 지역이 없어요. 위에서 도시를 검색해 추가하세요."
    override val searchNoResults = "검색 결과가 없어요."

    override val coolyPlus = "Cooly Plus"
    override val plusActive = "Cooly Plus 사용 중 — 지도를 함께 만들어 주셔서 감사해요!"
    override val plusTagline = "안전 기능은 전부 무료. Plus는 더 넓은 세상을 지켜보는 사람을 위한 거예요."
    override val plusFeatureUnlimitedPlaces = "관심 지역 무제한 — 무료 플랜은 1곳 포함"
    override val plusFeatureSupport = "글로벌 폭염 안전 지도를 함께 만들기"
    override fun placeLimitReached(limit: Int) =
        "무료 플랜은 관심 지역 ${limit}곳까지예요. Cooly Plus로 무제한 추가하세요."
    override val upgrade = "Cooly Plus 시작하기"
    override val notNow = "다음에"
    override val purchasesComingSoon = "결제 기능을 준비 중이에요. 곧 열립니다."
    override val purchasesUnavailable = "지금은 결제를 사용할 수 없어요. 잠시 후 다시 시도해 주세요."
    override val restorePurchases = "구매 복원"
    override val purchaseSuccess = "Cooly Plus에 오신 걸 환영해요! ❄️"
    override val purchaseFailed = "결제를 완료하지 못했어요. 다시 시도해 주세요."
    override val nothingToRestore = "이 계정에서 복원할 구매 내역이 없어요."
    override fun priceLine(price: String) = "가격: $price"
    override val onboardingPlacesHint = "팁: 상단의 🌍를 누르면 다른 도시의 더위·공기도 지켜볼 수 있어요."

    override val partialUpdateFailed = "일부 정보를 새로 불러오지 못했어요 — 마지막 정보를 표시 중이에요."

    override val dangerAlerts = "위험 알림"
    override val dangerAlertsDesc =
        "마지막 위치 주변의 더위·대기질이 위험 수준이 되면 앱이 꺼져 있어도 알려드려요."
    override val alertHeatTitle = "폭염 위험 경보"
    override fun alertHeatBody(temp: String) =
        "지금 주변 체감온도 $temp — Cooly에서 가장 가까운 시원한 곳을 확인하세요."
    override val alertAirTitle = "대기질 위험 경보"
    override fun alertAirBody(aqi: Int, level: String) =
        "지금 주변 AQI $aqi ($level) — 외출을 줄이고 실내로 이동하세요."
    override val notificationsOffHint =
        "알림이 꺼져 있어요 — 위험 알림을 받으려면 설정에서 알림을 허용하세요."
    override val openingHoursDisclaimer = "영업시간은 커뮤니티 데이터라 실제와 다를 수 있어요."
    override fun localizeOpeningHours(raw: String) = raw
        .replace("Mo", "월").replace("Tu", "화").replace("We", "수").replace("Th", "목")
        .replace("Fr", "금").replace("Sa", "토").replace("Su", "일")
        .replace("PH", "공휴일").replace("24/7", "24시간").replace("off", "휴무")
    override fun weeklyMaxLine(temp: String) = "앞으로 7일 최고 $temp"
    override val languageFollowsSystem = "시스템 언어 설정을 따라요"
    override fun versionLine(version: String) = "Cooly $version"
}

/** Pick the string bundle for a 2-letter language code (e.g. "ko"). Falls back to English. */
fun stringsFor(languageCode: String): Strings =
    when (languageCode.lowercase()) {
        "ko" -> KoStrings
        else -> EnStrings
    }

internal fun formatOneDecimal(value: Double): String {
    // Round instead of truncate: 1.99 must render "2.0", not "1.9".
    val scaled = kotlin.math.round(value * 10).toLong()
    val whole = scaled / 10
    val frac = (if (scaled < 0) -scaled else scaled) % 10
    return "$whole.$frac"
}
