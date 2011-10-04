package petrglad.pickapicture;

import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import petrglad.pickapicture.util.BrakeIterator;
import petrglad.pickapicture.util.FileTreeIterator;
import petrglad.pickapicture.util.StopIterationException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

public class ChooseFileActivity extends Activity {

    class IsPictureFile implements Predicate<File> {
        @Override
        public boolean apply(File input) {
            return input.isFile() && input.canRead()
                    && input.getName().toLowerCase().endsWith(".jpg");
        }
    }

    class NameFileFilter implements Predicate<File> {

        final String namePrefix;

        public NameFileFilter(String namePrefix) {
            this.namePrefix = namePrefix.toLowerCase();
        }

        @Override
        public boolean apply(File input) {
            return input.getName().toLowerCase().startsWith(namePrefix);
        }

        @Override
        public String toString() {
            return "NameFileFilter [namePrefix=" + namePrefix + "]";
        }
    }

    class FileListAdapter extends BaseAdapter /* implements Filterable */{
        private class ViewHolder {
            public TextView text1;
            public TextView text2;
        }

        public final List<File> list = Lists.newArrayList();

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder views;
            if (convertView == null) {
                convertView = LayoutInflater.from(ChooseFileActivity.this).inflate(
                        android.R.layout.simple_list_item_2, null);
                views = new ViewHolder();
                views.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                views.text2 = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(views);
            } else
                views = (ViewHolder) convertView.getTag();

            File f = list.get(position);
            if (f == null) {
                // FIXME we get here when filter changes.
                views.text1.setText("null");
                views.text2.setText("Unknown path");
            } else {
                views.text1.setText(f.getName());
                views.text2.setText(f.getParent());
            }
            return convertView;

        }

        @Override
        public long getItemId(int location) {
            return location;
        }

        @Override
        public Object getItem(int location) {
            return list.get(location);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        public void clear() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    list.clear();
                    notifyDataSetChanged();
                }
            });
        }

        public void add(final Iterable<File> batch) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (File f : batch)
                        list.add(f);
                    notifyDataSetChanged();
                }
            });
        }
    }

    static final String TAG = ChooseFileActivity.class.getName();
    public static final String RESULT_FILE_NAME_PROPERTY = "picture_file_name";

    /**
     * For scanner tasks
     */
    Executor executor = new ThreadPoolExecutor(0, 1, 2, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("Scn worker");
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }
            });

    volatile Runnable scanWorker;
    FileListAdapter listData;
    protected Thread worker;
    private ListView listView;
    private EditText filterText;

    // XXX Hack - need to clean up selection logic (how to do selection tracking
    // properly?).
    private long selectedRowId = ListView.INVALID_ROW_ID;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.choose_picture);

        final Button button = (Button) findViewById(R.id.clearFilterButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView text = (TextView) findViewById(R.id.filterTextField);
                String namePattern = "";
                text.setText(namePattern);
                setNamePattern(namePattern);
            }
        });

        listView = (ListView) findViewById(R.id.optionsListView);
        listData = new FileListAdapter();
        listView.setAdapter(listData);

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.v(TAG, "onItemClick " + arg2 + ", " + arg3);
                // TODO Show ok/cancel dialog here and return activity result
                // here if confirmed.
                Toast.makeText(getApplicationContext(),
                        "Selected " + getItemFile(arg3) + "\nUse menu to confirm choise.",
                        Toast.LENGTH_LONG).show();
                selectedRowId = arg3;
            }
        });

        filterText = (EditText) findViewById(R.id.filterTextField);
        filterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setNamePattern(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setNamePattern("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Options menu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.choose_file_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.item_choose_ok);
        // :( Just assigning new title for menu item does not have effect.
        if (selectedRowId == ListView.INVALID_ROW_ID) {
            menu.add(0, R.id.item_choose_ok, 0, "No item selected")
                    .setEnabled(false);
        } else {
            menu.add(0, R.id.item_choose_ok, 0, "Use file " + getItemFile(selectedRowId))
                    .setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.item_choose_ok == item.getItemId()) {
            stopScanner();
            Intent intent = this.getIntent();
            if (selectedRowId == ListView.INVALID_ROW_ID) {
                Toast.makeText(getApplicationContext(), "No file chosen.", Toast.LENGTH_SHORT)
                        .show();
            } else {
                intent.putExtra(RESULT_FILE_NAME_PROPERTY, getItemFile(selectedRowId));
                this.setResult(RESULT_OK, intent);
                finish();
            }
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    private String getItemFile(long rowId) {
        return listData.list.get((int) rowId).getAbsolutePath();
    }

    public void setNamePattern(String namePattern) {
        // TODO Do not re-scan if pattern did not change
        Log.d(TAG, "New filter " + namePattern);
        scan(namePattern.isEmpty() ? null : new NameFileFilter(namePattern));
    }

    protected void scan(final Predicate<File> nameFileFilter) {
        stopScanner();
        setProgressBarIndeterminateVisibility(true);
        listData.clear();
        scanWorker = new Runnable() {
            @Override
            public void run() {
                // TODO Split this procedure and extract scanner class
                final File root = new File("/mnt/sdcard");
                if (!root.isDirectory() || !root.canRead())
                    Log.e(TAG, "Can not scan " + root.getAbsolutePath());
                else {
                    Log.v(TAG, "Started iteration #" + hashCode());
                    try {
                        final Object r = this;
                        Supplier<Boolean> canContinue = new Supplier<Boolean>() {
                            @Override
                            public Boolean get() {
                                return ChooseFileActivity.this.scanWorker == r;
                            }
                        };
                        final Predicate<File> fileFilter = nameFileFilter == null ?
                                new IsPictureFile() : and(new IsPictureFile(), nameFileFilter);
                        // BrakeIterator is used to stop this procedure sooner
                        // (for faster re-scans).
                        final Iterator<File> i = filter(new BrakeIterator<File>(
                                new FileTreeIterator(root), canContinue), fileFilter);

                        // TODO Cache full scan results for faster re-filtering
                        // TODO Keep scan results when screen is rotated

                        // Batch updates to not update UI too often
                        long flushTime = System.currentTimeMillis();
                        List<File> batch = new ArrayList<File>();
                        while (i.hasNext()) {
                            long now = System.currentTimeMillis();
                            // XXX This does not work well if there are long
                            // pauses between results.
                            if (flushTime + 1500 < now) {
                                flushTime = now;
                                listData.add(batch);
                                batch = new ArrayList<File>();
                            } else
                                batch.add(i.next());
                        }
                        if (canContinue.get()) {
                            listData.add(batch);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setProgressBarIndeterminateVisibility(false);
                                }
                            });
                        }
                        Log.v(TAG, "Finished iteration #" + hashCode());
                    } catch (StopIterationException e) {
                        Log.v(TAG, "Aborted iteration #" + hashCode(), e);
                    }
                }
            }
        };
        executor.execute(scanWorker);
    }

    @Override
    protected void onDestroy() {
        stopScanner();
        super.onDestroy();
    }

    void stopScanner() {
        scanWorker = null;
    }
}
