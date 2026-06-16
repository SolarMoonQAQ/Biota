package cn.solarmoon.biota.entity.peafowl

import ru.nsk.kstatemachine.state.DefaultState

sealed class PeafowlState: DefaultState() {
    sealed class Intent: PeafowlState() {
        object PrepareFly: Intent()
        object Fly: Intent()
        object Display: Intent()
    }

}