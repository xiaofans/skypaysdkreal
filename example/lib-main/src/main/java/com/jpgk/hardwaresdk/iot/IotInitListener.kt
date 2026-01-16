package com.jpgk.hardwaresdk.iot

import com.jpgk.vendingmachine.lib_base.entity.DeviceEntity
import com.jpgk.vendingmachine.lib_base.entity.MachineEntity

interface IotInitListener {
    fun onSuccess(device: DeviceEntity, machine: MachineEntity)
    fun onAuthFail(error: Throwable)
    fun onFatal(error: Throwable)
}