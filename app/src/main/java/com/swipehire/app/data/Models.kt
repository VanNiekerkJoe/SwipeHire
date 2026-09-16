package com.swipehire.app.data

/** The two account types SwipeHire supports on one shared UI. */
enum class AccountType { STUDENT, COMPANY }

/** How a role/job is worked — filterable on Discover, shown on every card. */
enum class RemoteType(val label: String) {
    ON_SITE("On-site"),
    HYBRID("Hybrid"),
    REMOTE("Remote")
}

/** A job posting card shown to a student while swiping. */
data class JobPosting(
    val id: String,
    val company: String,
    val role: String,
    val location: String,
    val workAddress: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val blurb: String,
    val logoInitials: String,
    val remoteType: RemoteType,
    val salaryRange: String,
    /** Overlap between this role's tags and the viewer's own skills — the "why we matched" signal. */
    val matchedSkills: List<String>,
    /** Mock stand-in for "the other side already liked you back" — drives the match celebration. */
    val willMatch: Boolean
)

/** A student profile card shown to a company while swiping. */
data class StudentProfile(
    val id: String,
    val name: String,
    val course: String,
    val year: String,
    val skills: List<String>,
    val blurb: String,
    val avatarInitials: String,
    /** Overlap between this student's skills and what the company is hiring for. */
    val matchedSkills: List<String>,
    val willMatch: Boolean
)

data class ChatMessage(
    val id: String,
    val text: String,
    val fromMe: Boolean
)

/** A mutual match — company + student both swiped right, so a chat opened up. */
data class MatchChat(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarInitials: String,
    val lastMessage: String,
    val unread: Boolean,
    val messages: List<ChatMessage>
)

object MockData {

    val jobPostings = listOf(
        JobPosting(
            id = "j1", company = "Nexora Fintech", role = "Junior Backend Developer",
            location = "Sandton, Johannesburg · Hybrid",
            workAddress = "123 Rivonia Road, Sandton",
            latitude = -26.1076, longitude = 28.0567,
            tags = listOf("C#", ".NET", "SQL"),
            blurb = "Join our payments team building secure, high-throughput APIs for the South African market.",
            logoInitials = "NX",
            remoteType = RemoteType.HYBRID,
            salaryRange = "R18k – R24k / month",
            matchedSkills = listOf("C#", "SQL"),
            willMatch = true
        ),
        JobPosting(
            id = "j2", company = "Kestrel Cloud", role = "Graduate Software Engineer",
            location = "Cape Town · Remote",
            workAddress = "12 Bree Street, Cape Town",
            latitude = -33.9249, longitude = 18.4241,
            tags = listOf("Kotlin", "Android", "Azure"),
            blurb = "Ship native Android features for a fast-growing logistics platform used across Africa.",
            logoInitials = "KC",
            remoteType = RemoteType.REMOTE,
            salaryRange = "R22k – R28k / month",
            matchedSkills = listOf("Kotlin"),
            willMatch = true
        ),
        JobPosting(
            id = "j3", company = "Veridian Security", role = "Cybersecurity Intern",
            location = "Roodepoort · On-site",
            workAddress = "45 Ontdekkers Road, Roodepoort",
            latitude = -26.1006, longitude = 27.8563,
            tags = listOf("Networking", "SOC", "Python"),
            blurb = "Support our blue team with monitoring, incident response, and vulnerability triage.",
            logoInitials = "VS",
            remoteType = RemoteType.ON_SITE,
            salaryRange = "R15k – R19k / month",
            matchedSkills = emptyList(),
            willMatch = false
        ),
        JobPosting(
            id = "j4", company = "Loop Studio", role = "Frontend Developer",
            location = "Pretoria · Hybrid",
            workAddress = "88 Lynnwood Road, Pretoria",
            latitude = -25.7479, longitude = 28.2293,
            tags = listOf("React", "TypeScript", "UI/UX"),
            blurb = "Craft polished, accessible interfaces for a design-led product studio.",
            logoInitials = "LS",
            remoteType = RemoteType.HYBRID,
            salaryRange = "R20k – R26k / month",
            matchedSkills = emptyList(),
            willMatch = false
        ),
        JobPosting(
            id = "j5", company = "Atlas Retail Group", role = "IT Support Intern",
            location = "Constantia Kloof, Roodepoort · On-site",
            workAddress = "7 Christiaan de Wet Road, Constantia Kloof",
            latitude = -26.1244, longitude = 27.9204,
            tags = listOf("Windows", "Networking", "Helpdesk"),
            blurb = "Keep store systems running across our Gauteng retail branches.",
            logoInitials = "AR",
            remoteType = RemoteType.ON_SITE,
            salaryRange = "R12k – R15k / month",
            matchedSkills = emptyList(),
            willMatch = false
        ),
    )

    val studentProfiles = listOf(
        StudentProfile(
            id = "s1", name = "Amahle Dlamini", course = "BSc Computer Science", year = "Final year",
            skills = listOf("Java", "Kotlin", "REST APIs"),
            blurb = "Built two published Android apps; peer tutor for first-year programming.",
            avatarInitials = "AD",
            matchedSkills = listOf("Kotlin"),
            willMatch = true
        ),
        StudentProfile(
            id = "s2", name = "Sipho Nkosi", course = "BCAD Application Development", year = "Final year",
            skills = listOf("C#", ".NET", "Azure"),
            blurb = "Interned on a cloud-hosted event management platform; loves clean architecture.",
            avatarInitials = "SN",
            matchedSkills = listOf("C#", ".NET"),
            willMatch = true
        ),
        StudentProfile(
            id = "s3", name = "Lerato Mokoena", course = "BSc Information Systems", year = "3rd year",
            skills = listOf("SQL", "Data Analysis", "Python"),
            blurb = "Freelance data cleanup projects; comfortable turning messy data into insight.",
            avatarInitials = "LM",
            matchedSkills = emptyList(),
            willMatch = false
        ),
        StudentProfile(
            id = "s4", name = "Junaid Patel", course = "BSc Computer Science", year = "Final year",
            skills = listOf("Security", "Kotlin", "Networking"),
            blurb = "Runs a small cybersecurity portfolio of open-source scanning tools.",
            avatarInitials = "JP",
            matchedSkills = listOf("Kotlin"),
            willMatch = true
        ),
    )

    val matches = listOf(
        MatchChat(
            id = "m1", name = "Kestrel Cloud", subtitle = "Graduate Software Engineer",
            avatarInitials = "KC", lastMessage = "Great, let's set up a call this week!", unread = true,
            messages = listOf(
                ChatMessage("1", "Hi! We loved your profile, congrats on the match.", false),
                ChatMessage("2", "Thank you, really excited about the role!", true),
                ChatMessage("3", "Great, let's set up a call this week!", false),
            )
        ),
        MatchChat(
            id = "m2", name = "Amahle Dlamini", subtitle = "BSc Computer Science · Final year",
            avatarInitials = "AD", lastMessage = "Sent over my portfolio link, let me know!", unread = false,
            messages = listOf(
                ChatMessage("1", "Hi, thanks for the match! Here's my portfolio.", true),
                ChatMessage("2", "Sent over my portfolio link, let me know!", true),
            )
        ),
        MatchChat(
            id = "m3", name = "Nexora Fintech", subtitle = "Junior Backend Developer",
            avatarInitials = "NX", lastMessage = "We'll be in touch after the panel review.", unread = false,
            messages = listOf(
                ChatMessage("1", "Thanks for applying, panel review is Friday.", false),
                ChatMessage("2", "We'll be in touch after the panel review.", false),
            )
        ),
    )
}
