package `in`.app.connect.bottomnav.userprofile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.PopupViewProfile
import `in`.app.connect.R
import `in`.app.connect.utils.ConnectAppApplication

class ContactsActivity : AppCompatActivity() {
    private val CONTACTS_REQUEST_CODE = 101
    private val PHONE_PERMISSION_REQUEST_CODE = 102

    private var cursor: Cursor? = null
    private var adapter: CustomContactsAdapter? = null
    var contactList = mutableListOf<ContactInfo>()
    private lateinit var listView: ListView

    var filteredList = mutableListOf<ContactInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_contacts)
        listView = findViewById(R.id.contactListView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_REQUEST_CODE
            )
        } else {
            displayContacts("")
        }

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the back button


        val searchView: SearchView = findViewById(R.id.searchView)
        searchView.setOnSearchClickListener {
            searchView.visibility = View.VISIBLE
            findViewById<View>(R.id.toolbarTitle).visibility = View.GONE
            findViewById<View>(R.id.toolbarSubtitle).visibility = View.GONE
        }

        searchView.setOnCloseListener {
            findViewById<View>(R.id.toolbarTitle).visibility = View.VISIBLE
            findViewById<View>(R.id.toolbarSubtitle).visibility = View.VISIBLE
            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Update the filtered list based on the search query
                filteredList = filterContacts(newText ?: "").toMutableList()
                adapter = CustomContactsAdapter(this@ContactsActivity, filteredList)
                listView.adapter = adapter
                adapter?.notifyDataSetChanged()
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun filterContacts(filter: String): List<ContactInfo> {
        return contactList.filter { contact ->
            // Include a contact in the filtered list if its name or phone number contains the filter text
            val nameMatches = contact.name.contains(filter, ignoreCase = true)
            val phoneNumberMatches = contact.phoneNumber.contains(filter)
            nameMatches || phoneNumberMatches
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CONTACTS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                displayContacts("")
            } else {
                Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@ContactsActivity,
                    "Phone state permission allowed",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@ContactsActivity,
                    "Phone state permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun displayContacts(filter: String) {

        // Release the previous cursor and adapter if they exist
        cursor?.close()

        cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$filter%"),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
        )

        if (cursor != null) {
            try {
                cursor!!.moveToFirst()
                val phoneNumbers = mutableSetOf<String>()

                while (!cursor!!.isAfterLast) {
                    val displayName =
                        cursor!!.getString(cursor!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        cursor!!.getString(cursor!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    if (!phoneNumber.isNullOrBlank()) {
                        val standardizedPhoneNumber = standardizePhoneNumber(phoneNumber)
                        contactList.add(ContactInfo(displayName, standardizedPhoneNumber, false))
                        phoneNumbers.add(standardizedPhoneNumber)
                    }
                    cursor!!.moveToNext()
                }

                // Use a single batch query to check if phone numbers are registered
                isPhoneNumberRegistered(phoneNumbers) { registeredPhoneNumbers ->
                    val contactMap = mutableMapOf<String, ContactInfo>()

                    for (contact in contactList) {
                        val contactPhoneNumber = contact.phoneNumber
                        val normalizedContactPhoneNumber =
                            contactPhoneNumber.replace(Regex("[^\\d+]"), "")

                        // Check if the normalized contact phone number is contained in the registered phone numbers
                        if (registeredPhoneNumbers.any { it.contains(normalizedContactPhoneNumber) }) {
                            contact.isRegistered = true
                            // Change the contact's phoneNumber to the registered phoneNumber
                            contact.phoneNumber = registeredPhoneNumbers.find {
                                it.contains(normalizedContactPhoneNumber)
                            } ?: contact.phoneNumber
                        }
                        contactMap[contact.phoneNumber] = contact
                    }

                    contactList = contactMap.values.toList().toMutableList()
                    // Sort the contactList to display registered contacts first
                    contactList =
                        contactList.sortedWith(compareByDescending<ContactInfo> { it.isRegistered })
                            .toMutableList()
                    adapter = CustomContactsAdapter(this@ContactsActivity, contactList)
                    listView.adapter = adapter
                    adapter?.notifyDataSetChanged()
                    findViewById<TextView>(R.id.toolbarSubtitle).text =
                        "${contactList.size} contacts"
                }


            } catch (e: Exception) {
                println(e)
            } finally {
                cursor?.close()
            }
        }
    }

    private fun standardizePhoneNumber(phoneNumber: String): String {
        // Remove spaces and other characters from the phone number to standardize it
        return phoneNumber.replace(Regex("[^\\d+]"), "")
    }


    private fun isPhoneNumberRegistered(
        phoneNumbers: Set<String>,
        callback: (Set<String>) -> Unit
    ) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        val registeredPhoneNumbers = mutableSetOf<String>()

        databaseReference.orderByChild("phoneNumber").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnapshot ->
                    val dbPhoneNumber =
                        userSnapshot.child("phoneNumber").getValue(String::class.java)
                    if (dbPhoneNumber != null) {
                        val normalizedDbPhoneNumber = dbPhoneNumber.replace(Regex("[^\\d+]"), "")

                        // Iterate through the provided phone numbers
                        for (phoneNumber in phoneNumbers) {
                            val normalizedPhoneNumber = phoneNumber.replace(Regex("[^\\d+]"), "")
                            // Check if the normalized DB phone number contains the normalized provided phone number
                            if (normalizedDbPhoneNumber.endsWith(normalizedPhoneNumber)) {
                                registeredPhoneNumbers.add(normalizedDbPhoneNumber)
                                break
                            }
                        }
                    }
                }
                callback(registeredPhoneNumbers)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors if needed
                Toast.makeText(this@ContactsActivity, error.message, Toast.LENGTH_LONG).show()
                callback(emptySet())
            }
        })
    }

    data class ContactInfo(val name: String, var phoneNumber: String, var isRegistered: Boolean)

    class CustomContactsAdapter(
        private val context: Context,
        private val contactList: List<ContactInfo>
    ) : BaseAdapter() {

        private class ViewHolder(view: View) {
            val contactNameView: TextView
            val contactPhoneNumberView: TextView
            val viewTextView: TextView

            init {
                contactNameView = view.findViewById(R.id.contactNameTextView)
                contactPhoneNumberView = view.findViewById(R.id.contactPhoneNumberTextView)
                viewTextView = view.findViewById(R.id.viewTextView)
            }
        }

        override fun getCount(): Int {
            return contactList.size
        }

        override fun getItem(position: Int): Any {
            return contactList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                rowView = inflater.inflate(R.layout.contact_list_item, parent, false)
                viewHolder = ViewHolder(rowView)
                rowView.tag = viewHolder
            } else {
                rowView = convertView
                viewHolder = convertView.tag as ViewHolder
            }

            val contactInfo = getItem(position) as ContactInfo

            viewHolder.contactNameView.text = contactInfo.name
            viewHolder.contactPhoneNumberView.text = contactInfo.phoneNumber
            if (contactInfo.isRegistered) {
                viewHolder.viewTextView.text = "View"
                viewHolder.viewTextView.setOnClickListener {
                    // Handle the "View Profile" logic here
                    openProfile(contactInfo.phoneNumber)
                }
            } else {
                viewHolder.viewTextView.text = "Invite"
                viewHolder.viewTextView.setOnClickListener {
                    // Handle the "Invite" logic here
                    inviteContact(contactInfo.phoneNumber)
                }
            }
            return rowView
        }

        private fun openProfile(phoneNumber: String) {
            val intent = Intent(context, PopupViewProfile::class.java)
            intent.putExtra("phoneNumber", phoneNumber)
            context.startActivity(intent)
        }

        private fun inviteContact(phoneNumber: String) {
            // Construct the dynamic link URL
            val dynamicLinkUriString =
                "https://play.google.com/store/apps/details?id=in.connect.app\n"

            // Create the invitation message
            val inviteMessage =
                "Hello, let's connect on Connect-App! Download the app and join me for seamless communication: $dynamicLinkUriString"

            // Start the SMS intent
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
            intent.putExtra("sms_body", inviteMessage)
            context.startActivity(intent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}
