import UIKit
import Shared

// The Google Maps SDK is optional: when the SPM package isn't resolved yet, this file
// still compiles and the shared UI falls back to the self-drawn CanvasMap.
#if canImport(GoogleMaps)
import GoogleMaps

/// One GMSMapView wrapped behind the shared `NativeMapController` protocol.
final class GoogleMapController: NSObject, NativeMapController, GMSMapViewDelegate {
    private let mapView: GMSMapView
    private let onMarkerTap: (String) -> Void
    private var markers: [GMSMarker] = []

    init(onMarkerTap: @escaping (String) -> Void) {
        let options = GMSMapViewOptions()
        options.camera = GMSCameraPosition(latitude: 0, longitude: 0, zoom: 2)
        self.mapView = GMSMapView(options: options)
        self.onMarkerTap = onMarkerTap
        super.init()
        mapView.delegate = self
        mapView.settings.myLocationButton = true
        // Parity with Android MapUiSettings(zoomControlsEnabled = false): iOS has no zoom
        // controls by default, nothing to disable.
    }

    var view: UIView { mapView }

    func setCamera(lat: Double, lng: Double, zoom: Double, animated: Bool) {
        let camera = GMSCameraPosition(latitude: lat, longitude: lng, zoom: Float(zoom))
        if animated {
            mapView.animate(to: camera)
        } else {
            mapView.camera = camera
        }
    }

    func setMarkers(markers newMarkers: [NativeMapMarker]) {
        markers.forEach { $0.map = nil }
        markers = newMarkers.map { m in
            let marker = GMSMarker(
                position: CLLocationCoordinate2D(latitude: m.lat, longitude: m.lng)
            )
            marker.title = m.title
            marker.snippet = m.snippet
            marker.userData = m.id
            marker.map = mapView
            return marker
        }
    }

    func setMyLocationEnabled(enabled: Bool) {
        mapView.isMyLocationEnabled = enabled
    }

    func setMapStyleJson(json: String?) {
        if let json {
            mapView.mapStyle = try? GMSMapStyle(jsonString: json)
        } else {
            mapView.mapStyle = nil
        }
    }

    func dispose() {
        mapView.delegate = nil
        markers.forEach { $0.map = nil }
        markers = []
    }

    // Consume the tap: the Compose detail sheet is the marker UI (parity with Android).
    func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
        if let id = marker.userData as? String {
            onMarkerTap(id)
        }
        return true
    }
}

final class GoogleMapFactory: NativeMapFactory {
    func create(onMarkerTap: @escaping (String) -> Void) -> NativeMapController {
        GoogleMapController(onMarkerTap: onMarkerTap)
    }
}

enum GoogleMapsSetup {
    /// Call once at app start, before the Compose UI is created.
    static func activateIfConfigured() {
        let key = (Bundle.main.object(forInfoDictionaryKey: "GMAPS_API_KEY") as? String) ?? ""
        guard !key.isEmpty else {
            NSLog("Cooly: GMAPS_API_KEY is empty — using CanvasMap fallback")
            return
        }
        GMSServices.provideAPIKey(key)
        NativeMapBridge.shared.factory = GoogleMapFactory()
    }
}

#else

enum GoogleMapsSetup {
    /// GoogleMaps SPM package not resolved — the shared UI falls back to CanvasMap.
    static func activateIfConfigured() {
        NSLog("Cooly: GoogleMaps SDK not bundled — using CanvasMap fallback")
    }
}

#endif
