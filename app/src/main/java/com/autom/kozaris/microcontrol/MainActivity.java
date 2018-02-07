package com.autom.kozaris.microcontrol;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import com.autom.kozaris.microcontrol.Fragments.InputsFragment;
import com.autom.kozaris.microcontrol.Fragments.OutputsFragment;
import com.autom.kozaris.microcontrol.MicroModule.IConstants;
import com.autom.kozaris.microcontrol.Receivers.MqttClientServiceReceiver;

import static com.autom.kozaris.microcontrol.ConstantStrings.SETTINGS._ACTIVITY;

public class MainActivity extends AppCompatActivity implements MqttClientServiceReceiver.BroadcastListener {

    MqttClientServiceReceiver mqttServiceReceiver;
    //Καρτέλα που περιέχει την λίστα Εξόδων
    OutputsFragment outputsfragment;
    //Καρτέλα που περιέχει την λίστα Εισόδων
    InputsFragment inputsFragment;

    //region Activity Basic Overrides (Start, Stop, Create ,Destroy)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        outputsfragment = new OutputsFragment();
        inputsFragment = new InputsFragment();
        //region Αρχικοποίηση Toolbar and Tabs
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mikro Control");
            getSupportActionBar().setIcon(R.mipmap.ic_mikro_cloud);
        }
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        //endregion

        /*
        Αρχικοποίηση του Broadcast Receiver που λαμβάνει γεγονότα για την
        δραστηριότητα MainActivity.
        */
        RegisterReceivers();
        /*
        Αλλαγή της μεταβλητής Query_Active στις ρυθμίσεις προγράμματος,
        η μεταβλητή σηματοδοτεί ότι η δραστηριότητα MainActivity
        βρίσκετε σε λειτουργία.
        */
        SharedPreferences sp = getSharedPreferences(_ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, true)
                .apply();
        //Αρχικοποιήση κουμπιού συγχρονισμού "fab"
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                Όταν το κουμπί πατηθεί, τότε θα σταλεί κατάλληλο μήνυμα στο θέμα συγχρονισμού μέσω του Service "MicroMqttService"
                ώστε να διαβαστεί από τους μικροελεγκτές και να τους ενημερώσει σε ποιο "androidID"
                πρέπει να στείλουν τα στοιχεία τους
                */
                @SuppressLint("HardwareIds") String androidID = android.provider.Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                Intent synchronizeIntent = new Intent().setAction(ConstantStrings.ACTIONS._PUBLISH)
                        .putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, IConstants.Topics.SYNCHRONIZE)
                        .putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, androidID);
                sendBroadcast(synchronizeIntent);
                Snackbar.make(view, "Refreshing Connections", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences(_ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, true);
        ed.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sp = getSharedPreferences(_ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, false)
                .apply();
        stopService(new Intent(getApplicationContext(), MicroMqttService.class));
        if (mqttServiceReceiver != null) {
            unregisterReceiver(mqttServiceReceiver);
        }

    }

    @Override
    public void onBackPressed() {
        /* Ειδοποίησε με κατάλληλο μήνυμα όταν ο χρήστης προσπαθήσει να πατήσει το
        πλήκτρο "Πίσω" στην συσκευή του, ώστε να γίνει έξοδος από την δραστηριότητα MainActivity
         */
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Leaving Application");
        alertDialog.setMessage("You will be disconected");
        alertDialog.setPositiveButton("OK",

                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();

                    }
                });
        alertDialog.setNegativeButton("Ακυρο", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        alertDialog.show();

    }


    //endregion

    /*
        Αρχικοποίηση του Broadcast Receiver που λαμβάνει γεγονότα για την
        δραστηριοτητα MainActivity.
    */
    private void RegisterReceivers() {
        //Τα γεγονότα που ενδιαφέρουν την δραστηριότητα MainActivity ελέγχονται από τον MqttClientServiceReceiver
        // η εγγραφή στο φίλτρο _RECEIVED_MESSAGE θα μας δώσει μονο το γεγονός "Εισερχόμενο μήνυμα MQTT".
        registerReceiver(new MqttClientServiceReceiver(this), new IntentFilter(ConstantStrings.ACTIONS._RECEIVED_MESSAGE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_refresh || super.onOptionsItemSelected(item);

    }
    //region LISTENERS FOR MQTT EVENTS

    /**
     * Η μέθοδος onModuleCreateNew καλείτε όταν ενα εισερχόμενο μήνυμα MQTT  στο Service "ΜicroMqttServie"
     * αναγνωριστεί ως μήνυμα για την δημιουργία νέας συσκευής
     */
    @Override
    public void onModuleCreateNew(MicroModule newModule) {
        switch (newModule.getModuleType()) {
            case OUTPUT:
                //Αν η συσκευή "newModule" είναι έξοδος, τότε θα γίνει
                // εγγραφή στα θέματα LastWil και Data της συσκευής
                if (FindModulebyId(newModule.getModuleID(), outputsfragment.OutputModuleList) != null) {
                    return;
                }
                SubscribeBroadcast(newModule.getDataTopic());
                SubscribeBroadcast(newModule.getWillTopic());
                //Η συσκευή τοποθετείτε στην λίστα "OutputModuleList" για να διαβαστεί από
                //την καρτέλα εξόδων.
                outputsfragment.OutputModuleList.add(newModule);
                outputsfragment.recyclerAdapter.notifyDataSetChanged();
                break;
            case INPUT:
                if (FindModulebyId(newModule.getModuleID(), inputsFragment.InputModuleList) != null) {
                    return;
                }
                //Αν η συσκευή "newModule" είναι έξοδος, τότε θα γίνει εγγραφή στο θέμα LastWill της συσκευής
                SubscribeBroadcast(newModule.getWillTopic());
                //Η συσκευή τοποθετείτε στην λίστα "inputModuleList" για να διαβαστεί από
                //την καρτέλα εισόδων.
                inputsFragment.InputModuleList.add(newModule);
                inputsFragment.recyclerAdapter.notifyDataSetChanged();
                break;
            default:
        }
    }

    /**
     * Η μέθοδος SubscribeBroadcast δέχεται ενα θέμα σε μορφή String
     * και αναλαμβάνει να το προωθήσει μέσω ενός Intent  στο MicroMqttService
     */
    private void SubscribeBroadcast(String Topic) {
        Intent subscribe = new Intent()
                .setAction(ConstantStrings.ACTIONS._SUBSCRIBE)
                .putExtra(ConstantStrings.EXTRAS._SUB_TOPIC, Topic);
        sendBroadcast(subscribe);
    }

    /**
     * Η μέθοδος onModuleOutputData καλείτε όταν ενα εισερχόμενο μήνυμα MQTT  στο Service "ΜicroMqttServie"
     * αναγνωριστεί ως μήνυμα δεδομένων μιας υπάρχουσας συσκευής.
     */
    @Override
    public void onModuleOutputData(int moduleId, String data) {
        for (MicroModule e : outputsfragment.OutputModuleList) {
            if (e.getModuleID() == moduleId) {
                e.setData(data);
            }
        }
        outputsfragment.recyclerAdapter.notifyDataSetChanged();
    }

    /**
     * Η μέθοδος onModuleDisconected καλείτε όταν ενα εισερχόμενο μήνυμα MQTT  στο Service "ΜicroMqttServie"
     * αναγνωριστεί ως αποσύνδεσης μιας συσκευής από τον μεσίτη. Αναλαμβάνει να διαγράψει τις συσκευές
     * από την κατάλληλη λίστα InputModuleList η OutputModuleList
     */
    @Override
    public void onModuleDisconected(int moduleId, String Type) {
        MicroModule deletedModule;
        switch (Type) {
            case IConstants.ModuleTypes.INPUT:
                deletedModule = FindModulebyId(moduleId, inputsFragment.InputModuleList);
                if (deletedModule == null) {
                    return;
                }
                inputsFragment.InputModuleList.remove(deletedModule);
                inputsFragment.recyclerAdapter.notifyDataSetChanged();
                return;
            case IConstants.ModuleTypes.OUTPUT:
                deletedModule = FindModulebyId(moduleId, outputsfragment.OutputModuleList);
                if (deletedModule == null) {
                    return;
                }
                outputsfragment.OutputModuleList.remove(deletedModule);
                outputsfragment.recyclerAdapter.notifyDataSetChanged();
        }

    }

    /**
     * Η μέθοδος FindModulebyId δέχεται ενα αναγνωριστικό συσκευής "id" και μία λίστα
     * συσκευών "list" και αναλαμβάνει την εύρεση της συγκεκριμένης συσκευής απο την λίστα
     */
    public MicroModule FindModulebyId(int id, List<MicroModule> list) {
        for (MicroModule e : list) {
            if (e.getModuleID() == id) {
                return e;
            }
        }
        return null;
    }


    /**
     * SectionsPagerAdapter
     * Εσωτερική κλάση της δραστηριότητας {@link MainActivity} που αναλαμβάνει την αρχικοποίηση των καρτελών εισόδου και εξόδου
     * Συνδέει τις 2 αυτές καρτέλες με τα παράθυρα {@link InputsFragment} και {@link OutputsFragment}
     * Δίνει επικεφαλίδες στις καρτέλες
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return inputsFragment;
                case 1:
                    return outputsfragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }


        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Εισοδοι";
                case 1:
                    return "Εξοδοι";
            }
            return null;
        }

        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}