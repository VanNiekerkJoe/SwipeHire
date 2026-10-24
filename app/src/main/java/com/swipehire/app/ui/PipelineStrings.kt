package com.swipehire.app.ui

/**
 * Translations for the interview scheduler and pipeline tracker (user-defined
 * features 4 and 5). Kept separate from Localization.kt to avoid merge conflicts;
 * `tr()` falls back to these maps. Keys are the English strings used in the UI.
 * Have a first-language speaker review these before the final demo.
 */
internal val pipelineZu = mapOf(
    "Application status" to "Isimo sesicelo", "Matched" to "Ohambisanile", "Interview" to "Inhlolovo",
    "Offer" to "Isiphakamiso", "Hired" to "Uqashiwe", "Declined" to "Kunqatshiwe",
    "Proposed" to "Kuphakanyisiwe", "Accepted" to "Kwamukelwe", "Cancelled" to "Kukhanseliwe",
    "Schedule interview" to "Hlela inhlolovo", "Pick a date" to "Khetha usuku", "Pick a time" to "Khetha isikhathi",
    "Duration" to "Isikhathi", "minutes" to "amaminithi", "Format" to "Uhlobo",
    "In person" to "Ubuso nobuso", "Video call" to "Ikholi yevidiyo", "Phone call" to "Ikholi yocingo",
    "Note (optional)" to "Inothi (akubuyekezi)", "Send proposal" to "Thumela isiphakamiso",
    "Accept" to "Yamukela", "Decline" to "Yenqaba", "Add to calendar" to "Engeza kukhalenda",
    "Interviews" to "Izinhlolovo", "No interviews yet" to "Azikabikho izinhlolovo",
    "Move to interview" to "Yisa enhlolovweni", "Make offer" to "Nikeza isiphakamiso", "Reject" to "Yenqaba",
    "Withdraw" to "Hoxa", "Accept offer" to "Yamukela isiphakamiso",
    "Interview proposed" to "Inhlolovo iphakanyisiwe", "Interview accepted" to "Inhlolovo yamukelwe",
    "Interview declined" to "Inhlolovo yenqatshiwe", "Interview cancelled" to "Inhlolovo ikhanseliwe",
    "Application status updated" to "Isimo sesicelo sibuyekeziwe", "Now at stage" to "Manje esigabeni",
    "Pick a date first" to "Khetha usuku kuqala", "Pick a time first" to "Khetha isikhathi kuqala",
    "Pick a time at least 1 hour from now" to "Khetha isikhathi okungenani ihora elilodwa kusukela manje",
    "Duration must be 15–120 minutes" to "Isikhathi kumele sibe ngamaminithi angu-15 kuya ku-120",
    "This time clashes with another interview" to "Lesi sikhathi siphambana nenye inhlolovo",
    "Wait for a reply to your current proposal" to "Linda impendulo esiphakamisweni sakho samanje",
    "Only the company can propose interview times" to "Inkampani kuphela engaphakamisa izikhathi zenhlolovo",
    "Interviews can't be scheduled at this stage" to "Izinhlolovo azikwazi ukuhlelwa kulesi sigaba",
    "Something went wrong. Please try again." to "Kukhona okungahambanga kahle. Sicela uzame futhi."
)

internal val pipelineAf = mapOf(
    "Application status" to "Aansoekstatus", "Matched" to "Gepas", "Interview" to "Onderhoud",
    "Offer" to "Aanbod", "Hired" to "Aangestel", "Declined" to "Afgewys",
    "Proposed" to "Voorgestel", "Accepted" to "Aanvaar", "Cancelled" to "Gekanselleer",
    "Schedule interview" to "Skeduleer onderhoud", "Pick a date" to "Kies 'n datum", "Pick a time" to "Kies 'n tyd",
    "Duration" to "Duur", "minutes" to "minute", "Format" to "Formaat",
    "In person" to "Persoonlik", "Video call" to "Video-oproep", "Phone call" to "Foonoproep",
    "Note (optional)" to "Nota (opsioneel)", "Send proposal" to "Stuur voorstel",
    "Accept" to "Aanvaar", "Decline" to "Weier", "Add to calendar" to "Voeg by kalender",
    "Interviews" to "Onderhoude", "No interviews yet" to "Nog geen onderhoude nie",
    "Move to interview" to "Skuif na onderhoud", "Make offer" to "Maak aanbod", "Reject" to "Verwerp",
    "Withdraw" to "Onttrek", "Accept offer" to "Aanvaar aanbod",
    "Interview proposed" to "Onderhoud voorgestel", "Interview accepted" to "Onderhoud aanvaar",
    "Interview declined" to "Onderhoud geweier", "Interview cancelled" to "Onderhoud gekanselleer",
    "Application status updated" to "Aansoekstatus opgedateer", "Now at stage" to "Nou by stadium",
    "Pick a date first" to "Kies eers 'n datum", "Pick a time first" to "Kies eers 'n tyd",
    "Pick a time at least 1 hour from now" to "Kies 'n tyd minstens 1 uur van nou af",
    "Duration must be 15–120 minutes" to "Duur moet 15–120 minute wees",
    "This time clashes with another interview" to "Hierdie tyd bots met 'n ander onderhoud",
    "Wait for a reply to your current proposal" to "Wag vir 'n antwoord op jou huidige voorstel",
    "Only the company can propose interview times" to "Net die maatskappy kan onderhoudtye voorstel",
    "Interviews can't be scheduled at this stage" to "Onderhoude kan nie in hierdie stadium geskeduleer word nie",
    "Something went wrong. Please try again." to "Iets het skeefgeloop. Probeer asseblief weer."
)

internal val pipelineSt = mapOf(
    "Application status" to "Boemo ba kopo", "Matched" to "Ba tsamaellana", "Interview" to "Poledisano",
    "Offer" to "Tlhahiso", "Hired" to "O hirilwe", "Declined" to "E hannwe",
    "Proposed" to "E hlahiswe", "Accepted" to "E amohetswe", "Cancelled" to "E hlakotswe",
    "Schedule interview" to "Rala poledisano", "Pick a date" to "Kgetha letsatsi", "Pick a time" to "Kgetha nako",
    "Duration" to "Nako", "minutes" to "metsotso", "Format" to "Mokgwa",
    "In person" to "Ka sebele", "Video call" to "Mohala wa video", "Phone call" to "Mohala",
    "Note (optional)" to "Tlhokomediso (ha o rata)", "Send proposal" to "Romela tlhahiso",
    "Accept" to "Amohela", "Decline" to "Hana", "Add to calendar" to "Kenya kalendareng",
    "Interviews" to "Dipoledisano", "No interviews yet" to "Ha ho dipoledisano",
    "Move to interview" to "Fetela poledisanong", "Make offer" to "Etsa tlhahiso", "Reject" to "Hana",
    "Withdraw" to "Ikgele morao", "Accept offer" to "Amohela tlhahiso",
    "Interview proposed" to "Poledisano e hlahiswe", "Interview accepted" to "Poledisano e amohetswe",
    "Interview declined" to "Poledisano e hannwe", "Interview cancelled" to "Poledisano e hlakotswe",
    "Application status updated" to "Boemo ba kopo bo ntjhafaditswe", "Now at stage" to "Jwale ho mokgahlelo",
    "Pick a date first" to "Kgetha letsatsi pele", "Pick a time first" to "Kgetha nako pele",
    "Pick a time at least 1 hour from now" to "Kgetha nako e bonyane hora e le nngwe ho tloha jwale",
    "Duration must be 15–120 minutes" to "Nako e tshwanetse ho ba metsotso e 15–120",
    "This time clashes with another interview" to "Nako ena e kgahlana le poledisano e nngwe",
    "Wait for a reply to your current proposal" to "Letela karabo ya tlhahiso ya hao ya hajwale",
    "Only the company can propose interview times" to "Ke khamphani feela e ka hlahisang dinako tsa poledisano",
    "Interviews can't be scheduled at this stage" to "Dipoledisano ha di ka ralwa mokgahlelong ona",
    "Something went wrong. Please try again." to "Ho bile le bothata. Ka kopo leka hape."
)
