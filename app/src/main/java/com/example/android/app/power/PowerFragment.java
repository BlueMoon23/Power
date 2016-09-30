package com.example.android.app.power;

import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jonat on 18/09/2016.
 */
public class PowerFragment extends Fragment {

    public static final String WEB_ADDRESS= "http://www.bmreports.com/bsp/additional/soapfunctions.php?element=generationbyfueltypetable&submit=Invoke";

    ArrayAdapter<String> mForecastAdapter;

    public PowerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.powerfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id==R.id.action_refresh){
            FetchPowerTask powerTask = new FetchPowerTask();
            powerTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Create some dummy data
        String[] data = {
                "CCGT 32.6%",
                "OCGT 0%",
                "Oil 0%",
                "Coal 2.1%",
                "Nuclear 29.2%",
                "PS 1.6%",
                "Wind 21.6%",
                "NPSHYD 1.4%",
                "Other 4.9%",
                "INTFR 3.2%",
                "INTIRL 0%",
                "INTNED 3.3%",
                "INTEW 0%"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        //Create an array adapter to show the dummy data
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchPowerTask extends AsyncTask<Object, String, Integer> {

        private final String LOG_TAG = FetchPowerTask.class.getSimpleName();


        @Override
        protected Integer doInBackground(Object... objects) {
            XmlPullParser receivedData = tryDownloadingXmlData();
            int recordsFound = tryParsingXmlData(receivedData);
            return recordsFound;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if(values.length==0){
                Log.i(LOG_TAG, "No data downloaded");
            }
            if(values.length==5){
                String type = values[0];
                String ic = values[1];
                String data = values[2];
                String val = values[3];
                String pct = values[4];

                //Log it.
                Log.i(LOG_TAG, "Type: " + type + ", IC: " + ic + ", Value: " + val + ", Percentage: " + pct + ", Data: " + data);

            }
            super.onProgressUpdate(values);
        }

        private XmlPullParser tryDownloadingXmlData(){
            try{
                URL xmUrl = new URL(WEB_ADDRESS);
                XmlPullParser receivedData = XmlPullParserFactory.newInstance().newPullParser();
                receivedData.setInput(xmUrl.openStream(), null);
                Log.i(LOG_TAG,  "Received Data is: " + receivedData);
                return receivedData;

            }catch (XmlPullParserException e){
                Log.e(LOG_TAG, "XmlPullParserException", e);
            }catch (IOException e){
                Log.e(LOG_TAG, "XmlPullParserException", e);
            }

            return null;
        }

        private int tryParsingXmlData(XmlPullParser receivedData){
            if(receivedData!=null){
                try{
                    return processReceivedData(receivedData);
                }catch (XmlPullParserException e){
                    Log.e(LOG_TAG, "Pull Parser failure", e);
                }catch (IOException e){
                    Log.e(LOG_TAG, "IO Exception parsing XML", e);
                }
            }
            return 0;
        }

        private int processReceivedData(XmlPullParser xmlData) throws IOException, XmlPullParserException {
            int eventType = -1;
            int recordsFound = 0;

            //Find values in the XML records
            String type = "";
            String ic = "";
            String val = "";
            String pct = "";
            String data = "";



            while (eventType != XmlResourceParser.END_DOCUMENT){
                String tagName = xmlData.getName();

                Log.i(LOG_TAG, "" + tagName);


                switch (eventType){


                    case XmlResourceParser.START_TAG:

                        //Start of a record, so pull values encoded as attributes.
                        if(tagName.equals("FUEL")){
                            type = xmlData.getAttributeValue(null, "TYPE");
                            ic = xmlData.getAttributeValue(null, "IC");
                            val = xmlData.getAttributeValue(null, "VAL");
                            pct = xmlData.getAttributeValue(null, "PCT");
                            data = "";
                        }else if(tagName.equals("HH")){
                            Log.i(LOG_TAG, "Test");

                        }
                        break;









                    //Grab data text
                    case XmlResourceParser.TEXT:
                        data += xmlData.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if(tagName.equals("FUEL")){
                            recordsFound++;
                            publishProgress(type, ic, data, val, pct);
                        }
                        break;
                }
                eventType = xmlData.next();
            }

            if (recordsFound==0){
                publishProgress();
            }
            Log.i(LOG_TAG, "Finished processing " + recordsFound + " records");

            return recordsFound;
        }
    }
}
