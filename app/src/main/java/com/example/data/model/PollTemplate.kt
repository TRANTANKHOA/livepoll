package com.example.data.model

data class TemplateOption(
    val text: String,
    val subtitle: String? = null,
    val dateTimeSlot: String? = null,
    val venueAddress: String? = null,
    val duration: String? = null,
    val priceRating: String? = null
)

data class PollTemplate(
    val id: String,
    val title: String,
    val category: String,
    val categoryIcon: String,
    val description: String,
    val allowMultipleChoices: Boolean,
    val allowCustomOptions: Boolean,
    val isAnonymous: Boolean,
    val defaultDeadlineHours: Int?,
    val defaultTargetHeadcount: Int?,
    val defaultGroupId: String? = null,
    val defaultGroupName: String? = null,
    val defaultOptions: List<TemplateOption>
)

object PollTemplates {
    val templates: List<PollTemplate> = listOf(
        PollTemplate(
            id = "template_soccer",
            title = "⚽ Weekend 5v5 Soccer: Match Time & Pitch",
            category = "SOCCER",
            categoryIcon = "⚽",
            description = "Let's lock in the pitch location, kickoff time, and duration for our weekly soccer match!",
            allowMultipleChoices = true,
            allowCustomOptions = true,
            isAnonymous = false,
            defaultDeadlineHours = 6,
            defaultTargetHeadcount = 10,
            defaultGroupId = "group_soccer",
            defaultGroupName = "Weekend Soccer Squad ⚽",
            defaultOptions = listOf(
                TemplateOption(
                    text = "Riverside Turf Arena (Pitch 2)",
                    subtitle = "Turf grass, floodlights, parking available",
                    dateTimeSlot = "Saturday 4:00 PM - 5:30 PM",
                    venueAddress = "240 River Road Field, South Park",
                    duration = "90 mins",
                    priceRating = "$12 / player"
                ),
                TemplateOption(
                    text = "Central Park Community Pitch",
                    subtitle = "Natural grass, public access, BYO bibs",
                    dateTimeSlot = "Saturday 5:30 PM - 7:00 PM",
                    venueAddress = "Central Park Field 4",
                    duration = "90 mins",
                    priceRating = "Free"
                ),
                TemplateOption(
                    text = "Metro Sports Dome (Indoor)",
                    subtitle = "Indoor air-conditioned turf, cleats allowed",
                    dateTimeSlot = "Sunday 10:00 AM - 11:30 AM",
                    venueAddress = "88 Olympic Way, Midtown",
                    duration = "90 mins",
                    priceRating = "$15 / player"
                ),
                TemplateOption(
                    text = "Highland Soccer Complex",
                    subtitle = "Full-sized goals, warm-up cage included",
                    dateTimeSlot = "Sunday 4:00 PM - 5:30 PM",
                    venueAddress = "1200 Highland Hills Blvd",
                    duration = "90 mins",
                    priceRating = "$10 / player"
                )
            )
        ),
        PollTemplate(
            id = "template_drinks",
            title = "🍻 Friday Post-Work Drinks & Happy Hour",
            category = "DRINKS",
            categoryIcon = "🍻",
            description = "Celebrating sprint complete! Vote on the bar/lounge and check in your RSVP.",
            allowMultipleChoices = true,
            allowCustomOptions = true,
            isAnonymous = false,
            defaultDeadlineHours = 4,
            defaultTargetHeadcount = 12,
            defaultGroupId = "group_social",
            defaultGroupName = "Friday Social Club 🍻",
            defaultOptions = listOf(
                TemplateOption(
                    text = "The Rusty Anchor Rooftop",
                    subtitle = "Great skyline view, $6 draft beers till 7 PM",
                    dateTimeSlot = "Friday 6:00 PM onwards",
                    venueAddress = "500 High St, 8th Floor Rooftop",
                    duration = "2.5 hours",
                    priceRating = "$$"
                ),
                TemplateOption(
                    text = "Copper & Oak Craft Brewery",
                    subtitle = "Large outdoor beer garden, board games & food truck",
                    dateTimeSlot = "Friday 6:30 PM onwards",
                    venueAddress = "72 Industrial Ave, Arts District",
                    duration = "3 hours",
                    priceRating = "$"
                ),
                TemplateOption(
                    text = "Neon Social Lounge & Arcade",
                    subtitle = "2-for-1 cocktails, retro pinball & pool tables",
                    dateTimeSlot = "Friday 7:00 PM onwards",
                    venueAddress = "18 Market Square, Downtown",
                    duration = "2 hours",
                    priceRating = "$$"
                ),
                TemplateOption(
                    text = "Botanical Gin & Tapas Bar",
                    subtitle = "Cozy patio seating, mocktail menu available",
                    dateTimeSlot = "Friday 6:00 PM onwards",
                    venueAddress = "310 Garden Lane",
                    duration = "2 hours",
                    priceRating = "$$$"
                )
            )
        ),
        PollTemplate(
            id = "template_lunch",
            title = "🍕 Team Lunch & Venue Selection",
            category = "FOOD",
            categoryIcon = "🍕",
            description = "Quick vote for today's team lunch spot. Dietary requirements welcome in comments!",
            allowMultipleChoices = false,
            allowCustomOptions = true,
            isAnonymous = false,
            defaultDeadlineHours = 2,
            defaultTargetHeadcount = 8,
            defaultGroupId = "group_eng",
            defaultGroupName = "Product & Engineering Crew 💻",
            defaultOptions = listOf(
                TemplateOption(
                    text = "Luigi's Woodfired Pizzeria",
                    subtitle = "Neapolitan pizza, gluten-free crust options",
                    dateTimeSlot = "Today 12:30 PM",
                    venueAddress = "44 Main St",
                    duration = "1 hour",
                    priceRating = "$$"
                ),
                TemplateOption(
                    text = "Tokyo Ramen & Dumpling House",
                    subtitle = "Tonkotsu, vegetarian broth, fast service",
                    dateTimeSlot = "Today 12:30 PM",
                    venueAddress = "102 Station Plaza",
                    duration = "45 mins",
                    priceRating = "$"
                ),
                TemplateOption(
                    text = "Green Harvest Poke & Bowls",
                    subtitle = "Fresh salmon, tofu bowls, healthy & fast",
                    dateTimeSlot = "Today 12:30 PM",
                    venueAddress = "88 Green Court",
                    duration = "45 mins",
                    priceRating = "$$"
                ),
                TemplateOption(
                    text = "Tacos del Sol",
                    subtitle = "Street tacos, salsa bar, outdoor bench seating",
                    dateTimeSlot = "Today 12:30 PM",
                    venueAddress = "15 Fiesta Way",
                    duration = "1 hour",
                    priceRating = "$"
                )
            )
        ),
        PollTemplate(
            id = "template_rsvp",
            title = "🎉 Friends BBQ & Board Games Attendance",
            category = "EVENT",
            categoryIcon = "🎉",
            description = "Gathering at Maya's place this Sunday. Please RSVP and note if you are bringing +1s or snacks!",
            allowMultipleChoices = false,
            allowCustomOptions = false,
            isAnonymous = false,
            defaultDeadlineHours = 24,
            defaultTargetHeadcount = 15,
            defaultGroupId = "group_gamers",
            defaultGroupName = "Downtown Board Gamers 🎲",
            defaultOptions = listOf(
                TemplateOption(
                    text = "Sunday Afternoon Session",
                    subtitle = "Grill is on, backyard lawn games & strategy board games",
                    dateTimeSlot = "Sunday 1:00 PM - 5:00 PM",
                    venueAddress = "Maya's Backyard (Oakville)",
                    duration = "4 hours",
                    priceRating = "BYOB"
                ),
                TemplateOption(
                    text = "Sunday Evening Session",
                    subtitle = "Evening BBQ, campfire & party trivia quiz",
                    dateTimeSlot = "Sunday 5:00 PM - 9:00 PM",
                    venueAddress = "Maya's Backyard (Oakville)",
                    duration = "4 hours",
                    priceRating = "BYOB"
                )
            )
        ),
        PollTemplate(
            id = "template_feedback",
            title = "⭐ Hackathon Demo & Feedback Survey",
            category = "FEEDBACK",
            categoryIcon = "⭐",
            description = "Anonymous rating and constructive feedback on our project presentation and prototype.",
            allowMultipleChoices = false,
            allowCustomOptions = false,
            isAnonymous = true,
            defaultDeadlineHours = 12,
            defaultTargetHeadcount = null,
            defaultGroupId = "group_eng",
            defaultGroupName = "Product & Engineering Crew 💻",
            defaultOptions = listOf(
                TemplateOption(
                    text = "Overall Presentation Clarity & Pitch",
                    subtitle = "Rating 1 to 5 stars + feedback comments",
                    dateTimeSlot = "Demo Slot A (10:00 AM)",
                    venueAddress = "Auditorium Main Stage",
                    duration = "30 mins"
                ),
                TemplateOption(
                    text = "Product Concept & Technical Innovation",
                    subtitle = "Rating 1 to 5 stars + feedback comments",
                    dateTimeSlot = "Demo Slot B (11:00 AM)",
                    venueAddress = "Breakout Room 3",
                    duration = "30 mins"
                ),
                TemplateOption(
                    text = "UI/UX & Interactive Design Review",
                    subtitle = "Rating 1 to 5 stars + feedback comments",
                    dateTimeSlot = "Demo Slot C (2:00 PM)",
                    venueAddress = "Design Studio Lab",
                    duration = "45 mins"
                )
            )
        )
    )
}
