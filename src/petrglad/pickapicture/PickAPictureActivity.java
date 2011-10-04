package petrglad.pickapicture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PickAPictureActivity extends Activity {

    static final String TAG = PickAPictureActivity.class.getName();

    private static final int PICTURE_RESULT = 0;
    private static final int PICTURE_FILE_RESULT = 1;
    private static final String INSERT_TAG = "<!--insert-here--><p>Add more pictures via menu.</p>";

    private class PapWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            setProgressBarIndeterminateVisibility(false);
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            setProgressBarIndeterminateVisibility(true);
        }
    }

    WebView webView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new PapWebViewClient());
        webView.getSettings().setJavaScriptEnabled(false);
        if (!getHtmlFile().isFile())
            clearHtml();
        loadHtml();
        registerForContextMenu(webView);
    }

    private File getHtmlFile() {
        return new File(getExternalFilesDir(null), "list.html");
    }

    public void writeHtml(String html) {
        File outFile = getHtmlFile();
        OutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            IOUtils.write(html, out);
            Log.v(TAG, "New html " + html);
        } catch (IOException e) {
            Log.e(TAG, "Can not write html");
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public String readHtml() {
        InputStream in = null;
        try {
            in = new FileInputStream(getHtmlFile());
            return IOUtils.toString(in);
        } catch (IOException e) {
            Log.e(TAG, "Can not read html");
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void clearHtml() {
        writeHtml("<html><head/><body>" + INSERT_TAG + "</body></html>");
    }

    public void addLinkToHtml(File path) {
        Log.v(TAG, "New item image path " + path);
        String html = readHtml();
        if (html != null) // TODO Warn user about error.
            writeHtml(html.replaceFirst(INSERT_TAG,
                    "<p>" + path.getName() + "<br/>" +
                            "<img src=\"file://" + path + "\"/></p>"
                            + INSERT_TAG));
    }

    public void loadHtml() {
        File dir = getExternalFilesDir(null);
        if (dir == null) {
            // TODO Show error to user
            Log.v(TAG, "External files dir is not available.");
            return;
        }
        webView.loadUrl("file://" + getHtmlFile().getAbsolutePath());
        // webView.loadDataWithBaseURL(dir.getAbsolutePath(), html, "text/html",
        // "UTF-8", null);
        // webView.loadData(html, "text/html", "UTF-8"); // This does not load
        // linked resources
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "Options menu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.add_picture_from_camera == item.getItemId()) {
            this.startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), PICTURE_RESULT);
            return true;
        } else if (R.id.add_picture_from_file == item.getItemId()) {
            // List<ProviderInfo> providers =
            // getPackageManager().queryContentProviders(null, 0, 0);
            // Log.d(TAG, "Content providers: " + providers);
            this.startActivityForResult(new Intent(this, ChooseFileActivity.class),
                    PICTURE_FILE_RESULT);
            return true;
        } else if (R.id.clear_html_list == item.getItemId()) {
            clearHtml();
            loadHtml();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO (refactoring) Split this procedure
        if (requestCode == PICTURE_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                // Display image received on the view
                Bitmap pic = (Bitmap) data.getExtras().get("data");
                if (pic != null) {
                    final String pictureFileName = new SimpleDateFormat("yyyy-MM-dd-HH_mm_ss")
                            .format(new Date())
                            + ".jpg";
                    Log.v(TAG, "Adding picture from camera.");
                    File file = new File(getExternalFilesDir(null), pictureFileName);
                    try {
                        final OutputStream out = new FileOutputStream(file);
                        try {
                            pic.compress(Bitmap.CompressFormat.JPEG, 95, out);
                        } finally {
                            out.close();
                        }
                        addLinkToHtml(file);
                        loadHtml();
                    } catch (IOException e) {
                        Log.e(TAG, "Can not open file for saving.", e);
                    }
                }
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                // TODO Notify user that action was cancelled
                Log.d(TAG, "Adding picture from camera cancelled.");
            }
        } else if (requestCode == PICTURE_FILE_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.v(TAG, "Adding picture from file.");
                String picPath = (String) data.getExtras().get(ChooseFileActivity.RESULT_FILE_NAME_PROPERTY);
                Log.e(TAG, "Chosen file name " + picPath);
                addLinkToHtml(new File(picPath));
                loadHtml();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                // TODO Notify user that action was cancelled
                Log.d(TAG, "Adding picture from file cancelled.");
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
