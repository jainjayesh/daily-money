package com.bottleworks.dailymoney;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;

import com.bottleworks.commons.util.Files;
import com.bottleworks.commons.util.Formats;
import com.bottleworks.commons.util.GUIs;
import com.bottleworks.commons.util.Logger;
import com.bottleworks.dailymoney.data.Account;
import com.bottleworks.dailymoney.data.DataCreator;
import com.bottleworks.dailymoney.data.Detail;
import com.bottleworks.dailymoney.data.IDataProvider;
import com.bottleworks.dailymoney.ui.Contexts;
import com.bottleworks.dailymoney.ui.ContextsActivity;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

public class DataMaintenanceActivity extends ContextsActivity implements OnClickListener {

    String CSV_ENCODEING = "utf8";
    
    String workingFolder;
    
    boolean datedCSV = false;
    
    static final String APPVER = "appver:";
    
    DateFormat format = new SimpleDateFormat("yyyyMMdd");
    
    int vercode = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datamain);
        workingFolder = Contexts.instance().getPrefWorkingFolder();
        datedCSV = Contexts.instance().isPrefExportDatedCSV();
        
        vercode = Contexts.instance().getApplicationVersionCode();
        
        initialListener();

    }

    private void initialListener() {
        findViewById(R.id.datamain_import_csv).setOnClickListener(this);
        findViewById(R.id.datamain_export_csv).setOnClickListener(this);
        findViewById(R.id.datamain_reset).setOnClickListener(this);
        findViewById(R.id.datamain_create_default).setOnClickListener(this);
        findViewById(R.id.datamain_clear_folder).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.datamain_import_csv:
            doImportCSV();
            break;
        case R.id.datamain_export_csv:
            doExportCSV();
            break;
        case R.id.datamain_reset:
            doReset();
            break;
        case R.id.datamain_create_default:
            doCreateDefault();
            break;
        case R.id.datamain_clear_folder:
            doClearFolder();
            break;
        }
    }

    private void doClearFolder() {
        final GUIs.IBusyListener job = new GUIs.BusyAdapter() {
            @Override
            public void onBusyFinish() {
                GUIs.alert(DataMaintenanceActivity.this, i18n.string(R.string.msg_folder_cleared,workingFolder));
            }

            @Override
            public void run() {
                File sd = Environment.getExternalStorageDirectory();
                File folder = new File(sd, workingFolder);
                if (!folder.exists()) {
                    return;
                }
                for(File f: folder.listFiles()){
                   if(f.isFile() && f.getName().toLowerCase().endsWith(".csv")){
                       f.delete();
                   }
                }
            }
        };

        GUIs.confirm(this, i18n.string(R.string.qmsg_clear_folder,workingFolder), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(Object data) {
                if (((Integer) data).intValue() == GUIs.OK_BUTTON) {
                    GUIs.doBusy(DataMaintenanceActivity.this, job);
                }
                return true;
            }
        });
        
    }

    private void doCreateDefault() {

        final GUIs.IBusyListener job = new GUIs.BusyAdapter() {
            @Override
            public void onBusyFinish() {
                GUIs.alert(DataMaintenanceActivity.this, R.string.msg_default_created);
            }

            @Override
            public void run() {
                IDataProvider idp = Contexts.instance().getDataProvider();
                new DataCreator(idp, i18n).createDefaultAccount();
            }
        };

        GUIs.confirm(this, i18n.string(R.string.qmsg_create_default), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(Object data) {
                if (((Integer) data).intValue() == GUIs.OK_BUTTON) {
                    GUIs.doBusy(DataMaintenanceActivity.this, job);
                }
                return true;
            }
        });
    }

    private void doReset() {

        final GUIs.IBusyListener job = new GUIs.BusyAdapter() {
            @Override
            public void run() {
                IDataProvider idp = Contexts.instance().getDataProvider();
                idp.reset();
            }
        };

        GUIs.confirm(this, i18n.string(R.string.qmsg_reset), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(Object data) {
                if (((Integer) data).intValue() == GUIs.OK_BUTTON) {
                    GUIs.doBusy(DataMaintenanceActivity.this, job);
                }
                return true;
            }
        });
    }

    private void doExportCSV() {        
        new AlertDialog.Builder(this).setTitle(i18n.string(R.string.qmsg_export_csv))
                .setItems(R.array.csv_impexp_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        final GUIs.IBusyListener job = new GUIs.BusyAdapter() {
                            public void onBusyError(Throwable t) {
                                GUIs.error(DataMaintenanceActivity.this, t);
                            }
                            @Override
                            public void run() {
                                try {
                                    _exportToCSV(which);
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage(),e);
                                }
                            }
                        };
                        GUIs.doBusy(DataMaintenanceActivity.this, job);
                    }
                }).show();
    }

    private void doImportCSV() {
        
        new AlertDialog.Builder(this).setTitle(i18n.string(R.string.qmsg_import_csv))
                .setItems(R.array.csv_impexp_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        final GUIs.IBusyListener job = new GUIs.BusyAdapter() {
                            public void onBusyError(Throwable t) {
                                GUIs.error(DataMaintenanceActivity.this, t);
                            }

                            @Override
                            public void run() {
                                try {
                                    _importFromCSV(which);
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage(),e);
                                }
                            }
                        };
                        GUIs.doBusy(DataMaintenanceActivity.this, job);
                    }
                }).show();
    }
    
    

    private File getWorkingFile(String name, boolean create) throws IOException {
        File sd = Environment.getExternalStorageDirectory();
        File folder = new File(sd, workingFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File file = new File(folder, name);
        if (create && !file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    /** running in thread **/
    private void _exportToCSV(int mode) throws IOException {
        if(Contexts.DEBUG){
            Logger.d("export to csv "+mode);
        }
        boolean account = false;
        boolean detail = false;
        switch(mode){
            case 0:
                account = detail = true;
                break;
            case 1:
                account = true;
                break;
            case 2:
                detail = true;
                break;
            default :return;
        }
        IDataProvider idp = Contexts.instance().getDataProvider();
        StringWriter sw;
        CsvWriter csvw;
        int count = 0;
        if(detail){
            sw = new StringWriter();
            csvw = new CsvWriter(sw, ',');
            csvw.writeRecord(new String[]{"id","from","to","date","value","note","archived",APPVER+vercode});
            for (Detail d : idp.listAllDetail()) {
                count++;
                csvw.writeRecord(new String[] { Integer.toString(d.getId()), d.getFrom(), d.getTo(),
                        Formats.normalizeDate2String(d.getDate()), Formats.normalizeDouble2String(d.getMoney()),
                        d.getNote(),Boolean.toString(d.isArchived())});
            }
            csvw.close();
            String csv = sw.toString();
            File file = getWorkingFile("details.csv", true);
            Files.saveString(csv, file, CSV_ENCODEING);
            if(datedCSV){
                file = getWorkingFile("details."+format.format(new Date())+".csv", true);
                Files.saveString(csv, file, CSV_ENCODEING);
            }
            if(Contexts.DEBUG){
                Logger.d("export to details.csv");
            }
        }

        if(account){
            sw = new StringWriter();
            csvw = new CsvWriter(sw, ',');
            csvw.writeRecord(new String[]{"id","type","name","init",APPVER+vercode});
            for (Account a : idp.listAccount(null)) {
                count++;
                csvw.writeRecord(new String[]{a.getId(),a.getType(),a.getName(),Formats.normalizeDouble2String(a.getInitialValue())});
            }
            csvw.close();
            String csv = sw.toString();
            File file = getWorkingFile("accounts.csv", true);
            Files.saveString(csv, file, CSV_ENCODEING);
            if(datedCSV){
                file = getWorkingFile("accounts."+format.format(new Date())+".csv", true);
                Files.saveString(csv, file, CSV_ENCODEING);
            }
            if(Contexts.DEBUG){
                Logger.d("export to accounts.csv");
            }
        }
        
        final String msg = i18n.string(R.string.msg_csv_exported,Integer.toString(count),workingFolder);
        GUIs.post(new Runnable(){
            @Override
            public void run() {
                GUIs.alert(DataMaintenanceActivity.this,msg);                
            }});
        

    }
    
    private int getAppver(String str){
        if(str!=null && str.startsWith(APPVER)){
            try{
                return Integer.parseInt(str.substring(APPVER.length()));
            }catch(Exception x){
                if(Contexts.DEBUG){
                    Logger.d(x.getMessage());
                }
            }
        }
        return 0;
    }
    
    /** running in thread **/
    private void _importFromCSV(int mode) throws Exception{
        if(Contexts.DEBUG){
            Logger.d("import from csv "+mode);
        }
        boolean account = false;
        boolean detail = false;
        switch(mode){
            case 0:
                account = detail = true;
                break;
            case 1:
                account = true;
                break;
            case 2:
                detail = true;
                break;
            default :return;
        }
        
        IDataProvider idp = Contexts.instance().getDataProvider();
        Runnable nocsv = new Runnable(){
            @Override
            public void run() {
                GUIs.alert(DataMaintenanceActivity.this,R.string.msg_no_csv);                
            }};
            
        File details = getWorkingFile("details.csv", false);
        File accounts = getWorkingFile("accounts.csv", false);
        
        if((detail && (!details.exists() || !details.canRead())) || 
                (account && (!accounts.exists() || !accounts.canRead())) ){
            GUIs.post(nocsv);
            return;
        }
        
        CsvReader accountReader=null;
        CsvReader detailReader=null;
        
        try{
            int count = 0;
            if(account){
                accountReader = new CsvReader(new FileReader(accounts));
            }
            if(detail){
                detailReader = new CsvReader(new FileReader(details));
            }
            
            if((accountReader!=null && !accountReader.readHeaders())){
                GUIs.post(nocsv);
                return;
            }
            
            //don't combine with account checker
            if((detailReader!=null && !detailReader.readHeaders())){
                GUIs.post(nocsv);
                return;
            }
            
            if(detail){
                detailReader.setTrimWhitespace(true);
                int appver = getAppver(detailReader.getHeaders()[detailReader.getHeaderCount()-1]);
                
                idp.deleteAllDetail();
                while(detailReader.readRecord()){
                    Detail det = new Detail(detailReader.get("from"),detailReader.get("to"),Formats.normalizeString2Date(detailReader.get("date")),Formats.normalizeString2Double(detailReader.get("value")),detailReader.get("note"));
                    det.setArchived(Boolean.parseBoolean(detailReader.get("archived")));
                    idp.newDetailNoCheck(Integer.parseInt(detailReader.get("id")),det);
                    count ++;
                }
                detailReader.close();
                detailReader = null;
                if(Contexts.DEBUG){
                    Logger.d("import to details.csv ver:"+appver);
                }
            }
            
            if(account){
                accountReader.setTrimWhitespace(true);
                int appver = getAppver(accountReader.getHeaders()[accountReader.getHeaderCount()-1]);
                idp.deleteAllAccount();
                while(accountReader.readRecord()){
                    Account acc = new Account(accountReader.get("type"),accountReader.get("name"),Formats.normalizeString2Double(accountReader.get("init")));
                    idp.newAccountNoCheck(accountReader.get("id"),acc);
                    count ++;
                }
                accountReader.close();
                accountReader = null;
                if(Contexts.DEBUG){
                    Logger.d("import to accounts.csv ver:"+appver);
                }
            }
            
            final String msg = i18n.string(R.string.msg_csv_imported,Integer.toString(count),workingFolder);
            GUIs.post(new Runnable(){
                @Override
                public void run() {
                    GUIs.alert(DataMaintenanceActivity.this,msg);                
                }});
        }finally{
            if(accountReader!=null){
                accountReader.close();
            }
            if(detailReader!=null){
                detailReader.close();
            }
        }
    }
}