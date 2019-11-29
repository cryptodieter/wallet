package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsModule
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsWrapped
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinItemsAdapter
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.ui.dialogs.AlertDialogFragment
import kotlinx.android.synthetic.main.activity_create_wallet.*

class RestoreCoinsActivity : BaseActivity(), CoinItemsAdapter.Listener {
    private lateinit var presenter: RestoreCoinsPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter
    private var buttonEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_coins)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val presentationMode: PresentationMode = intent.getParcelableExtra(ModuleField.PRESENTATION_MODE)
                ?: PresentationMode.Initial
        val predefinedAccountTypeString = intent.extras?.getString(ModuleField.PREDEFINED_ACCOUNT_TYPE)
        val predefinedAccountType = PredefinedAccountType.fromString(predefinedAccountTypeString)

        predefinedAccountType?.let {
            presenter = ViewModelProvider(this, RestoreCoinsModule.Factory(presentationMode, it)).get(RestoreCoinsPresenter::class.java)
        } ?: kotlin.run {
            //predefinedAccountType must not be null
            finish()
        }

        observeView(presenter.view as RestoreCoinsView)
        observeRouter(presenter.router as RestoreCoinsRouter)

        coinItemsAdapter = CoinItemsAdapter(this)
        coins.adapter = coinItemsAdapter

        presenter.onLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuRestore)?.apply {
            isEnabled = buttonEnabled
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuRestore -> {
                presenter.onProceedButtonClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ModuleCode.COIN_SETTINGS -> {
                if (resultCode == Activity.RESULT_CANCELED) {
                    presenter.onCancelSelectingCoinSettings()
                } else if (resultCode == Activity.RESULT_OK && data != null) {
                    val coin = data.getParcelableExtra<Coin>(ModuleField.COIN) ?: return
                    val coinSettings = data.getParcelableExtra<CoinSettingsWrapped>(ModuleField.COIN_SETTINGS)
                            ?: return

                    presenter.onSelectCoinSettings(coinSettings.settings, coin)
                }
            }
            ModuleCode.RESTORE -> {
                val accountType = data?.getParcelableExtra<AccountType>(ModuleField.ACCOUNT_TYPE)
                        ?: return
                presenter.didRestore(accountType)
            }
        }
    }

    //ManageWalletsAdapter.Listener

    override fun enable(item: CoinToggleViewItem) {
        presenter.onEnable(item)
    }

    override fun disable(item: CoinToggleViewItem) {
        presenter.onDisable(item)
    }

    private fun observeView(view: RestoreCoinsView) {
        view.setTitle.observe(this, Observer { predefinedAccountType ->
            val typeTitle = getString(predefinedAccountType.title)
            collapsingToolbar.title = getString(R.string.Wallet, typeTitle)
        })

        view.coinsLiveData.observe(this, Observer {(featuredItems, coinItems) ->
            coinItemsAdapter.featuredCoins = featuredItems
            coinItemsAdapter.coins = coinItems
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.proceedButtonEnabled.observe(this, Observer { enabled ->
            buttonEnabled = enabled
            invalidateOptionsMenu()
        })

        view.errorLiveEvent.observe(this, Observer {
            if (it is EosUnsupportedException) {
                AlertDialogFragment.newInstance(
                        R.string.Alert_TitleWarning,
                        R.string.ManageCoins_EOSAlert_CreateButton,
                        R.string.Alert_Ok
                ).show(supportFragmentManager, "alert_dialog")
            }
        })
    }

    private fun observeRouter(router: RestoreCoinsRouter) {
        router.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this)
            setResult(RESULT_OK, Intent())
            finish()
        })
        router.showCoinSettings.observe(this, Observer { (coin, coinSettings) ->
            CoinSettingsModule.startForResult(coin, coinSettings, this)
        })
        router.showRestoreEvent.observe(this, Observer { predefinedAccountType ->
            RestoreModule.startForResult(this, predefinedAccountType)
        })
        router.close.observe(this, Observer {
            finish()
        })
    }
}