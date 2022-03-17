enum class State(val key: String) {
    Started("started"),
    WaitPhone("wait_phone"),
    WaitBnb("wait_bnb"),
    WaitJoinTg("wait_tg"),
    WaitJoinTwitter("wait_twitter"),


    Finished("finished");

    companion object {
        fun fromString(stateStr: String?) = State.values().find { it.key == stateStr }
    }
}

fun String?.isState(state: State) = this != null && state.toString() == this