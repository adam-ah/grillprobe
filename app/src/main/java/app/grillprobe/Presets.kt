package app.grillprobe

enum class Animal(
    val icon: Int,
    val label: Int,
) {
    COW(icon = R.drawable.ic_cow, label = R.string.animal_beef),
    PIG(icon = R.drawable.ic_pig, label = R.string.animal_pork),
    CHICKEN(icon = R.drawable.ic_chicken, label = R.string.animal_chicken),
    LAMB(icon = R.drawable.ic_lamb, label = R.string.animal_lamb),
}

enum class Preset(
    val label: Int,
    val celsius: Int,
    val animal: Animal,
) {
    BEEF_RARE(label = R.string.preset_rare, celsius = 52, animal = Animal.COW),
    BEEF_MEDIUM_RARE(label = R.string.preset_medium_rare, celsius = 56, animal = Animal.COW),
    BEEF_MEDIUM(label = R.string.preset_medium, celsius = 60, animal = Animal.COW),
    BEEF_MEDIUM_WELL(label = R.string.preset_medium_well, celsius = 65, animal = Animal.COW),
    BEEF_WELL_DONE(label = R.string.preset_well_done, celsius = 70, animal = Animal.COW),
    BEEF_BRISKET(label = R.string.preset_brisket, celsius = 93, animal = Animal.COW),
    PORK_TENDERLOIN(label = R.string.preset_tenderloin, celsius = 63, animal = Animal.PIG),
    PORK_ROAST(label = R.string.preset_roast, celsius = 70, animal = Animal.PIG),
    PORK_RIBS(label = R.string.preset_ribs, celsius = 93, animal = Animal.PIG),
    PORK_PULLED(label = R.string.preset_pulled_pork, celsius = 95, animal = Animal.PIG),
    CHICKEN_BREAST(label = R.string.preset_breast, celsius = 74, animal = Animal.CHICKEN),
    CHICKEN_THIGH(label = R.string.preset_thigh, celsius = 82, animal = Animal.CHICKEN),
    CHICKEN_WINGS(label = R.string.preset_wings, celsius = 85, animal = Animal.CHICKEN),
    LAMB_RARE(label = R.string.preset_rare, celsius = 52, animal = Animal.LAMB),
    LAMB_MEDIUM_RARE(label = R.string.preset_medium_rare, celsius = 57, animal = Animal.LAMB),
    LAMB_MEDIUM(label = R.string.preset_medium, celsius = 62, animal = Animal.LAMB),
    LAMB_WELL_DONE(label = R.string.preset_well_done, celsius = 70, animal = Animal.LAMB),
    LAMB_PULLED(label = R.string.preset_pulled_shoulder, celsius = 93, animal = Animal.LAMB),
    ;

    companion object {
        fun named(name: String?): Preset? = entries.firstOrNull { it.name == name }

        fun of(animal: Animal): List<Preset> = entries.filter { it.animal == animal }
    }
}
