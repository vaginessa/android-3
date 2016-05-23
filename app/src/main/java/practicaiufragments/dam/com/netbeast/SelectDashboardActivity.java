package practicaiufragments.dam.com.netbeast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;



/**
 * Created by Cayetano Rodríguez Medina on 19/5/16.
 */
public class SelectDashboardActivity extends Activity {

    private String IP;
    private String port;

    private static String TAG = SelectDashboardActivity.class.getSimpleName();

    ListView listView;
    CustomListAdapter adapter;
    ArrayList<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_dashboard_activity);

        listView = (ListView) findViewById(R.id.list);

        list = new ArrayList<>();
        adapter = new CustomListAdapter(this,list);

        listView.setAdapter(adapter);

        // Check for WiFi connectivity
        ConnectivityManager connManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = null;
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) // connected to wifi
                mWifi  = activeNetwork;
        }

        if(mWifi == null || !mWifi.isConnected())
        {
            Log.d(TAG, "Sorry! You need to be in a WiFi network in order to send UDP multicast packets. Aborting.");
            // If wifi is not connected show toast
            Toast.makeText(getApplicationContext(),
                    "Sorry! You need to be in a WiFi network", Toast.LENGTH_SHORT).show();
        }

        fillList();
    }

    public void fillList(){
        Global g = Global.getInstance();
        JSONArray dashboards = g.getDashboards();

        if (dashboards != null)
            // Go over the JSONArray dashboards that has all dashboards as JSON Objects
            for (int i = 0; i < dashboards.length(); i++) {
                // Create the list if it's not created
                if (list == null)
                    list = new ArrayList<>();

                try {
                    // Get the JSONObject for position i in the dashboards list
                    JSONObject dash = (JSONObject) dashboards.get(i);
                    IP = dash.getString("ip");
                    port = dash.getString("port");
                    // Add to the list in screen the dashboard in position i, as IP:port
                    list.add(IP + ":" + port);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();
            }
        else
            Log.d("LOG", "No dashboards");
    }

    // Method that will be called in the onClick of the "Use dashboard in the cloud" button
    public void cloudDashboard(View v){
        Global g = Global.getInstance();
        // dashboard cloud address
        g.setIP("dashboard.827722d5.svc.dockerapp.io");
        g.setPort("");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
