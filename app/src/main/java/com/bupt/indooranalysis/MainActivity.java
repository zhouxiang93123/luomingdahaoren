package com.bupt.indooranalysis;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bupt.indoorPosition.bean.InspectedBeacon;
import com.bupt.indoorPosition.bean.Inspector;
import com.bupt.indoorPosition.callback.FragmentServiceCallback;
import com.bupt.indoorPosition.callback.InspectUpdateCallback;
import com.bupt.indoorPosition.callback.SettingUpdateCallback;
import com.bupt.indoorPosition.location.LocationProvider;
import com.bupt.indoorPosition.model.ModelService;
import com.bupt.indoorPosition.model.UserService;
import com.bupt.indoorPosition.uti.Constants;
import com.bupt.indoorPosition.uti.Global;
import com.bupt.indooranalysis.fragment.DataFragment;
import com.bupt.indooranalysis.fragment.HistoryFragment;
import com.bupt.indooranalysis.fragment.InspectFragment;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        InspectFragment.OnFragmentInteractionListener,
        HistoryFragment.OnFragmentInteractionListener,
        DataFragment.OnFragmentInteractionListener,
        FragmentServiceCallback {

    private ViewPager mPager;
    private ArrayList<Fragment> mFragmentList = new ArrayList<Fragment>();
    private FragmentPagerAdapter fragmentPagerAdapter;
    private NavigationView navigationView;

    private TextView mTabInspect, mTabHistory, mTabData;
    private ImageView userProfile;
    private TextView userCity, userProvince, userCompany, userName;

    private InspectFragment inspectFragment;
    private HistoryFragment historyFragment;
    private DataFragment dataFragment;
    private int currentIndex;
    private HomeActivityReceiver receiver;
    private Map<String, Intent> serviceMap;
    public Handler handler;
    private BluetoothAdapter bAdapter;
    public TelephonyManager telephonyManager;
    private Timer keepAliveTimer;
    private InspectUpdateCallback cbInspect;
    private SettingUpdateCallback cbSetting;
    private FloatingActionMenu floatingActionMenu;
    private FloatingActionButton actionButton;
    private ArrayList<SubActionButton> floorbuttons;

    private ArrayList<String> floor = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponent();
        initLogin();
        initFloorSelectButton();
    }

    //初始化楼层选择按钮
    protected void initFloorSelectButton(){

        //初始化楼层
        floor.add("F1");
        floor.add("F2");
        floor.add("F3");
        floor.add("F4");
        floor.add("F5");

        floorbuttons = new ArrayList<SubActionButton>();
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(getDrawable(R.drawable.ic_floor));
        actionButton = new FloatingActionButton.Builder(this).setContentView(icon).build();
        FloatingActionMenu.Builder builder = new FloatingActionMenu.Builder(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(150,150);
        actionButton.setPosition(3,layoutParams);

        for(int i=0;i<floor.size();i++){
            TextView textView = new TextView(this);
            textView.setText(floor.get(i));
            SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
            floorbuttons.add(itemBuilder.setContentView(textView).build());
            builder.addSubActionView(floorbuttons.get(i));
        }
        builder.setStartAngle(90);
        builder.attachTo(actionButton);
        floatingActionMenu = builder.build();

        floorbuttons.get(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"select floor",Toast.LENGTH_SHORT).show();
            }
        });

        floorbuttons.get(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"select floor",Toast.LENGTH_SHORT).show();
            }
        });

        floorbuttons.get(2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"select floor",Toast.LENGTH_SHORT).show();
            }
        });

        floorbuttons.get(3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"select floor",Toast.LENGTH_SHORT).show();
            }
        });

        floorbuttons.get(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"select floor",Toast.LENGTH_SHORT).show();
            }
        });

        //待添加其它floor监听器
    }

    protected void initComponent(){

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        //init tab
        mTabInspect = (TextView) findViewById(R.id.txt_tab_inspect);
        mTabHistory = (TextView) findViewById(R.id.txt_tab_history);
        mTabData = (TextView) findViewById(R.id.txt_tab_data);
        mPager = (ViewPager) findViewById(R.id.container);

        //init app toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        //init app navigation
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);


        navigationView.setNavigationItemSelectedListener(this);

        inspectFragment = new InspectFragment();
        historyFragment = new HistoryFragment();
        dataFragment = new DataFragment();

        mFragmentList.add(inspectFragment);
        mFragmentList.add(historyFragment);
        mFragmentList.add(dataFragment);


        fragmentPagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), mFragmentList);
        mPager.setAdapter(fragmentPagerAdapter);
        mPager.setCurrentItem(0);
        mTabInspect.setTextColor(Color.BLACK);
        mTabHistory.setTextColor(Color.GRAY);
        mTabData.setTextColor(Color.GRAY);
        //    mTabInspect.setBackground(null);
        mTabHistory.setBackground(null);
        mTabData.setBackground(null);
        Log.i("Init Component", "add Fragment");

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                Log.i("Init Component", "onPageScrolled");

            }

            @Override
            public void onPageSelected(int position) {

                if(position!=0){
                    floatingActionMenu.close(true);
                    actionButton.setVisibility(View.INVISIBLE);
                }else actionButton.setVisibility(View.VISIBLE);

                mTabInspect.setTextColor(Color.GRAY);
                mTabHistory.setTextColor(Color.GRAY);
                mTabData.setTextColor(Color.GRAY);

                mTabInspect.setBackground(null);
                mTabHistory.setBackground(null);
                mTabData.setBackground(null);

                switch (position) {
                    case 0:
                        mTabInspect.setTextColor(Color.BLACK);
                        mTabInspect.setBackground(getDrawable(R.drawable.shape_rect_button));
                        break;
                    case 1:
                        mTabHistory.setTextColor(Color.BLACK);
                        mTabHistory.setBackground(getDrawable(R.drawable.shape_rect_button));
                        break;
                    case 2:
                        mTabData.setTextColor(Color.BLACK);
                        mTabData.setBackground(getDrawable(R.drawable.shape_rect_button));
                        break;
                    default:
                        break;
                }

                currentIndex = position;

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // initTabLineWidth();
        initUserCenter();
        // 初始化广播接收器
        IntentFilter intentFilter = new IntentFilter(
                Constants.ACTIONURL.MAIN_ACTIVITY_ACTION);
        receiver = new HomeActivityReceiver();
        registerReceiver(receiver, intentFilter);
        //
        serviceMap = new HashMap<String, Intent>();
        // 初始化百度定位
        new LocationProvider(this);
        // 初始化handler
        handler = new HomeHandler();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // 打开蓝牙
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(MainActivity.this, R.string.ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        // Log.d("bluetooth", "ok");
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bAdapter = bluetoothManager.getAdapter();
        bAdapter.enable();
        keepAliveTimer = new Timer();
        keepAliveTimer.schedule(new TimerTask() {

            /**
             * 如果登录，定时发送位置信息，保持服务器session的活性
             */
            @Override
            public void run() {
                // System.out.println(LocationProvider.getProvince() + " "
                // + LocationProvider.getCity() + " "
                // + LocationProvider.getLongditude() + " "
                // + LocationProvider.getLatitidue() + " "
                // + LocationProvider.getPoi() + " "
                // + LocationProvider.getTime());
                switch (Global.loginStatus) {
                    case NOT_LOGINED:
                        break;
                    case LOGINED:
                        boolean statusNow = ModelService.keepAlive(
                                MainActivity.this, LocationProvider.getLocation());
                        if (!statusNow) {
                            Global.loginStatus = Global.LoginStatus.NOT_LOGINED;
                        }
                        break;
                }
            }
        }, 10000, 1000 * 60 * 3);
    }

    //初始化用户相关
    public void initUserCenter() {

        View navHeaderView = navigationView.getHeaderView(0);
        userProfile = (ImageView) navHeaderView.findViewById(R.id.imageView);
        userCity = (TextView) navHeaderView.findViewById(R.id.txt_user_city);
        userProvince = (TextView) navHeaderView.findViewById(R.id.txt_user_province);
        userCompany = (TextView) navHeaderView.findViewById(R.id.txt_user_company);
        userName = (TextView) navHeaderView.findViewById(R.id.txt_user_name);

        if (Global.loginStatus == Global.LoginStatus.NOT_LOGINED) {
            userProfile.setImageResource(R.mipmap.ic_launcher);
            userName.setText("请登录");
            userCity.setVisibility(View.INVISIBLE);
            userProvince.setVisibility(View.INVISIBLE);
            userCompany.setVisibility(View.INVISIBLE);
        } else {
            userProfile.setImageResource(R.drawable.ic_user_profile);
            //TODO


        }

        userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Global.loginStatus == Global.LoginStatus.NOT_LOGINED) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(MainActivity.this, UserCenterActivity.class));
                }
            }
        });

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        return;
    }

    public class MyFragmentPagerAdapter extends FragmentPagerAdapter {

        ArrayList<Fragment> list;

        public MyFragmentPagerAdapter(FragmentManager fm, ArrayList<Fragment> list) {
            super(fm);
            this.list = list;
        }

        @Override
        public Fragment getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getCount() {
            return list.size();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_history) {
            // Handle the History request
            onHistoryItemSelected();
        } else if (id == R.id.nav_data) {
            onDataItemSelected();
        } else if (id == R.id.nav_setting) {
            onSettingsItemSelected();
        } else if (id == R.id.nav_info) {
            onSysteminfoItemSelected();
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            Intent mail = new Intent(Intent.ACTION_SENDTO);
            mail.setData(Uri.parse("maiapp_iconlto:luomingtibo@gmail.com"));
            mail.putExtra(Intent.EXTRA_SUBJECT,"智能室分系统意见反馈");
            mail.putExtra(Intent.EXTRA_TEXT,"写下您的意见...\n");
            startActivity(mail);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onHistoryItemSelected() {

        startActivity(new Intent(this, TabbedActivity.class));
    }

    public void onSysteminfoItemSelected() {

        startActivity(new Intent(this, AboutSystemActivity.class));
    }

    public void onSettingsItemSelected() {
        startActivity(new Intent(this, SettingsActivity.class));

    }

    public void onDataItemSelected() {

    }


    private void initLogin() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Inspector inspector = UserService
                        .selectAllInspector(MainActivity.this);
                ModelService.loadCookie(MainActivity.this);
                // 尝试询问服务器session是否存在
                boolean statusNow = ModelService.keepAlive(MainActivity.this,
                        LocationProvider.getLocation());
                if (inspector == null) {
                    // inspector不存在，用户已经注销
                    if (statusNow) {
                        // 服务器session存在
                        UserService.userLogout(MainActivity.this);
                    }
                    Message msg = new Message();
                    msg.what = 0xf01;
                    handler.sendMessage(msg);
                    return;
                } else {
                    if (statusNow) {
                        // 服务器session存在
                        Global.loginStatus = Global.LoginStatus.LOGINED;
                    } else {
                        // 服务器session不存在，尝试重新登录
                        // 重新登录，保存inspector和session
                        UserService.userLogin(MainActivity.this,
                                inspector.getUsername(),
                                inspector.getPassword());
                    }
                }

                if (Global.loginStatus == Global.LoginStatus.LOGINED) {
                    System.out.println("logined");
                    Message msg = new Message();
                    msg.what = Constants.MSG.HAS_LOGINED;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        Log.i("homeActivity onDestroy", "onDestroy");
        whenFinish();
        super.onDestroy();
    }

    private void whenFinish() {
        if (keepAliveTimer != null)
            keepAliveTimer.cancel();
        if (receiver != null)
            unregisterReceiver(receiver);
        // 停止服务
        for (String k : serviceMap.keySet()) {
            Intent i = serviceMap.get(k);
            stopService(i);
        }
        // 保存cookie
        ModelService.saveCookie(this);
    }

    public class HomeActivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;
            // Log.i("HomeActivityReceiver", "HomeActivityReceiver 收到广播");
            String type = intent.getStringExtra("type");
            Bundle b = intent.getExtras();

            if (Constants.INTENT_TYPE.NONE.equals(type)) {
                // 尝试获取服务器响应bundle
                String s = b.getString("reason");
                int code = b.getInt("code");
                if (s != null) {
                    if (code < 0) {
                        Log.d("code", s);
                        Toast.makeText(MainActivity.this,
                                "server code: " + code + "\n" + s, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            } else if (Constants.INTENT_TYPE.BEACON_LIST_DISPLAY.equals(type)) {
                // 尝试获取showList响应Bundle
                ArrayList<InspectedBeacon> list = (ArrayList<InspectedBeacon>) b
                        .getSerializable("showList");
                if (list != null) {
                    // Log.i("HomeActivityReceiver", "list not null");
                    if (cbInspect != null) {
                        Message msg = new Message();
                        msg.what = Constants.MSG.SHOW_BEACON;
                        msg.setData(b);
                        cbInspect.handleUpdateMessage(msg);
                    }
                    return;
                }
            } else if (Constants.INTENT_TYPE.KEEP_ALIVE.equals(type)) {
                Toast.makeText(MainActivity.this, "与服务器连接已断开\n请检查网络连接并登录",
                        Toast.LENGTH_SHORT).show();
            }

        }
    }

    // 转发message
    class HomeHandler extends Handler {
        // private WeakReference<MainActivity> mActivity;

        // public MainHandler(Activity a) {
        // mActivity = new WeakReference<MainActivity>(a);
        // }
        @Override
        public void handleMessage(Message msg) {
            // Log.i("Home_activity HomeHandler", "receive msg.what " +
            // msg.what);
            // Log.i("Home_activity HomeHandler", "" + (msg.what & 0xf0));
            // Log.i("Home_activity HomeHandler", "" + (msg.what & 0x0f));
            if ((msg.what & 0xf0) == ((int) 0xf0)) {
                if (cbInspect != null) {
                    cbInspect.handleUpdateMessage(msg);
                }
            } else if ((msg.what & 0x0f) == ((int) 0x0f)) {
                // Log.i("Home_activity HomeHandler", "0x0f");
                if (cbSetting != null) {
                    cbSetting.handleUpdateMessage(msg);
                }
            } else if ((msg.what & 0xf00) == ((int) 0xf00)) {
                switch (msg.what) {
                    case 0xf01:
                        Toast.makeText(MainActivity.this, "请登录", Toast.LENGTH_SHORT)
                                .show();
                        break;
                }

            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void startOrStopActivityService(Intent intent, boolean isStart) {
        if (isStart) {
            startService(intent);
            serviceMap.put(intent.getAction(), intent);
        } else {
            stopService(intent);
            serviceMap.remove(intent.getAction());
        }

    }

    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                whenFinish();
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
