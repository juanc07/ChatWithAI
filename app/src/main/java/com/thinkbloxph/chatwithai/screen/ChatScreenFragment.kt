package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
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
    private var userDb:UserDatabase? = null
    private var simulateTyping: Boolean = false

    private var isEnableSpeaking: Boolean = false
    private var isSpeaking: Boolean = false
    private lateinit var reminderManager: ReminderManager
    private lateinit var callback: OnBackPressedCallback
    private var isRecording = false


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
                var searchCreditPrice = _userViewModel.getCompletionCreditPrice()

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

                                            if(_userViewModel.getEnableSearch() == true && GoogleSearchAPI.getInstance().containsSearchKeyword(firstMessage!!) && _userViewModel.getCurrentPrompt() == getString(R.string.search_mode)){
                                                Log.d(TAG, "[${INNER_TAG}]:recordButton detect searching mode!!")
                                                startSearch(firstMessage, searchCreditPrice!!)
                                            }else{
                                                Log.d(TAG, "[${INNER_TAG}]:recordButton detect none searching mode!!")
                                                firstMessage?.let { actualFirstMessage ->
                                                    startCompletion(
                                                        actualFirstMessage, completionCreditPrice!!
                                                    )
                                                }
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
            var searchCreditPrice = _userViewModel.getCompletionCreditPrice()

            Log.d(TAG, "[${INNER_TAG}]: check credit: ${remainingCredit}}!")
            if (remainingCredit != null && isSubscribed != null) {
                if (remainingCredit >= _userViewModel.getCompletionCreditPrice()!! || isSubscribed) {

                    // Send button click logic here
                    val messageText = messageInputField.text.toString()
                    var message: ChatMessage? = null
                    if (!messageText.isNullOrEmpty() && !messageText.isBlank()) {
                        message = ChatMessage(messageText, "me", false, false)
                        messageListAdapter.addMessage(message!!)
                        messageInputField.text?.clear()
                        try {
                            enableDisableRecordSend(false)
                            val (isReminder, reminderText) = checkIfReminder(messageText)
                            if(isReminder){
                                outputAIMessage(listOf(reminderText.toString()), completionCreditPrice!!)
                            }else{
                                if(reminderManager.containsStopAlarmKeyword(messageText)){
                                    reminderManager.cancelAlarm(1)
                                }else{
                                    if(_userViewModel.getEnableSearch() == true && GoogleSearchAPI.getInstance().containsSearchKeyword(messageText) && _userViewModel.getCurrentPrompt() == getString(R.string.search_mode) ){
                                        Log.d(TAG, "[${INNER_TAG}]:sendButton detect searching mode!!")
                                        startSearch(messageText, searchCreditPrice!!)
                                    }else{
                                        Log.d(TAG, "[${INNER_TAG}]:sendButton detect none searching mode!!")
                                        UIHelper.getInstance().showLoading()
                                        startCompletion(messageText, completionCreditPrice!!)
                                    }
                                }
                            }
                        }catch (e:Exception){
                            Log.d(
                                TAG,
                                "[${INNER_TAG}]: sendButton e: $e"
                            )
                            showErrorDialog()
                        }
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

    private fun outputAIMessage(messages: List<String>?, creditPrice:Long){
        if (!messages.isNullOrEmpty()) {
            // The messages are not empty
            // Do something with the messages here
            val firstMessage = messages[0]?.trim()
            if (firstMessage != null) {
                MessageCollector.addMessage(firstMessage)
            }

            val aiResponse =
                firstMessage?.let { it1 ->
                    ChatMessage(
                        it1,
                        "AI",
                        false,
                        true
                    )
                }
            if (aiResponse != null) {
                messageListAdapter.addMessage(aiResponse)
            }

            deductCredit(creditPrice)
            UIHelper.getInstance().hideLoading()
            enableDisableRecordSend(true)
        }
    }

    fun startSearch(messageText: String,completionCreditPrice:Long){
        lifecycleScope.launch {
            var messages: List<String>? = null
            val searchResults = searchGoogle(messageText)
            Log.d(
                TAG,
                "[${INNER_TAG}]: searchResults $searchResults"
            )

            val clickableResults = mutableListOf<SpannableString>()
            for (result in searchResults) {
                val clickableResult = GoogleSearchAPI.getInstance().makeClickableUrls(result)
                clickableResults.add(clickableResult)
            }

            messages = listOf(TextUtils.join("\n\n", clickableResults))

            withContext(Dispatchers.Main) {
                outputAIMessage(messages,completionCreditPrice)
               /* if (!messages.isNullOrEmpty()) {
                    val firstMessage = messages[0]?.trim()
                    if (firstMessage != null) {
                        MessageCollector.addMessage(firstMessage)
                    }

                    val aiResponse =
                        firstMessage?.let { it1 ->
                            ChatMessage(
                                it1,
                                "AI",
                                false,
                                true
                            )
                        }
                    if (aiResponse != null) {
                        messageListAdapter.addMessage(aiResponse)
                    }

                    deductCredit(completionCreditPrice)

                    UIHelper.getInstance().hideLoading()
                    enableDisableRecordSend(true)
                }*/
            }
        }
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
                            _userViewModel.getGptToken(),
                            _userViewModel.getGptModel()
                        )
                    } else {
                        var prevMessage = openAI.summarizeText(
                            MessageCollector.getPreviousMessages(),
                            _userViewModel.getGptToken(),
                            _userViewModel.getGptModel()
                        ).toString()
                        messages = openAI.getCompletion(
                            messageText,
                            _userViewModel.getCurrentPrompt(),
                            prevMessage,
                            _userViewModel.getGptToken(),
                            _userViewModel.getGptModel()
                        )
                    }
                } else {
                    messages = openAI.getCompletion(
                        messageText,
                        _userViewModel.getCurrentPrompt(),
                        null,
                        _userViewModel.getGptToken(),
                        _userViewModel.getGptModel()
                    )
                }

                // Update UI with messages
                withContext(Dispatchers.Main) {
                    Log.v(TAG, "[${INNER_TAG}]: start complete result: $messages")
                    if (!messages.isNullOrEmpty()) {
                        // The messages are not empty
                        // Do something with the messages here
                        val firstMessage = messages[0]?.trim()
                        Log.v(TAG, "[${INNER_TAG}]: start complete firstMessage: $firstMessage")
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
                        Log.v(TAG, "[${INNER_TAG}]: start complete result: empty!")
                        showErrorDialog()
                    }
                }
            } catch (e: Exception) {
                // Show dialog to notify user of error
                Log.v(TAG, "[${INNER_TAG}]: exception error:  $e")
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
                userDb?.updateCredit(
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

        userDb = UserDatabase(_userViewModel.getDefaultDBUrl())

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
                sendAIHintMessage("Please give me the necessary details about the email, and I'll assist you in creating it.")
            }
            getString(R.string.report_mode) -> {
                actionTitle = "Create a Report"
                sendAIHintMessage("Please provide me with the necessary details about the report, and I'll assist you in creating it.")
            }
            getString(R.string.twitter_post_mode) -> {
                actionTitle = "Create a Twitter Post"
                sendAIHintMessage("Please share the details with me, and I'll assist you in creating your Tweet post.")
            }
            getString(R.string.facebook_post_mode) -> {
                actionTitle = "Create a Facebook Post"
                sendAIHintMessage("Please provide me with the necessary information so that I can assist you in creating your Facebook post.")
            }
            getString(R.string.article_mode) -> {
                actionTitle = "Create an Article"
                sendAIHintMessage("Please give me the topic and specifics of your article, and I'll assist you in crafting it.")
            }
            getString(R.string.contract_mode) -> {
                actionTitle = "Create simple contract"
                sendAIHintMessage("Please provide me with the essential information regarding your contract, so that I can create it for you.")
            }
            getString(R.string.summarize_mode) -> {
                actionTitle = "Summarize Info"
                sendAIHintMessage("Please provide the information you would like me to simplify and summarize.")
            }
            getString(R.string.general_assistant_mode) -> {
                actionTitle = "Ask anything you want?"
                sendAIHintMessage("Don't be shy! We'd love to hear from you and answer any questions you have.")
            }
            getString(R.string.translator_mode) -> {
                actionTitle = "Translate Text"
                sendAIHintMessage("I can help you improve and clarify any text you provide, and translate it into several languages including Spanish, French, German, and more. Just let me know the text and the language you want me to translate it into.")
            }
            getString(R.string.spelling_mode) -> {
                actionTitle = "Correct Spelling"
                sendAIHintMessage("I can help you improve and clarify any text you provide. Please type the text you would like me to review or edit.")
            }
            getString(R.string.search_mode) -> {
                actionTitle = "Search"
                sendAIHintMessage("If you'd like, I can assist you with conducting a search on a topic, and then you can ask me any questions you may have about it.")
            }
            getString(R.string.product_description_mode) -> {
                actionTitle = "Create Product Info"
                sendAIHintMessage("Please share the necessary information about your product with me so that I can create it according to your needs.")
            }
            getString(R.string.english_teacher_mode) -> {
                actionTitle = "English Teacher"
                sendAIHintMessage("If you have any questions related to English, feel free to ask and I will provide explanations and teach you.")
            }
            getString(R.string.science_teacher_mode) -> {
                actionTitle = "Science Teacher"
                sendAIHintMessage("If you have any questions related to science, feel free to ask and I will be happy to provide you with explanations and teach you.")
            }
            getString(R.string.father_mode) -> {
                actionTitle = "Father Advise"
                sendAIHintMessage("I'll be playing the role of your father today, so if you have any problems, concerns, or questions, feel free to share them with me.")
            }
            getString(R.string.mother_mode) -> {
                actionTitle = "Mother Advise"
                sendAIHintMessage("Dear child, I'll be acting as your mother for today. If you have any problems, concerns, or questions, feel free to share them with me.")
            }
        }

        UIHelper.getInstance().setActionBarTitle(actionTitle)
    }

    fun sendAIHintMessage(hintMessage:String){
        if(messageListAdapter.itemCount == 0){
            val message = ChatMessage(
                hintMessage,
                "AI",
                false,
                false
            )
            messageListAdapter.addMessage(message)
        }
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

    private suspend fun searchGoogle(query: String): List<Triple<String, String,String>> {
        val decryptedApiKey = CryptoUtils.decrypt(_userViewModel.getEncryptedSearchApiKey(), _userViewModel.getSearchApiSecretKey())
        return GoogleSearchAPI.getInstance().searchGoogle(query, decryptedApiKey, _userViewModel.getSearchEngineId(),_userViewModel.getSearchNumResults()!!)
    }

    fun checkIfReminder(input: String): Pair<Boolean, String?> {
        // Parse the input to get the reminder time and message
        val (reminderTime, reminderText) = reminderManager.parseReminderText(input)

        // If the reminder time was successfully parsed, set the reminder
        return if (reminderTime != null) {
            val title = "Reminder!"
            reminderManager.setReminder(reminderTime, title)

            // Print the confirmation message
            Log.d(TAG, "[${INNER_TAG}]:reminderTime: ${reminderTime}}!")
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