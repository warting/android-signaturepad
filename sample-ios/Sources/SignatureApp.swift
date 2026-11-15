import SwiftUI
import SignatureSample

@main
struct SignatureApp: App {
    var body: some Scene {
        WindowGroup {
            SignatureView().ignoresSafeArea(.keyboard)
        }
    }
}

struct SignatureView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
