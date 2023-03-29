package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.login.LoginManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.*
import com.thinkbloxph.chatwithai.api.GoogleApi
import com.thinkbloxph.chatwithai.databinding.FragmentChatScreenBinding
import com.thinkbloxph.chatwithai.helper.*
import com.thinkbloxph.chatwithai.`interface`.TypingStatusListener
import com.thinkbloxph.chatwithai.network.UserDatabase
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val INNER_TAG = "ChatScreenFragment"

class ChatScreenFragment : Fragment(),TextToSpeechListener {
    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var sendButton: Button
    private lateinit var recordButton: Button
    private lateinit var messageInputField: TextInputEditText
    private val userDb = UserDatabase()
    private var simulateTyping: Boolean = false

    private var isEnableSpeaking: Boolean = false
    private var isSpeaking: Boolean = false

    private lateinit var reminderManager: ReminderManager

    //private var currentPrompt:String = ""
    private lateinit var callback: OnBackPressedCallback
    private var isRecording = false
    val permissionManager = PermissionManager.getInstance()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        /* val spinnerItem = requireActivity().findViewById<AppCompatSpinner>(R.id.action_spinner)
         val spinnerState = Bundle().apply {
             if(spinnerItem?.selectedItemPosition!=null){
                 putInt("selected_item_position", spinnerItem.selectedItemPosition)
             }
         }
         outState.putBundle("spinner_state", spinnerState)*/
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        /*if (savedInstanceState != null) {
            val spinnerItem = requireActivity().findViewById<AppCompatSpinner>(R.id.action_spinner)
            val spinnerState = savedInstanceState.getBundle("spinner_state")
            if (spinnerState != null) {
                spinnerItem.setSelection(spinnerState.getInt("selected_item_position"))
            }
        }*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        firebaseAuth = Firebase.auth

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        messageListAdapter = MessageListAdapter(binding.messageListRecyclerView)
        binding.messageListRecyclerView.adapter = messageListAdapter
        binding.messageListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set other view references using binding
        messageInputField = binding.messageInputField
        sendButton = binding.sendButton
        recordButton = binding.recordButton
        recordButton.isEnabled = isEnableSpeaking


        reminderManager = ReminderManager.getInstance(requireContext())

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        TextToSpeechHelper.initialize(requireContext())
        TextToSpeechHelper.getInstance().addListener(this)

        messageListAdapter.typingStatusListener = object : TypingStatusListener {
            override fun onTypingStatusChanged(isTyping: Boolean) {
                // Notify the listener that the typing status has changed
                sendButton.isEnabled = !isTyping
                recordButton.isEnabled = !isTyping
            }
        }

        val recordButton = binding.recordButton
        val openAI = OpenAIAPI(lifecycleScope, requireContext())

        recordButton.setOnClickListener {
            if (AudioRecorder.getInstance()?.checkPermission()!!) {
                Log.d(TAG, "[${INNER_TAG}]: permission granted!")
                var remainingCredit = _userViewModel.getCredit()
                var isSubscribed = _userViewModel.getIsSubscribed()
                var recordCreditPrice = _userViewModel.getRecordCreditPrice()
                var completionCreditPrice = _userViewModel.getCompletionCreditPrice()

                if (remainingCredit != null && isSubscribed != null) {
                    if (remainingCredit >= recordCreditPrice!! || isSubscribed) {
                        isRecording = !isRecording // Toggle the boolean value

                        recordButton.apply {
                            if (isRecording) {
                                setIconResource(R.drawable.baseline_stop_circle_24)
                            } else {
                                setIconResource(R.drawable.baseline_keyboard_voice_24)
                                // disable record button
                                UIHelper.getInstance().showLoading()
                                enableDisableRecordSend(false)
                            }
                        }

                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                var recordedMessages: List<String>? = null
                                if (isRecording) {
                                    AudioRecorder.getInstance()?.startRecording(requireContext())
                                } else {
                                    val file = File(AudioRecorder.getInstance()?.stopRecording())

                                    recordedMessages =
                                        openAI.transcribeAudio(file, _userViewModel.getGptToken())

                                    withContext(Dispatchers.Main) {
                                        //println(messages)
                                        if (!recordedMessages.isNullOrEmpty()) {
                                            // The messages are not empty
                                            // Do something with the messages here
                                            val firstMessage = recordedMessages!![0]?.trim()
                                            if (firstMessage != null) {
                                                MessageCollector.addMessage(firstMessage)
                                            }

                                            val recordResponse =
                                                firstMessage?.let { actualMessage ->
                                                    ChatMessage(
                                                        actualMessage,
                                                        "me",
                                                        simulateTyping = false,
                                                        isShowShare = false
                                                    )
                                                }

                                            recordResponse?.let { actualResponse ->
                                                messageListAdapter.addMessage(
                                                    actualResponse
                                                )
                                            }
                                            deductCredit(recordCreditPrice)
                                            firstMessage?.let { actualFirstMessage ->
                                                startCompletion(
                                                    actualFirstMessage, completionCreditPrice!!
                                                )
                                            }
                                        } else {
                                            // The messages are empty
                                            // Handle this case here
                                            Log.d(
                                                TAG,
                                                "[${INNER_TAG}]: message reply is empty!"
                                            )
                                            showErrorDialog()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(
                                    TAG,
                                    "[${INNER_TAG}]: e: ${e.toString()}"
                                )
                                // Show dialog to notify user of error
                                withContext(Dispatchers.Main) {
                                    showErrorDialog()
                                }
                            }
                        }
                    } else {
                        // The messages are empty
                        // Handle this case here
                        UIHelper.getInstance().hideLoading()
                        showDialog("Sorry Buddy", getString(R.string.out_of_credit_info))
                    }
                } else {
                    showErrorDialog()
                }
            } else {
                Log.d(TAG, "[${INNER_TAG}]: permission not granted!")
            }
        }

        // Access other views using binding here
        sendButton.setOnClickListener {

            var remainingCredit = _userViewModel.getCredit()
            var isSubscribed = _userViewModel.getIsSubscribed()
            var completionCreditPrice = _userViewModel.getCompletionCreditPrice()

            Log.d(TAG, "[${INNER_TAG}]: check credit: ${remainingCredit}}!")
            if (remainingCredit != null && isSubscribed != null) {
                if (remainingCredit >= _userViewModel.getCompletionCreditPrice()!! || isSubscribed) {
                    // Send button click logic here
                    val messageText = messageInputField.text.toString()
                    if (!messageText.isNullOrEmpty() && !messageText.isBlank()) {
                        enableDisableRecordSend(false)
                        val message = ChatMessage(messageText, "me", false, false)
                        messageListAdapter.addMessage(message)
                        messageInputField.text?.clear()
                        UIHelper.getInstance().showLoading()
                        startCompletion(messageText, completionCreditPrice!!)
                    } else if (messageText.contains(" ")) {
                        showDialog("Hey Buddy", "Please enter a message!")
                    } else {
                        // The messages are empty
                        // Handle this case here
                        showDialog("Hey Buddy", "Please enter a message!")
                    }
                } else {
                    // The messages are empty
                    // Handle this case here
                    UIHelper.getInstance().hideLoading()
                    showDialog("Sorry Buddy", getString(R.string.out_of_credit_info))
                }
            } else {
                showErrorDialog()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)

        val switchMenuItem = menu.findItem(R.id.switch_toggle)

        switchMenuItem.actionView?.findViewById<SwitchCompat>(R.id.switch_toggle)
            ?.setOnCheckedChangeListener { _, isChecked ->
                // Handle the switch toggle event here
                Log.d(TAG, "[${INNER_TAG}]: switchMenuItem check isChecked: ${isChecked}}!")
                isEnableSpeaking = isChecked
                recordButton.isEnabled = isChecked
            }

        /*currentPrompt = getString(R.string.chatty)

        val spinnerItem = menu.findItem(R.id.action_spinner)
        val spinner = spinnerItem.actionView as AppCompatSpinner
        val choices = resources.getStringArray(R.array.choices_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_layout, choices)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val choice = choices[position]

                when (choice) {
                    "Chatty" -> {
                        currentPrompt = getString(R.string.chatty)
                        Log.d("SpinnerSelection", "Selected Chatty!")
                    }
                    "Direct to the point" -> {
                        currentPrompt = getString(R.string.direct_to_point)
                        Log.d("SpinnerSelection", "Selected Direct to the point!")
                    }
                    "Talking to kids" -> {
                        currentPrompt = getString(R.string.talking_to_kids)
                        Log.d("SpinnerSelection", "Selected Talking to kids!")
                    }
                    "As a Friend" -> {
                        currentPrompt = getString(R.string.as_a_friend)
                        Log.d("SpinnerSelection", "Selected As a Friend!")
                    }
                    "Markdown Format" -> {
                        currentPrompt = getString(R.string.format_markdown)
                        Log.d("SpinnerSelection", "Selected Markdown Format!")
                    }
                    "Punchy and Attention Grabber" -> {
                        currentPrompt = getString(R.string.punchy_and_attention_grabber)
                        Log.d("SpinnerSelection", "Selected Punchy and Attention Grabber!")
                    }
                    "Persuasive and storyteller" -> {
                        currentPrompt = getString(R.string.persuasive_and_story_teller)
                        Log.d("SpinnerSelection", "Selected Persuasive and storyteller!")
                    }
                    "Clear and easy" -> {
                        currentPrompt = getString(R.string.clear_and_easy)
                        Log.d("SpinnerSelection", "Selected Clear and easy!")
                    }
                    "Creative and descriptive" -> {
                        currentPrompt = getString(R.string.creative_and_descriptive)
                        Log.d("SpinnerSelection", "Selected Creative and descriptive!")
                    }
                    "Professional and informative" -> {
                        currentPrompt = getString(R.string.professional_and_informative)
                        Log.d("SpinnerSelection", "Selected Professional and informative!")
                    }
                    "Formal complex and in-depth" -> {
                        currentPrompt = getString(R.string.formal_complex_in_depth)
                        Log.d("SpinnerSelection", "Selected Formal complex and in-depth!")
                    }
                    "Summarize" -> {
                        currentPrompt = getString(R.string.summarize)
                        Log.d("SpinnerSelection", "Selected Summarize!")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }*/

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.v(TAG, "[${INNER_TAG}]: click back button")
                clearInput()
                requireActivity().onBackPressed() // Call onBackPressed() to handle the back button press in the fragment
                true
            }
            R.id.action_button -> {
                // Handle button click here
                Log.v(TAG, "[${INNER_TAG}]: click clear action bar button")
                if (sendButton.isEnabled) {
                    clearInput()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Call this method to hide the button
        _binding = null
        TextToSpeechHelper.getInstance().shutdown()
    }

    private fun startCompletion(messageText: String,completionCreditPrice:Long) {
        val openAI = OpenAIAPI(lifecycleScope, requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var messages: List<String>? = null

                if (!MessageCollector.getPreviousMessages().isNullOrEmpty()) {
                    if (openAI.isSummaryLengthValid(MessageCollector.getPreviousMessages())) {
                        messages = openAI.getCompletion(
                            messageText,
                            _userViewModel.getCurrentPrompt(),
                            MessageCollector.getPreviousMessages(),
                            _userViewModel.getGptToken()
                        )
                    } else {
                        var prevMessage = openAI.summarizeText(
                            MessageCollector.getPreviousMessages(),
                            _userViewModel.getGptToken()
                        ).toString()
                        messages = openAI.getCompletion(
                            messageText,
                            _userViewModel.getCurrentPrompt(),
                            prevMessage,
                            _userViewModel.getGptToken()
                        )
                    }
                } else {
                    messages = openAI.getCompletion(
                        messageText,
                        _userViewModel.getCurrentPrompt(),
                        null,
                        _userViewModel.getGptToken()
                    )
                }

                // Update UI with messages
                withContext(Dispatchers.Main) {
                    //println(messages)
                    if (!messages.isNullOrEmpty()) {
                        // The messages are not empty
                        // Do something with the messages here
                        val firstMessage = messages[0]?.trim()
                        // for collecting summary of chat or context of chat
                        // to prevent ai to tell unrelated answer
                        firstMessage?.let { MessageCollector.addMessage(it) }

                        UIHelper.getInstance().hideLoading()
                        val aiResponse =
                            firstMessage?.let { it1 ->
                                ChatMessage(
                                    it1,
                                    "AI",
                                    simulateTyping,
                                    true
                                )
                            }

                        // for adding the message to actual chat , for updating the chat message view
                        aiResponse?.let { messageListAdapter.addMessage(it) }
                        // call deduct credit here
                        deductCredit(completionCreditPrice)

                        if(isEnableSpeaking && !isSpeaking){
                            isSpeaking = true
                            enableDisableRecordSend(false)
                            firstMessage?.let { TextToSpeechHelper.getInstance().speak(it) }
                        }else{
                            enableDisableRecordSend(true)
                        }
                    } else {
                        // The messages are empty
                        // Handle this case here
                        showErrorDialog()
                    }
                }
            } catch (e: Exception) {
                // Show dialog to notify user of error
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    private fun deductCredit(creditPrice:Long) {
        var isSubscribed = _userViewModel.getIsSubscribed()
        if (!isSubscribed!!) {
            // deduct each time the ai reply when not subscribed
            var creditToDeduct =
                (creditPrice * -1).toInt()
            _userViewModel.getCredit()?.let { it1 ->
                userDb.updateCredit(
                    it1,
                    creditToDeduct,
                    callback = { newCredit, isSuccess ->
                        if (isSuccess) {
                            if (newCredit != null) {
                                _userViewModel.setCredit(
                                    newCredit
                                )
                            }
                            Log.d(
                                TAG,
                                "[${INNER_TAG}]: deduct credit success!"
                            )
                        } else {
                            Log.d(
                                TAG,
                                "[${INNER_TAG}]: deduct credit failed!"
                            )
                        }
                    })
            }
        } else {
            // user is subscribed no credit will be deducted
        }
    }

    private fun enableDisableRecordSend(enable: Boolean) {
        sendButton.isEnabled = enable
        if(isEnableSpeaking){
            recordButton.isEnabled = enable
        }
    }

    private fun showDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                sendButton.isEnabled = true
                dialog.dismiss()
            }
            .show()
    }

    private fun showErrorDialog() {
        UIHelper.getInstance().hideLoading()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Oops!")
            .setMessage("Something went wrong. Please try again later.")
            .setPositiveButton("OK") { dialog, _ ->
                enableDisableRecordSend(true)
                dialog.dismiss()
            }
            .show()
    }

    private fun generateRandomString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun showHideBottomNavigation(isShow: Boolean) {
        val bottomNav =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        if (isShow) {
            bottomNav?.visibility = View.VISIBLE
        } else {
            bottomNav?.visibility = View.GONE
        }
    }

    private fun showHideSideNavigation(isShow: Boolean) {
        val sideNavView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        if (isShow) {
            sideNavView?.visibility = View.VISIBLE
        } else {
            sideNavView?.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            chatScreenFragment = this@ChatScreenFragment
        }

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event here
                // For example, you can show a dialog or navigate to a different screen
                Log.v(TAG, "[${INNER_TAG}]: handleOnBackPressed event!!")
                clearInput()
                findNavController().navigate(R.id.action_chatScreenFragment_to_modeScreenFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onStart() {
        super.onStart()
        // show action bar without back button
        //UIHelper.getInstance().showHideActionBarWithoutBackButton(true,(requireActivity() as MainActivity).binding)
        UIHelper.getInstance().showHideActionBar(true, (requireActivity() as MainActivity).binding)
        showHideBottomNavigation(false)
        showHideSideNavigation(false)

        var actionTitle: String = ""
        when (_userViewModel.getCurrentPrompt()) {
            getString(R.string.email_mode) -> {
                actionTitle = "Create an Email"
                val message = ChatMessage(
                    "Please give me the necessary details about the email, and I'll assist you in creating it.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.report_mode) -> {
                actionTitle = "Create a Report"
                val message = ChatMessage(
                    "Please provide me with the necessary details about the report, and I'll assist you in creating it.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.twitter_post_mode) -> {
                actionTitle = "Create a Twitter Post"
                val message = ChatMessage(
                    "Please share the details with me, and I'll assist you in creating your Tweet post.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.facebook_post_mode) -> {
                actionTitle = "Create a Facebook Post"
                val message = ChatMessage(
                    "Please provide me with the necessary information so that I can assist you in creating your Facebook post.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.article_mode) -> {
                actionTitle = "Create an Article"
                val message = ChatMessage(
                    "Please give me the topic and specifics of your article, and I'll assist you in crafting it.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.contract_mode) -> {
                actionTitle = "Create simple contract"
                val message = ChatMessage(
                    "Please provide me with the essential information regarding your contract, so that I can create it for you.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.summarize_mode) -> {
                actionTitle = "Summarize Info"
                val message = ChatMessage(
                    "Please provide the information you would like me to simplify and summarize.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.general_assistant_mode) -> {
                actionTitle = "Ask anything you want?"
                val message = ChatMessage(
                    "Don't be shy! We'd love to hear from you and answer any questions you have.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.translator_mode) -> {
                actionTitle = "Translate Text"
                val message = ChatMessage(
                    "I can help you improve and clarify any text you provide, and translate it into several languages including Spanish, French, German, and more. Just let me know the text and the language you want me to translate it into.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
            getString(R.string.spelling_mode) -> {
                actionTitle = "Correct Spelling"
                val message = ChatMessage(
                    "I can help you improve and clarify any text you provide. Please type the text you would like me to review or edit.",
                    "AI",
                    false,
                    false
                )
                messageListAdapter.addMessage(message)
            }
        }

        UIHelper.getInstance().setActionBarTitle(actionTitle)
    }

    private fun clearInput() {
        messageListAdapter.clearMessages()
        MessageCollector.clearMessages()
    }

    fun signout() {
        var currentProvider: String? = null
        if (firebaseAuth.currentUser?.providerData?.size!! > 0) {
            //Prints Out google.com for Google Sign In, prints facebook.com for Facebook
            currentProvider =
                firebaseAuth.currentUser!!.providerData.get(firebaseAuth.currentUser!!.providerData.size - 1).providerId
            Log.v(TAG, "[${INNER_TAG}]: currentProvider ${currentProvider}")
        }

        when (currentProvider) {
            "facebook.com" -> {
                LoginManager.getInstance().logOut();
                Log.v(TAG, "[${INNER_TAG}]: facebook SignOut success!")
            }
            "google.com" -> {
                GoogleApi.getInstance()?.signOut {
                    Log.v(TAG, "[${INNER_TAG}]: google SignOut success!")
                }
            }
            else -> {
                Log.v(TAG, "[${INNER_TAG}]: provider not detected!")
            }
        }
        firebaseAuth.signOut()
    }

    private suspend fun searchGoogle(query: String): List<Triple<String, String, String>> {
        val decryptedApiKey = CryptoUtils.decrypt(
            _userViewModel.getEncryptedSearchApiKey(),
            _userViewModel.getSearchApiSecretKey()
        )
        return GoogleSearchAPI.getInstance().searchGoogle(
            query,
            decryptedApiKey,
            _userViewModel.getSearchEngineId(),
            _userViewModel.getSearchNumResults()!!
        )
    }

    fun checkIfReminder(input: String): Pair<Boolean, String?> {
        // Parse the input to get the reminder time and message
        val (reminderTime, reminderText) = reminderManager.parseReminderText(input)

        // If the reminder time was successfully parsed, set the reminder
        return if (reminderTime != null) {
            val title = "Reminder!"
            reminderManager.setReminder(reminderTime, title)

            // Print the confirmation message
            println(reminderText)
            Log.d(TAG, "[${INNER_TAG}]:checkIfReminder: ${reminderText}}!")
            Pair(true, reminderText)
        } else {
            // Print the error message
            println(reminderText)
            Log.d(TAG, "[${INNER_TAG}]:checkIfReminder: ${reminderText}}!")
            Pair(false, null)
        }
    }

    override fun onSpeechDone() {
        requireActivity().runOnUiThread {
            if(isSpeaking){
                enableDisableRecordSend(true)
                isSpeaking = false
            }
        }
    }
}