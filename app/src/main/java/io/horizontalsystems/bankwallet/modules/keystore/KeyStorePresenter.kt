package io.horizontalsystems.bankwallet.modules.keystore

class KeyStorePresenter(private val interactor: KeyStoreModule.IInteractor,
                        private val router: KeyStoreModule.IRouter,
                        private val mode: KeyStoreModule.ModeType) : KeyStoreModule.IViewDelegate, KeyStoreModule.IInteractorDelegate {

    var view: KeyStoreModule.IView? = null

    override fun viewDidLoad() {
        interactor.resetApp()

        when (mode) {
            KeyStoreModule.ModeType.NoSystemLock -> view?.showNoSystemLockWarning()
            KeyStoreModule.ModeType.InvalidKey -> view?.showInvalidKeyWarning()
        }
    }

    override fun onCloseInvalidKeyWarning() {
        interactor.removeKey()
        router.openLaunchModule()
    }

    override fun onCloseNoSystemLockWarning() {
        router.closeApplication()
    }

}