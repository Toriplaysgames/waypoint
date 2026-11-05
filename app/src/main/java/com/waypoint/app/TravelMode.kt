package com.waypoint.app

/** Add a mode here with an actual routing profile, rather than reusing car ETAs. */
enum class TravelMode(private val englishLabel:String,val costing:String,val routeDescription:String) {
    CAR("Car","auto","driving"),
    MOTORCYCLE("Motorcycle","motorcycle","motorcycle"),
    BICYCLE("Bicycle","bicycle","cycling"),
    WALK("Walk","pedestrian","walking");

    val label: String get() = tr(englishLabel, when(this) { CAR -> "רכב"; MOTORCYCLE -> "אופנוע"; BICYCLE -> "אופניים"; WALK -> "הליכה" })

    /** Hebrew noun phrase for this mode's route, as in "מחשבים מסלול נסיעה". */
    val hebrewRoute: String get() = when(this) { CAR -> "מסלול נסיעה"; MOTORCYCLE -> "מסלול לאופנוע"; BICYCLE -> "מסלול רכיבה"; WALK -> "מסלול הליכה" }
}
