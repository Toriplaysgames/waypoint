package com.waypoint.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun QuestEditor(world: World, initial: QuestRecord?, seedPlace: Place?, template: String,
                         controller: QuestController, navigation: NavigationController, nav: NavState,
                         onSaved: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val busy by controller.busy.collectAsStateWithLifecycle()
    var id by rememberSaveable { mutableStateOf(initial?.id ?: UUID.randomUUID().toString()) }
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }
    var details by rememberSaveable { mutableStateOf(initial != null || template != "Everyday task") }
    var placeName by rememberSaveable { mutableStateOf(initial?.placeName ?: seedPlace?.name) }
    var placeAddress by rememberSaveable { mutableStateOf(initial?.placeAddress ?: seedPlace?.address) }
    var latitude by rememberSaveable { mutableStateOf(initial?.latitude ?: seedPlace?.point?.lat) }
    var longitude by rememberSaveable { mutableStateOf(initial?.longitude ?: seedPlace?.point?.lon) }
    var hasFocus by rememberSaveable { mutableStateOf(initial?.hasFocus ?: (template == "Focus session")) }
    var minutes by rememberSaveable { mutableStateOf((initial?.focusMinutes ?: 25).toString()) }
    var customMinutes by rememberSaveable { mutableStateOf(initial?.focusMinutes?.let { it !in listOf(15, 25, 45) } ?: false) }
    var reminder by rememberSaveable { mutableStateOf(initial?.reminderAt) }
    var characterName by rememberSaveable { mutableStateOf(initial?.characterName.orEmpty()) }
    var portrait by rememberSaveable { mutableStateOf(initial?.portrait ?: "scout") }
    var imageName by rememberSaveable { mutableStateOf(initial?.imageName) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    var lookup by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val dirty = title != initial?.title.orEmpty() || notes != initial?.notes.orEmpty() || placeName != initial?.placeName ||
        hasFocus != (initial?.hasFocus ?: false) || reminder != initial?.reminderAt || imageName != initial?.imageName ||
        characterName != initial?.characterName.orEmpty() || portrait != (initial?.portrait ?: "scout") || minutes != (initial?.focusMinutes ?: 25).toString()
    fun discardDraft() {
        if (imageName != initial?.imageName) imageName?.let { QuestImages.delete(context, it) }
        pendingPhoto?.let { QuestImages.delete(context, it) }
        onClose()
    }
    fun close() { if (dirty) discard = true else discardDraft() }
    BackHandler { if (lookup) lookup = false else close() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            photoBusy = true
            try { pendingPhoto = QuestImages.import(context, uri) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: tr("This photo could not be opened. Try another.","לא הצלחנו לפתוח את התמונה. נסו תמונה אחרת.") }
            finally { photoBusy = false }
        }
    }
    fun chooseReminder() {
        val calendar = Calendar.getInstance().apply { timeInMillis = reminder ?: (System.currentTimeMillis() + 60 * 60_000L) }
        DatePickerDialog(context, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) error = tr("Choose a reminder time in the future.","בחרו שעת תזכורת עתידית.")
                else reminder = calendar.timeInMillis
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(context)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    if (lookup) {
        QuestPlacePicker(nav, navigation, onPick = { place ->
            placeName = place.name; placeAddress = place.address; latitude = place.point.lat; longitude = place.point.lon; lookup = false
        }, onBack = { lookup = false })
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (initial == null) tr("Add a quest","הוספת משימה") else tr("Edit quest","עריכת המשימה")) },
        navigationIcon = { IconButton(onClick = ::close, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back to quests","חזרה למשימות")) } }) },
        bottomBar = {
            Surface {
                Box(Modifier.fillMaxWidth().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        val duration = minutes.toIntOrNull()
                        if (validQuestTitle(title) == null) error = tr("Give your quest a title of up to 160 characters.","תנו למשימה כותרת של עד 160 תווים.")
                        else if (duration == null || duration !in 1..180) error = tr("Choose a timer from 1 to 180 minutes.","בחרו טיימר של 1 עד 180 דקות.")
                        else controller.save(QuestRecord(id = id, title = title, notes = notes, placeName = placeName, placeAddress = placeAddress,
                            latitude = latitude, longitude = longitude, hasFocus = hasFocus, focusMinutes = duration, reminderAt = reminder,
                            characterName = characterName, portrait = portrait, imageName = imageName), onSaved)
                    }, enabled = !busy && !photoBusy && title.isNotBlank(), modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 52.dp), shape = hudShape(world)) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(if (initial == null) tr("Add quest","הוספת המשימה") else tr("Save changes","שמירת השינויים"))
                    }
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(tr("What would you like to do?","מה בא לך לעשות?"), style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(title, onValueChange = { if (it.length <= 160) title = it }, label = { Text(tr("Quest title","כותרת המשימה")) },
                    placeholder = { Text(when (template) { "Visit a place" -> tr("Visit the library","ביקור בספרייה"); "Focus session" -> tr("Read for a while","קריאה לזמן מה"); else -> tr("Pick up groceries","קניות במכולת") }) },
                    modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3, supportingText = { Text(tr("A title is all you need.","כל מה שצריך הוא כותרת.")) })
                TextButton(onClick = { details = !details }) { Icon(if (details) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null); Text(if (details) tr("Hide details","הסתרת הפרטים") else tr("Add details","הוספת פרטים")) }
                if (details) {
                    OutlinedTextField(notes, onValueChange = { if (it.length <= 4000) notes = it }, label = { Text(tr("Notes (optional)","הערות (לא חובה)")) }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Place","מקום"), style = MaterialTheme.typography.titleLarge)
                        if (placeName != null) { Text(placeName!!); Text(placeAddress.orEmpty(), style = MaterialTheme.typography.bodySmall) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { lookup = true }) { Icon(Icons.Default.Place, null); Text(if (placeName == null) tr("Choose a place","בחירת מקום") else tr("Change place","החלפת המקום")) }
                            if (placeName != null) TextButton(onClick = { placeName = null; placeAddress = null; latitude = null; longitude = null }) { Text(tr("Remove place","הסרת המקום")) }
                        }
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Reminder","תזכורת"), style = MaterialTheme.typography.titleLarge)
                        if (reminder != null) Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(reminder!!)))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = ::chooseReminder) { Text(if (reminder == null) tr("Choose date and time","בחירת תאריך ושעה") else tr("Change reminder","שינוי התזכורת")) }
                            if (reminder != null) TextButton(onClick = { reminder = null }) { Text(tr("Remove reminder","הסרת התזכורת")) }
                        }
                        if (reminder != null) QuestAlertAccess(controller)
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(tr("Focus timer","טיימר ריכוז"), style = MaterialTheme.typography.titleLarge); Text(tr("Make a little time for this task.","להקדיש למשימה קצת זמן."), style = MaterialTheme.typography.bodySmall) }
                        Switch(hasFocus, onCheckedChange = { hasFocus = it }, modifier = Modifier.semantics { contentDescription = tr("Focus timer","טיימר ריכוז") })
                    }
                    if (hasFocus) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45).forEach { value -> FilterChip(selected = !customMinutes && minutes == value.toString(), onClick = { customMinutes = false; minutes = value.toString() }, label = { Text(tr("$value min","$value דק׳")) }) }
                            FilterChip(selected = customMinutes, onClick = { customMinutes = true }, label = { Text(tr("Custom","מותאם אישית")) })
                        }
                        if (customMinutes) OutlinedTextField(minutes, onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) minutes = it }, label = { Text(tr("Minutes (1–180)","דקות (1–180)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider()
                    Text(tr("Give your quest a character","דמות למשימה"), style = MaterialTheme.typography.titleLarge)
                    Text(tr("Choose a name and picture to appear on this task. Both are optional.","בחרו שם ותמונה שיופיעו במשימה. שניהם לא חובה."), style = MaterialTheme.typography.bodyMedium)
                    QuestPortrait(QuestRecord(title = title, characterName = characterName, portrait = portrait, imageName = imageName), Modifier.size(80.dp))
                    OutlinedTextField(characterName, onValueChange = { if (it.length <= 60) characterName = it }, label = { Text(tr("Character name (optional)","שם הדמות (לא חובה)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("scout", "guide", "companion").forEach { option -> FilterChip(selected = portrait == option && imageName == null, onClick = {
                            if (imageName != initial?.imageName) imageName?.let { QuestImages.delete(context, it) }; imageName = null; portrait = option
                        }, label = { Text(option.replaceFirstChar { it.titlecase() }) }) }
                    }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !photoBusy) { Text(if (photoBusy) tr("Opening photo…","פותחים את התמונה…") else tr("Choose a photo","בחירת תמונה")) }
                    Text(tr("Your picture stays on this phone.","התמונה שלך נשארת בטלפון הזה."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (pendingPhoto != null) AlertDialog(onDismissRequest = { pendingPhoto?.let { QuestImages.delete(context, it) }; pendingPhoto = null },
        title = { Text(tr("Use this crop?","להשתמש בחיתוך הזה?")) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuestPortrait(QuestRecord(title = tr("Photo preview","תצוגה מקדימה של התמונה"), imageName = pendingPhoto), Modifier.fillMaxWidth().aspectRatio(1f))
            Text(tr("The centre of your photo will appear in the character portrait.","מרכז התמונה יופיע בדיוקן הדמות."))
        } }, confirmButton = { TextButton(onClick = {
            if (imageName != initial?.imageName) imageName?.let { QuestImages.delete(context, it) }
            imageName = pendingPhoto; pendingPhoto = null
        }) { Text(tr("Use photo","שימוש בתמונה")) } }, dismissButton = { TextButton(onClick = { pendingPhoto?.let { QuestImages.delete(context, it) }; pendingPhoto = null }) { Text(tr("Cancel","ביטול")) } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(tr("Leave without saving?","לצאת בלי לשמור?")) }, text = { Text(tr("Your changes to this quest will be discarded.","השינויים שביצעת במשימה יימחקו.")) },
        confirmButton = { TextButton(onClick = ::discardDraft) { Text(tr("Discard changes","ביטול השינויים")) } }, dismissButton = { TextButton(onClick = { discard = false }) { Text(tr("Keep editing","המשך עריכה")) } })
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text(tr("Check this detail","כדאי לבדוק את הפרט הזה")) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text(tr("OK","אישור")) } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestPlacePicker(nav: NavState, navigation: NavigationController, onPick: (Place) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { navigation.search("") }
    BackHandler(onBack = onBack)
    Scaffold(topBar = { TopAppBar(title = { Text(tr("Choose a place","בחירת מקום")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back to quest","חזרה למשימה")) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(query, onValueChange = { query = it.take(256); navigation.search(query) }, label = { Text(tr("Place or address","מקום או כתובת")) }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            if (query.isBlank()) {
                item { Text(tr("Saved places","מקומות שמורים"), style = MaterialTheme.typography.titleLarge) }
                if (nav.saved.isEmpty()) item { Text(tr("No saved places yet. Search for a place above.","עדיין אין מקומות שמורים. חפשו מקום למעלה.")) }
                items(nav.saved, key = { it.id }) { place -> QuestPlaceRow(place, onPick) }
            } else {
                if (nav.searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                nav.searchError?.let { item { Text(tr("Search is unavailable. Try again, or choose a saved place.","החיפוש אינו זמין. נסו שוב, או בחרו מקום שמור.")) } }
                if (!nav.searching && nav.searchResults.isEmpty() && query.length >= 2) item { Text(tr("No places found. Try a street or a nearby town.","לא נמצאו מקומות. נסו רחוב או יישוב סמוך.")) }
                items(nav.searchResults, key = { it.id }) { place -> QuestPlaceRow(place, onPick) }
            }
        }
    }
}

@Composable private fun QuestPlaceRow(place: Place, onPick: (Place) -> Unit) {
    ListItem(headlineContent = { Text(place.name) }, supportingContent = { Text(place.shownAddress) },
        leadingContent = { Icon(Icons.Default.Place, null) },
        modifier = Modifier.fillMaxWidth().clickable { onPick(place) })
}
