package com.example.caspe.firebasedatabase;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.caspe.firebasedatabase.Model.ShoppingList;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.ArrayList;
import java.util.Map;

//Sofie
public class ShoppingListActivity extends AppCompatActivity {

    ShoppingList shoppingList;
    private ArrayList<String> items;
    private ArrayList<String> deletedItems;
    private ArrayAdapter<String> itemsAdapter;
    private ListView deletedVItems;
    private ArrayAdapter<String> deletedItemsAdapter;
    private ListView lvItems;
    // ArrayList<User> users;
    private String m_Text = "";
    private FirebaseAuth mAuth;
    private String ShoppingListName;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    //Remote Config
    private static final String DELETED_ITEM = "deleted_items";
    private static final String ADDED_ITEM = "added_items";
    private static final String ADDED_ITEM_COLOR = "added_items_color";

    private TextView deletedText;
    private TextView addedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shooping_list);

        Intent intent = getIntent();
        String write = intent.getStringExtra("text");
        deletedText = (TextView) findViewById(R.id.deletedItemText);
        addedText = (TextView) findViewById(R.id.addedItemsText);

        getShoppingListItems(write);
        setTitle(intent.getStringExtra("text"));

        lvItems = (ListView) findViewById(R.id.lvItems);

        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
                String selectedItem = (String) parent.getItemAtPosition(position);
                handleItem(selectedItem, false);
            }

            });

        deletedVItems = (ListView) findViewById(R.id.deletedItems);
        deletedVItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
                String selectedItem = (String) parent.getItemAtPosition(position);
                handleItem(selectedItem, true);

            }

        });

        final Button button = (Button) findViewById(R.id.btnAddItem);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAddItem();

            }
        });

        //Også til Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        remoteConfig();
        }

    //Remote config
    private void remoteConfig() {
        deletedText.setText(mFirebaseRemoteConfig.getString(DELETED_ITEM));
        addedText.setText(mFirebaseRemoteConfig.getString(ADDED_ITEM));
        long cacheExpiration = 3600; // 1 hour in seconds.
        if (mFirebaseRemoteConfig.getInfo( ).getConfigSettings( ).isDeveloperModeEnabled( )) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch ( cacheExpiration )
                .addOnCompleteListener ( this, new OnCompleteListener <Void>( ) {
                    @Override
                    public void onComplete(@NonNull Task <Void> task ) {
                        if ( task.isSuccessful ( )) {
                            mFirebaseRemoteConfig.activateFetched();
                        }
                    }
                });
    }

    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shoppinglistmenu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addUser:
                addUserToList();
                return true;
            case R.id.signOut:
                mAuth.signOut();
                startActivity(new Intent(this, MainActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void addUserToList(){

        final FirebaseHandler fire = new FirebaseHandler();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add user to shopping list by email");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Add user", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                ref.orderByChild("email").equalTo(m_Text).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot datas: dataSnapshot.getChildren()) {
                            String keys = datas.getKey();
                            fire.addUserToShoppingList(shoppingList.getName(), keys);
                            Toast.makeText(ShoppingListActivity.this, keys, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void getShoppingListItems(String ShoppingListName) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ShopDb = database.getReference().child("shoppingLists").child(ShoppingListName);
        ShopDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                shoppingList = dataSnapshot.getValue(ShoppingList.class);
                if (shoppingList != null) {

                    Toast.makeText(ShoppingListActivity.this, shoppingList.getName(),
                            Toast.LENGTH_LONG).show();

                    items = new ArrayList<String>();
                    itemsAdapter = new ArrayAdapter<String>(ShoppingListActivity.this, android.R.layout.simple_list_item_1, items);
                    lvItems.setAdapter(itemsAdapter);

                    deletedItems = new ArrayList<String>();
                    deletedItemsAdapter = new ArrayAdapter<String>(ShoppingListActivity.this, android.R.layout.simple_list_item_1, deletedItems);
                    deletedVItems.setAdapter(deletedItemsAdapter);

            if (shoppingList.getItems() != null) {
                    for (Map.Entry<String, Boolean> entry : shoppingList.getItems().entrySet()) {

                        if (entry.getValue() == true) {
                            items.add(entry.getKey());
                        }
                        if(entry.getValue() == false){
                            deletedItems.add(entry.getKey());
                        }
                    }
                }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());

            }
    });}

    public void onAddItem() {
        EditText etNewItem = (EditText) findViewById(R.id.etNewItem);
        String itemText = etNewItem.getText().toString();
        handleItem(itemText, true);
        etNewItem.setText("");
    }

    public void handleItem(String name, Boolean check) {
        FirebaseHandler fire = new FirebaseHandler();
        fire.addAndRemoveItem(shoppingList.getName(), name, check);

    }
}
